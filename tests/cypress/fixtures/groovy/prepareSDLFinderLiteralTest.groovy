import org.jahia.services.content.*
import org.jahia.services.usermanager.JahiaUserManagerService

import javax.jcr.RepositoryException

// Content for the SDL finder literal test. Titles carry the "sdlLit" marker so the assertions
// can single these nodes out of whatever else the instance holds.
//
// jcr:title and desc are i18n on jnt:news, so they are set from a locale-bearing session, as
// prepareValidityTest.groovy does. The date is not i18n and stays on the plain one.
JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback() {
    @Override
    Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
        JCRNodeWrapper rootTests = session.getNode("/sites/systemsite/contents").addNode("sdlFinderLiteralTest", "jnt:contentFolder")

        // plain title, inside the queried date range
        JCRNodeWrapper plain = addNews(rootTests, "plain", 2024, Calendar.JUNE, 1)
        // title containing an apostrophe
        JCRNodeWrapper quoted = addNews(rootTests, "quoted", 2024, Calendar.JUNE, 2)
        // outside the queried date range
        JCRNodeWrapper outOfRange = addNews(rootTests, "outofrange", 1999, Calendar.JANUARY, 1)

        session.save()

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(
                JahiaUserManagerService.getInstance().lookupUser("root").getJahiaUser(), "default", Locale.ENGLISH, enSession -> {
            setTitle(enSession, plain.getPath(), "sdlLit plain marker")
            setTitle(enSession, quoted.getPath(), "sdlLit O'Brien marker")
            setTitle(enSession, outOfRange.getPath(), "sdlLit out of range marker")
            enSession.save()
            return null
        })

        JCRPublicationService.getInstance().publishByMainId(rootTests.getIdentifier(), "default", "live", ["en"].toSet(), true, null)
    }

    private JCRNodeWrapper addNews(JCRNodeWrapper parent, String name, int year, int month, int day) {
        JCRNodeWrapper news = parent.addNode(JCRContentUtils.findAvailableNodeName(parent, "sdlLitNews-" + name), "jnt:news")
        Calendar date = Calendar.getInstance()
        date.clear()
        date.set(year, month, day)
        news.setProperty("date", date)
        return news
    }

    private void setTitle(JCRSessionWrapper enSession, String path, String title) {
        JCRNodeWrapper news = enSession.getNode(path)
        news.setProperty("jcr:title", title)
        news.setProperty("desc", title)
    }
})
