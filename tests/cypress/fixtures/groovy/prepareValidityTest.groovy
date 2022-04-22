import org.jahia.services.content.*
import org.jahia.services.usermanager.JahiaUserManagerService

import javax.jcr.RepositoryException

JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback() {
    @Override
    Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
        JCRNodeWrapper pageNode = session.getNode("/sites/systemsite/").addNode("testValidity", "jnt:page")
        pageNode.setProperty("j:templateName", "home")
        pageNode.addNode( "controlNode", "jnt:text")
        JCRNodeWrapper unpublish = pageNode.addNode( "unpublished", "jnt:text")
        JCRNodeWrapper visibility = pageNode.addNode( "visibility", "jnt:text")
        JCRNodeWrapper condition = visibility.addNode("j:conditionalVisibility", "jnt:conditionalVisibility").addNode("jnt:startEndDateCondition1650027997279", "jnt:startEndDateCondition")
        condition.setProperty("start", "1980-03-29T15:06:00.000Z")
        condition.setProperty("end", "2020-03-31T15:06:00.000Z" )

        JCRNodeWrapper inactive = pageNode.addNode( "with-inactive-language", "jnt:text")
        inactive.setProperty("j:invalidLanguages",new String[] {"en"})
        session.save()
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(JahiaUserManagerService.getInstance().lookupUser("root").getJahiaUser(), "default", Locale.ENGLISH, enSession -> {
            enSession.getNode(pageNode.getPath()).setProperty("jcr:title", "page title")
            enSession.getNode(inactive.getPath()).setProperty("text", "text in english")
            enSession.save()
            return null
        })
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(JahiaUserManagerService.getInstance().lookupUser("root").getJahiaUser(), "default", Locale.FRENCH, frSession -> {
            frSession.getNode(pageNode.getPath()).setProperty("jcr:title", "page title fr")
            frSession.getNode(inactive.getPath()).setProperty("text", "text in french")
            frSession.save()
            return null
        })
        // Publish
        JCRPublicationService.getInstance().publishByMainId(pageNode.getIdentifier(), "default", "live", ["en", "fr"].toSet(), true, null)

        JCRPublicationService.getInstance().unpublish([unpublish.getIdentifier()].toList())
    }
})