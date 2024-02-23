
import org.jahia.services.content.JCRCallback
import org.jahia.services.content.JCRNodeWrapper
import org.jahia.services.content.JCRSessionWrapper
import org.jahia.services.content.JCRTemplate


import javax.jcr.RepositoryException
import java.text.MessageFormat

JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback() {
    @Override
    Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
        def nodeCreated = 0;
        JCRNodeWrapper rootTests = session.getNode("/sites/systemsite/contents").addNode("paginationNodeTest", "jnt:contentFolder")
        createFolders(session, rootTests, ["en"].toList(), 1, 3, 3, nodeCreated);
        session.save();
        return null;
    }

    private void createFolders(JCRSessionWrapper session, JCRNodeWrapper parent, List<Locale> locales, int level, int iterations, int maxLevel, int nodeCreated) throws RepositoryException {

        //loop around your structure, I hardcoded two levels
        while (level <= maxLevel) {
            for (int i = 0; i < iterations; i++) {
                createFolder("Folder Level " + level + " - " + i, "folder-" + level + "-" + i, parent, session, locales, level, iterations, maxLevel, nodeCreated);
            }
            return;
        }
        session.save();

    }

    private void createFolder(String title, String systemName, JCRNodeWrapper parent,
                                        JCRSessionWrapper session, List<Locale> locales, int level, int iterations, int maxLevel, int nodeCreated) throws RepositoryException {

        JCRNodeWrapper subPage = parent.addNode(systemName, "jnt:contentFolder");
        nodeCreated++;
        for (int i = 0; i < iterations; i++) {
            JCRNodeWrapper news = subPage.addNode("news-" + level + "-" + i, "jnt:news");
            news.setProperty("jcr:title", MessageFormat.format("News {0} {1}", level, i));
            nodeCreated++;
        }

        if(nodeCreated % 500 == 0) {
            session.save()
        }
        createFolders(session, subPage, locales, level + 1, iterations, maxLevel, nodeCreated);
    }
})
