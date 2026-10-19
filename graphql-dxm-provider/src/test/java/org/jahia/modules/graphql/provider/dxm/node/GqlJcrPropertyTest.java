package org.jahia.modules.graphql.provider.dxm.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;

import org.jahia.modules.graphql.provider.dxm.security.PermissionHelper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.utils.EncryptionUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * Covers the two fields that read a decrypted value: which session receives one, and how many times the
 * permission behind it is evaluated.
 */
public class GqlJcrPropertyTest {

    private static final String PLAINTEXT = "the-value-of-a-password-property";

    private JCRNodeWrapper jcrNode;
    private GqlJcrNode node;

    @Before
    public void createNode() throws RepositoryException {
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
     * A query that asks for the field on every property of one node evaluates the permission once. Asking per
     * property would run one evaluation of the access control per property of every editor form.
     */
    @Test
    public void thePermissionIsEvaluatedOncePerNode() throws RepositoryException {
        allow(true);

        for (int i = 0; i < 3; i++) {
            new GqlJcrProperty(singleValued(), node).getDecryptedValue();
        }

        verify(jcrNode, times(1)).hasPermission(PermissionHelper.VIEW_ENCRYPTED_VALUE);
    }

    private void allow(boolean allowed) {
        when(jcrNode.hasPermission(PermissionHelper.VIEW_ENCRYPTED_VALUE)).thenReturn(allowed);
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
