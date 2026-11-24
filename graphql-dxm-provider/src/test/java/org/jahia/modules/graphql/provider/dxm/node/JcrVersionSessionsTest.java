package org.jahia.modules.graphql.provider.dxm.node;

import graphql.GraphQLContext;
import graphql.execution.AbortExecutionException;
import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.FragmentDefinition;
import graphql.language.OperationDefinition;
import graphql.parser.Parser;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRWorkspaceWrapper;
import org.jahia.services.usermanager.JahiaUser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the decision that {@link JcrVersionSessions} takes when an operation starts, with a fake of the version
 * sessions of core.
 */
public class JcrVersionSessionsTest {

    private static final String OPERATION_ID = "0b1f7d6c-2f7e-4b8e-9a3c-5d2e1f0a9b8c";

    private FakeVersionApi api;
    private JcrVersionSessions versionSessions;
    private HttpServletRequest request;
    private GraphQLContext context;

    @Before
    public void setUp() {
        api = new FakeVersionApi();
        api.user = user("editor");
        versionSessions = new JcrVersionSessions(api);
        request = request();
        context = GraphQLContext.newContext().build();
    }

    @After
    public void tearDown() {
        versionSessions.resumeVersion(null);
    }

    @Test
    public void plainQueryOpensNoVersion() {
        begin("{ jcr { nodeByPath(path: \"/sites\") { name } } }", Collections.emptyMap());

        assertFalse(versionSessions.isOpened(context));
        assertNull(api.selector);
        assertNull(versionSessions.currentVersion());
        versionSessions.checkOperation(context);
    }

    @Test
    public void versionAtOpensTheVersionOfTheOperation() {
        begin("{ jcr(versionAt: 1700000000000) { nodeByPath(path: \"/sites\") { name } } }", Collections.emptyMap());

        assertTrue(versionSessions.isOpened(context));
        assertEquals("at:" + Instant.ofEpochMilli(1700000000000L), api.selector);
        assertEquals(api.selector, versionSessions.currentVersion());
        versionSessions.checkOperation(context);
    }

    @Test
    public void versionAtReadsAVariable() {
        begin("query Q($at: Long) { jcr(versionAt: $at) { workspace } }", Collections.singletonMap("at", 1000L));

        assertTrue(versionSessions.isOpened(context));
        assertEquals("at:" + Instant.ofEpochMilli(1000L), api.selector);
    }

    @Test
    public void nullVersionAtOpensNoVersion() {
        begin("query Q($at: Long) { jcr(versionAt: $at) { workspace } other }", Collections.singletonMap("at", null));

        assertFalse(versionSessions.isOpened(context));
        assertNull(api.selector);
        versionSessions.checkOperation(context);
    }

    @Test
    public void versionOperationOpensAnOperationThatTheUserCanSee() {
        begin("{ jcr(versionOperation: \"" + OPERATION_ID + "\") { workspace } }", Collections.emptyMap());

        assertTrue(versionSessions.isOpened(context));
        assertEquals("op:" + OPERATION_ID, api.selector);
        assertEquals(1, api.checks);
    }

    @Test
    public void unknownOperationFailsAndClearsTheVersion() {
        api.canShow = false;

        begin("{ jcr(versionOperation: \"" + OPERATION_ID + "\") { workspace } }", Collections.emptyMap());

        assertRefused("No version that you can read carries the operation " + OPERATION_ID);
        assertNull(api.selector);
        assertNull(versionSessions.currentVersion());
    }

    @Test
    public void operationThatIsNotAUuidFails() {
        begin("{ jcr(versionOperation: \"not-a-uuid\") { workspace } }", Collections.emptyMap());

        assertRefused("versionOperation is not a UUID: not-a-uuid");
    }

    @Test
    public void bothArgumentsFail() {
        begin("{ jcr(versionAt: 1, versionOperation: \"" + OPERATION_ID + "\") { workspace } }", Collections.emptyMap());

        assertRefused("A query reads a version with versionAt or with versionOperation, not with both");
    }

    @Test
    public void liveWorkspaceFails() {
        begin("{ jcr(workspace: LIVE, versionAt: 1) { workspace } }", Collections.emptyMap());

        assertRefused("A query reads a version in the EDIT workspace only");
    }

    @Test
    public void liveWorkspaceInAVariableFails() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("ws", NodeQueryExtensions.Workspace.LIVE);
        begin("query Q($ws: Workspace) { jcr(workspace: $ws, versionAt: 1) { workspace } }", variables);

        assertRefused("A query reads a version in the EDIT workspace only");
    }

    @Test
    public void anotherRootFieldFails() {
        begin("{ jcr(versionAt: 1) { workspace } currentUser { name } }", Collections.emptyMap());

        assertRefused("A query that reads a version selects the jcr field alone at its root");
    }

    @Test
    public void rootFieldsOfAFragmentCount() {
        begin("query { ...Root } fragment Root on Query { jcr(versionAt: 1) { workspace } currentUser { name } }",
                Collections.emptyMap());

        assertRefused("A query that reads a version selects the jcr field alone at its root");
    }

    @Test
    public void versionInAFragmentOpensTheVersion() {
        begin("query { ... on Query { ...Root } } fragment Root on Query { jcr(versionAt: 1) { workspace } }",
                Collections.emptyMap());

        assertTrue(versionSessions.isOpened(context));
    }

    @Test
    public void jahiaWithoutVersionSessionsFails() {
        versionSessions = new JcrVersionSessions(null);

        begin("{ jcr(versionAt: 1) { workspace } }", Collections.emptyMap());

        assertRefused("Reading a version needs Jahia 8.2.4 or later");
    }

    @Test
    public void operationWithoutARequestFails() {
        request = null;

        begin("{ jcr(versionAt: 1) { workspace } }", Collections.emptyMap());

        assertRefused("Reading a version needs an HTTP request");
    }

    @Test
    public void guestFails() {
        api.user = user("guest");

        begin("{ jcr(versionAt: 1) { workspace } }", Collections.emptyMap());

        assertRefused("A guest cannot read a version");
        assertNull(api.selector);
    }

    @Test
    public void versionOfAnotherCallerFails() {
        api.requested = true;

        begin("{ jcr(versionAt: 1) { workspace } }", Collections.emptyMap());

        assertRefused("This request already shows a version");
        assertNull(api.selector);
    }

    @Test
    public void openEditSessionOfAFilterLetsTheVersionOpen() throws RepositoryException {
        // The security filter opens this session before GraphQL runs, and core closes it when the version is set.
        api.sessions.add(session("default", "editor", true, false));

        begin("{ jcr(versionAt: 1) { workspace } }", Collections.emptyMap());

        assertTrue(versionSessions.isOpened(context));
    }

    @Test
    public void editSessionWithUnsavedChangesFails() throws RepositoryException {
        api.sessions.add(session("default", "editor", true, true));

        begin("{ jcr(versionAt: 1) { workspace } }", Collections.emptyMap());

        assertRefused("This request has unsaved changes in the EDIT workspace, so it cannot read a version");
        assertNull(api.selector);
    }

    @Test
    public void unsavedChangesOutsideTheEditSessionsOfTheUserLetTheVersionOpen() throws RepositoryException {
        api.sessions.add(session("live", "editor", true, true));
        api.sessions.add(session("default", "editor", false, true));
        api.sessions.add(session("default", "someone-else", true, true));

        begin("{ jcr(versionAt: 1) { workspace } }", Collections.emptyMap());

        assertTrue(versionSessions.isOpened(context));
    }

    @Test
    public void queryInsideARenderFails() {
        api.rendering = true;

        begin("{ jcr(versionAt: 1) { workspace } }", Collections.emptyMap());

        assertRefused("A query that runs inside a render cannot read a version");
        assertNull(api.selector);
    }

    @Test
    public void operationAfterAVersionInTheSameRequestFails() {
        begin("{ jcr(versionAt: 1) { workspace } }", Collections.emptyMap());
        assertTrue(versionSessions.isOpened(context));

        context = GraphQLContext.newContext().build();
        begin("mutation { jcr { mutateNode(pathOrId: \"/sites\") { delete } } }", Collections.emptyMap());

        assertRefused("A request that read a version runs no other operation after it");
        assertNull(versionSessions.currentVersion());
    }

    @Test
    public void operationOfAnotherRequestCarriesNoVersion() {
        begin("{ jcr(versionAt: 1) { workspace } }", Collections.emptyMap());
        assertEquals(api.selector, versionSessions.currentVersion());

        request = request();
        context = GraphQLContext.newContext().build();
        begin("{ jcr { workspace } }", Collections.emptyMap());

        assertNull(versionSessions.currentVersion());
        versionSessions.checkOperation(context);
    }

    @Test
    public void mutationOpensNoVersion() {
        begin("mutation { jcr(versionAt: 1) { workspace } }", Collections.emptyMap());

        assertFalse(versionSessions.isOpened(context));
        assertNull(api.selector);
    }

    @Test
    public void resumeVersionOpensAndClearsTheVersionOfAThread() {
        versionSessions.resumeVersion("at:carried");

        assertEquals("at:carried", api.selector);
        assertEquals("at:carried", versionSessions.currentVersion());

        versionSessions.resumeVersion(null);

        assertNull(api.selector);
        assertNull(versionSessions.currentVersion());
    }

    private void begin(String query, Map<String, Object> variables) {
        Document document = new Parser().parseDocument(query);
        OperationDefinition operation = null;
        Map<String, FragmentDefinition> fragments = new HashMap<>();
        for (Definition<?> definition : document.getDefinitions()) {
            if (definition instanceof OperationDefinition) {
                operation = (OperationDefinition) definition;
            } else if (definition instanceof FragmentDefinition) {
                fragments.put(((FragmentDefinition) definition).getName(), (FragmentDefinition) definition);
            }
        }
        versionSessions.beginOperation(operation, fragments, variables, request, context);
    }

    private void assertRefused(String message) {
        assertFalse(versionSessions.isOpened(context));
        try {
            versionSessions.checkOperation(context);
            fail("The operation should be refused");
        } catch (AbortExecutionException e) {
            assertEquals(message, e.getMessage());
        }
    }

    private static HttpServletRequest request() {
        Map<String, Object> attributes = new HashMap<>();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        return request;
    }

    private static JahiaUser user(String name) {
        JahiaUser user = mock(JahiaUser.class);
        when(user.getName()).thenReturn(name);
        when(user.getUsername()).thenReturn(name);
        return user;
    }

    private static JCRSessionWrapper session(String workspace, String username, boolean live, boolean unsaved)
            throws RepositoryException {
        JCRWorkspaceWrapper workspaceWrapper = mock(JCRWorkspaceWrapper.class);
        when(workspaceWrapper.getName()).thenReturn(workspace);
        JCRSessionWrapper session = mock(JCRSessionWrapper.class);
        when(session.getWorkspace()).thenReturn(workspaceWrapper);
        when(session.isLive()).thenReturn(live);
        when(session.hasPendingChanges()).thenReturn(unsaved);
        JahiaUser user = user(username);
        when(session.getUser()).thenReturn(user);
        return session;
    }

    /** A fake of the version sessions of core, which records the version of the thread. */
    private static final class FakeVersionApi implements JcrVersionSessions.VersionApi {

        private final List<JCRSessionWrapper> sessions = new ArrayList<>();
        private Object selector;
        private boolean canShow = true;
        private boolean requested;
        private boolean rendering;
        private int checks;
        private JahiaUser user;

        @Override
        public Object atInstant(Instant instant) {
            return "at:" + instant;
        }

        @Override
        public Object atOperation(UUID operationId) {
            return "op:" + operationId;
        }

        @Override
        public void setCurrentVersionSelector(Object selector) {
            this.selector = selector;
        }

        @Override
        public boolean canShowVersion(Object selector) {
            checks++;
            return canShow;
        }

        @Override
        public boolean isVersionSessionRequested() {
            return requested;
        }

        @Override
        public boolean isVersionSession(JCRSessionWrapper session) {
            return false;
        }

        @Override
        public boolean isRendering() {
            return rendering;
        }

        @Override
        public JahiaUser getCurrentUser() {
            return user;
        }

        @Override
        public Collection<JCRSessionWrapper> getOpenUserSessions() {
            return sessions;
        }
    }
}
