package org.jahia.modules.graphql.provider.dxm.user;

import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.usermanager.JahiaGroup;
import org.jahia.services.usermanager.JahiaUser;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The contract of the principal types: which names their generic {@code property(name:)} accessor
 * answers for, and which permission the fields that reach them from the admin query declare.
 *
 * <p>Each accessor is asserted in both directions, so an accessor wired to answer {@code null} for every
 * name would satisfy no more of this class than one that answers for every name.
 */
public class PrincipalQueryTest {

    /** Shaped like a stored digest so that a value passing through is unmistakable. */
    private static final String DIGEST = "p:tXC2ffF57fWkO7NB+nMtQM8p==$CGZVzSWjOWNGBy8WM1FW06GwGDPlwxrdHVJj60DxvME=";

    private static final String ORGANIZATION = "j:organization";
    private static final String FIRST_NAME = "j:firstName";

    private static JahiaUser user() {
        JahiaUser user = mock(JahiaUser.class);
        when(user.getName()).thenReturn("jay");
        when(user.getProperty(JCRUserNode.J_PASSWORD)).thenReturn(DIGEST);
        when(user.getProperty(FIRST_NAME)).thenReturn("Jay");
        when(user.getProperty(ORGANIZATION)).thenReturn("Jahia");
        return user;
    }

    private static JahiaGroup group() {
        JahiaGroup group = mock(JahiaGroup.class);
        when(group.getProperty(JCRUserNode.J_PASSWORD)).thenReturn(DIGEST);
        when(group.getProperty(ORGANIZATION)).thenReturn("Jahia");
        return group;
    }

    @Test
    public void userPropertyAnswersForPublishedNames() {
        GqlUser gqlUser = new GqlUser(user());

        assertEquals("Jahia", gqlUser.getProperty(ORGANIZATION));
        assertEquals("Jay", gqlUser.getProperty(FIRST_NAME));
        assertNull(gqlUser.getProperty(JCRUserNode.J_PASSWORD));
    }

    @Test
    public void currentUserPropertyAnswersForPublishedNames() {
        GqlCurrentUser gqlCurrentUser = new GqlCurrentUser(user());

        assertEquals("Jahia", gqlCurrentUser.getProperty(ORGANIZATION));
        assertEquals("Jay", gqlCurrentUser.getProperty(FIRST_NAME));
        assertNull(gqlCurrentUser.getProperty(JCRUserNode.J_PASSWORD));
    }

    @Test
    public void groupPropertyAnswersForPublishedNames() {
        GqlGroup gqlGroup = new GqlGroup(group());

        assertEquals("Jahia", gqlGroup.getProperty(ORGANIZATION));
        assertNull(gqlGroup.getProperty(JCRUserNode.J_PASSWORD));
    }

    /** The typed profile fields name their own property, so they read the same either way. */
    @Test
    public void typedProfileFieldsAnswerFromTheSameAccount() {
        JahiaUser account = user();

        assertEquals("Jay", new GqlUser(account).getFirstname());
        assertEquals("Jahia", new GqlUser(account).getOrganization());
        assertEquals("jay", new GqlUser(account).getUsername());
    }

    @Test
    public void adminPrincipalEntryFieldsRequireTheAdminQueryPermission() throws NoSuchMethodException {
        assertRequiresAdminQuery(UserAdminExtension.class.getDeclaredMethod("getUserAdmin"));
        assertRequiresAdminQuery(UserGroupExtension.class.getDeclaredMethod("getUserGroup"));
    }

    private static void assertRequiresAdminQuery(Method entryField) {
        GraphQLRequiresPermission permission = entryField.getAnnotation(GraphQLRequiresPermission.class);

        assertNotNull(entryField.getName() + " declares a required permission", permission);
        assertEquals("graphqlAdminQuery", permission.value());
    }
}
