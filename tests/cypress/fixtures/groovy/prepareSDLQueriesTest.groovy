import org.jahia.services.content.*

import javax.jcr.RepositoryException

JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback() {
    @Override
    Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
        JCRNodeWrapper rootTests = session.getNode("/sites/systemsite/contents").addNode("sdlQueriesTest", "jnt:contentFolder")
        (1..10).each {addOffer(rootTests) }

        session.save()

        // Publish
        JCRPublicationService.getInstance().publishByMainId(rootTests.getIdentifier(), "default", "live", ["en"].toSet(), true, null)
    }

    private void addOffer(JCRNodeWrapper rootTests) {
        JCRNodeWrapper offer = rootTests.addNode(JCRContentUtils.findAvailableNodeName(rootTests, "offer"), "jnt:offer")
        offer.setProperty("jcr:title", "Offer Title");
        offer.setProperty("subTitle", "Offer sub title");
        (1..3).each { addCoverage(offer) }
    }

    private void addCoverage(JCRNodeWrapper offer) {
        JCRNodeWrapper coverage = offer.addNode(JCRContentUtils.findAvailableNodeName(offer, "coverageOffer"), "jnt:coverageOffer")
        coverage.setProperty("jcr:title", "Coverage Title");
    }
})
