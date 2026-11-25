package org.jahia.modules.graphql.provider.dxm.node;

import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.versioning.JcrVersionSessions;
import org.jahia.services.content.JCRSessionFactory;
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
@PrepareForTest({JcrVersionSessions.class, JCRSessionFactory.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*", "com.sun.org.apache.xalan.*"})
public class GqlJcrQueryTest {

    @Test
    public void aQueryOfAVersionRefusesAPlainSession() throws RepositoryException {
        JCRSessionWrapper plain = mock(JCRSessionWrapper.class);
        JCRSessionFactory factory = mock(JCRSessionFactory.class);
        when(factory.getCurrentUserSession("default")).thenReturn(plain);
        mockStatic(JCRSessionFactory.class);
        when(JCRSessionFactory.getInstance()).thenReturn(factory);
        JcrVersionSessions versionSessions = mock(JcrVersionSessions.class);
        when(versionSessions.isVersionSession(plain)).thenReturn(false);
        mockStatic(JcrVersionSessions.class);
        when(JcrVersionSessions.getInstance()).thenReturn(versionSessions);
        GqlJcrQuery query = new GqlJcrQuery(NodeQueryExtensions.Workspace.EDIT, true);

        DataFetchingException e = assertThrows(DataFetchingException.class, () -> query.getNodeByPath("/sites", null));

        assertEquals("The session of this query does not show the version that the query asks for", e.getMessage());
    }
}
