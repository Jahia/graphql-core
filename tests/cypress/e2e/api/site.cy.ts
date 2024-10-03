import {addNode, createSite, createUser, deleteSite, deleteUser} from '@jahia/cypress';
import {grantUserRole} from '../../fixtures/acl';

describe('Test graphql site queries', () => {
    const siteName = 'graphqlSite';
    const username = 'testUser';
    const password = 'password';

    beforeEach('create test site and test user and grant editor role to the user on the site', () => {
        createSite(siteName);
        createUser(username, password);
        grantUserRole('/sites/' + siteName, 'editor', username);
    });

    afterEach('Remove test data', () => {
        deleteSite(siteName);
        deleteUser(username);
    });

    after('Logout', () => {
        cy.logout();
    });

    it('Should be able to read the homePage property when requested as root', () => {
        cy.apollo({
            queryFile: 'site/getAllSitesWithHomePage.graphql'
        }).should(result => {
            const site = getSite(result, siteName);
            expect(site).to.have.property('name', siteName);
            expect(site).to.have.property('site');
            expect(site.site).to.have.property('homePage');
            expect(site.site.homePage).to.have.property('name', 'home');
            expect(site.site.homePage).to.have.property('path', `/sites/${siteName}/home`);
        });
    });

    it('Should be able to read the homePage property when requested as authorized user (editor)', () => {
        cy
            .apolloClient({username: username, password: password})
            .apollo({
                queryFile: 'site/getAllSitesWithHomePage.graphql'
            }).should(result => {
                const site = getSite(result, siteName);
                expect(site).to.have.property('name', siteName);
                expect(site).to.have.property('site');
                expect(site.site).to.have.property('homePage');
                expect(site.site.homePage).to.have.property('name', 'home');
                expect(site.site.homePage).to.have.property('path', `/sites/${siteName}/home`);
            });
    });

    it('Should not be able to see the homePage field when requested by an unauthorized user', () => {
        // Disable the ACL inheritance on the home page
        addNode({
            parentPathOrId: '/sites/' + siteName + '/home',
            name: 'j:acl',
            primaryNodeType: 'jnt:acl',
            properties: [
                {name: 'j:inherit', value: 'false'}
            ]
        });

        cy
            .apolloClient({username: username, password: password})
            .apollo({
                queryFile: 'site/getAllSitesWithHomePage.graphql'
            }).should(result => {
                const site = getSite(result, siteName);
                expect(site).to.have.property('name', siteName);
                expect(site).to.have.property('site');
                expect(site.site).to.have.property('homePage', null, 'Unauthorized user should not see the homePage field');
            });
    });
});

function getSite(result, siteName) {
    const sites = result.data.jcr.nodesByQuery.nodes.filter(s => s.name === siteName);
    expect(sites).to.have.lengthOf(1);
    return sites[0];
}
