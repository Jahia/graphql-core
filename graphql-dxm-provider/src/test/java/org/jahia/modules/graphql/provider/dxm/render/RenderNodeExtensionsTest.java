package org.jahia.modules.graphql.provider.dxm.render;

import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.modules.graphql.provider.dxm.versioning.JcrVersionSessions;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.jcr.RepositoryException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(JcrVersionSessions.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*", "com.sun.org.apache.xalan.*"})
public class RenderNodeExtensionsTest {

    @Test
    public void aNodeOfAVersionRendersWithoutEditMode() throws RepositoryException {
        JCRSessionWrapper session = mock(JCRSessionWrapper.class);
        JCRNodeWrapper jcrNode = mock(JCRNodeWrapper.class);
        when(jcrNode.getSession()).thenReturn(session);
        GqlJcrNode node = mock(GqlJcrNode.class);
        when(node.getNode()).thenReturn(jcrNode);
        JcrVersionSessions versionSessions = mock(JcrVersionSessions.class);
        when(versionSessions.isVersionSession(session)).thenReturn(true);
        mockStatic(JcrVersionSessions.class);
        when(JcrVersionSessions.getInstance()).thenReturn(versionSessions);
        RenderNodeExtensions extensions = new RenderNodeExtensions(node);

        GqlJcrWrongInputException e = assertThrows(GqlJcrWrongInputException.class,
                () -> extensions.getRenderedContent(null, null, null, null, null, true, null, null));

        assertEquals("A version, or a node with unsaved values, renders without edit mode", e.getMessage());
    }
}
