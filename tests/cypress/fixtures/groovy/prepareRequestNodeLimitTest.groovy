import org.jahia.services.content.JCRCallback
import org.jahia.services.content.JCRNodeWrapper
import org.jahia.services.content.JCRSessionWrapper
import org.jahia.services.content.JCRTemplate

import javax.jcr.RepositoryException

// Builds a small, deterministic subtree for the per-request node allowance test: three levels of five, so the root has
// 5 + 25 + 125 = 155 descendants.
//
// The exact shape matters. Walking the subtree once reads 155 nodes; walking it with `descendants` nested one level
// deeper reads 430 (155, plus 30 for each of the 5 top folders and 5 for each of the 25 second-level folders). The
// spec puts the allowance between those two figures, which is what tells a per-request bound apart from the
// per-connection one - both queries stay far below graphql.fields.node.limit either way.
//
// Its own root, rather than the tree preparePaginationNodeLimitTest.groovy builds, so the two specs cannot collide
// over the same path whatever order they run in.
JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback() {
    @Override
    Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
        JCRNodeWrapper contents = session.getNode("/sites/systemsite/contents")
        if (contents.hasNode("requestNodeLimitTest")) {
            contents.getNode("requestNodeLimitTest").remove()
            session.save()
        }
        JCRNodeWrapper root = contents.addNode("requestNodeLimitTest", "jnt:contentFolder")
        for (int i = 0; i < 5; i++) {
            JCRNodeWrapper folder = root.addNode("folder-" + i, "jnt:contentFolder")
            for (int j = 0; j < 5; j++) {
                JCRNodeWrapper sub = folder.addNode("sub-" + i + "-" + j, "jnt:contentFolder")
                for (int k = 0; k < 5; k++) {
                    sub.addNode("leaf-" + i + "-" + j + "-" + k, "jnt:contentFolder")
                }
            }
        }
        session.save()
        return null
    }
})
