package org.jahia.modules.graphql.provider.dxm.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;

import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.security.PermissionHelper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.utils.EncryptionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Covers the two fields that read a decrypted value: which session receives one, and on which node the
 * permission behind it is evaluated.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(JCRSessionFactory.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*", "com.sun.org.apache.xalan.*", "javax.crypto.*"})
public class GqlJcrPropertyTest {

    private static final String PLAINTEXT = "the-value-of-a-password-property";

    private JCRNodeWrapper jcrNode;
    private JCRNodeWrapper rootNode;
    private GqlJcrNode node;

    @Before
    public void createNode() throws RepositoryException {
        rootNode = mock(JCRNodeWrapper.class);
        JCRSessionWrapper session = mock(JCRSessionWrapper.class);
        when(session.getRootNode()).thenReturn(rootNode);
        JCRSessionFactory factory = mock(JCRSessionFactory.class);
        when(factory.getCurrentUserSession(Constants.EDIT_WORKSPACE)).thenReturn(session);
        mockStatic(JCRSessionFactory.class);
        when(JCRSessionFactory.getInstance()).thenReturn(factory);
        jcrNode = mock(JCRNodeWrapper.class);
        when(jcrNode.getPrimaryNodeTypeName()).thenReturn("jnt:content");
        node = new GqlJcrNodeImpl(jcrNode);
    }

    @Test
    public void aSessionHoldingThePermissionReadsTheDecryptedValue() throws RepositoryException {
        allow(true);

        assertEquals(PLAINTEXT, new GqlJcrProperty(singleValued(), node).getDecryptedValue());
    }

    @Test
    public void aSessionWithoutThePermissionReadsNoDecryptedValue() throws RepositoryException {
        allow(false);
        JCRPropertyWrapper property = singleValued();

        assertNull(new GqlJcrProperty(property, node).getDecryptedValue());
        verify(property, never()).getValue();
    }

    @Test
    public void aSessionHoldingThePermissionReadsTheDecryptedValues() throws RepositoryException {
        allow(true);

        List<String> values = new GqlJcrProperty(multiValued(), node).getDecryptedValues();

        assertEquals(Arrays.asList(PLAINTEXT, PLAINTEXT), values);
    }

    @Test
    public void aSessionWithoutThePermissionReadsNoDecryptedValues() throws RepositoryException {
        allow(false);
        JCRPropertyWrapper property = multiValued();

        assertTrue(new GqlJcrProperty(property, node).getDecryptedValues().isEmpty());
        verify(property, never()).getValues();
    }

    /**
     * A grant on the node of the property does not count: only a grant that reaches the repository root does.
     */
    @Test
    public void aGrantOnTheNodeOfThePropertyAloneReadsNoDecryptedValue() throws RepositoryException {
        when(jcrNode.hasPermission(PermissionHelper.VIEW_ENCRYPTED_VALUE)).thenReturn(true);
        allow(false);

        assertNull(new GqlJcrProperty(singleValued(), node).getDecryptedValue());
        verify(jcrNode, never()).hasPermission(PermissionHelper.VIEW_ENCRYPTED_VALUE);
    }

    /**
     * A module may return a node that it read through a system session: the permission of that session does not
     * count, only the permission of the current user does.
     */
    @Test
    public void theSessionThatLoadedTheNodeDoesNotGrantTheDecryptedValue() throws RepositoryException {
        JCRNodeWrapper systemRoot = mock(JCRNodeWrapper.class);
        when(systemRoot.hasPermission(PermissionHelper.VIEW_ENCRYPTED_VALUE)).thenReturn(true);
        JCRSessionWrapper systemSession = mock(JCRSessionWrapper.class);
        when(systemSession.getRootNode()).thenReturn(systemRoot);
        when(jcrNode.getSession()).thenReturn(systemSession);
        allow(false);

        assertNull(new GqlJcrProperty(singleValued(), node).getDecryptedValue());
    }

    @Test
    public void aValueThatDoesNotDecryptReadsAsNull() throws RepositoryException {
        allow(true);
        JCRValueWrapper value = mock(JCRValueWrapper.class);
        when(value.getString()).thenReturn("not an encrypted value");
        JCRPropertyWrapper property = mock(JCRPropertyWrapper.class);
        when(property.isMultiple()).thenReturn(false);
        when(property.getValue()).thenReturn(value);

        assertNull(new GqlJcrProperty(property, node).getDecryptedValue());
    }

    @Test
    public void aValueThatDoesNotDecryptEmptiesTheValues() throws RepositoryException {
        allow(true);
        JCRValueWrapper decrypts = encrypted();
        JCRValueWrapper value = mock(JCRValueWrapper.class);
        when(value.getString()).thenReturn("not an encrypted value");
        JCRValueWrapper[] values = new JCRValueWrapper[] { decrypts, value };
        JCRPropertyWrapper property = mock(JCRPropertyWrapper.class);
        when(property.isMultiple()).thenReturn(true);
        when(property.getValues()).thenReturn(values);

        assertTrue(new GqlJcrProperty(property, node).getDecryptedValues().isEmpty());
    }

    private void allow(boolean allowed) {
        when(rootNode.hasPermission(PermissionHelper.VIEW_ENCRYPTED_VALUE)).thenReturn(allowed);
    }

    private static JCRPropertyWrapper singleValued() throws RepositoryException {
        // The value is built before the stubbing below starts: a mock created inside a when() argument
        // leaves that stubbing unfinished.
        JCRValueWrapper value = encrypted();
        JCRPropertyWrapper property = mock(JCRPropertyWrapper.class);
        when(property.isMultiple()).thenReturn(false);
        when(property.getValue()).thenReturn(value);
        return property;
    }

    private static JCRPropertyWrapper multiValued() throws RepositoryException {
        JCRValueWrapper[] values = new JCRValueWrapper[] { encrypted(), encrypted() };
        JCRPropertyWrapper property = mock(JCRPropertyWrapper.class);
        when(property.isMultiple()).thenReturn(true);
        when(property.getValues()).thenReturn(values);
        return property;
    }

    private static JCRValueWrapper encrypted() throws RepositoryException {
        String ciphertext = EncryptionUtils.passwordBaseEncrypt(PLAINTEXT);
        JCRValueWrapper value = mock(JCRValueWrapper.class);
        when(value.getString()).thenReturn(ciphertext);
        return value;
    }
}
