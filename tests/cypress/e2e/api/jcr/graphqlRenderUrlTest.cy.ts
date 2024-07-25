import {addNode, addVanityUrl, createSite, deleteSite, publishAndWaitJobEnding} from '@jahia/cypress';

const sitename = 'graphql_test_renderurl';
describe('Test graphql render url generation', () => {
    before('Create a site', () => {
        createSite(sitename);
        addNode({
            parentPathOrId: '/sites/' + sitename + '/home',
            name: 'page1',
            primaryNodeType: 'jnt:page',
            properties: [
                {name: 'jcr:title', value: 'page1'},
                {name: 'j:templateName', value: 'simple'}
            ]
        });
        addNode({
            parentPathOrId: '/sites/' + sitename + '/home/page1',
            name: 'page2',
            primaryNodeType: 'jnt:page',
            properties: [
                {name: 'jcr:title', value: 'page2'},
                {name: 'jcr:title', value: 'page2'},
                {name: 'j:templateName', value: 'simple'}
            ]
        });
        publishAndWaitJobEnding('/sites/' + sitename);
    });

    after('Delete the site', () => {
        deleteSite(sitename);
    });

    it('Returns correct values for live workspaces', () => {
        cy.apollo({
            queryFile: 'jcr/renderUrl.graphql',
            variables: {
                path: `/sites/${sitename}/home`,
                workspace: 'LIVE',
                lang: 'en'
            }
        }).should(resp => {
            expect(resp.data.jcr.nodeByPath.renderUrl).to.exist;
            expect(resp.data.jcr.nodeByPath.renderUrl).to.be.equal(`/sites/${sitename}/home.html`);
        });

        cy.apollo({
            queryFile: 'jcr/renderUrl.graphql',
            variables: {
                path: `/sites/${sitename}/home/page1`,
                workspace: 'LIVE',
                lang: 'en'
            }
        }).should(resp => {
            expect(resp.data.jcr.nodeByPath.renderUrl).to.exist;
            expect(resp.data.jcr.nodeByPath.renderUrl).to.be.equal(`/sites/${sitename}/home/page1.html`);
        });

        cy.apollo({
            queryFile: 'jcr/renderUrl.graphql',
            variables: {
                path: `/sites/${sitename}/home/page1/page2`,
                workspace: 'LIVE',
                lang: 'en'
            }
        }).should(resp => {
            expect(resp.data.jcr.nodeByPath.renderUrl).to.exist;
            expect(resp.data.jcr.nodeByPath.renderUrl).to.be.equal(`/sites/${sitename}/home/page1/page2.html`);
        });
    });

    it('Returns correct values for default workspaces', () => {
        cy.apollo({
            queryFile: 'jcr/renderUrl.graphql',
            variables: {
                path: `/sites/${sitename}/home`,
                workspace: 'EDIT',
                lang: 'en'
            }
        }).should(resp => {
            expect(resp.data.jcr.nodeByPath.renderUrl).to.exist;
            expect(resp.data.jcr.nodeByPath.renderUrl).to.be.equal(`/cms/render/default/en/sites/${sitename}/home.html`);
        });

        cy.apollo({
            queryFile: 'jcr/renderUrl.graphql',
            variables: {
                path: `/sites/${sitename}/home/page1`,
                workspace: 'EDIT',
                lang: 'en'
            }
        }).should(resp => {
            expect(resp.data.jcr.nodeByPath.renderUrl).to.exist;
            expect(resp.data.jcr.nodeByPath.renderUrl).to.be.equal(`/cms/render/default/en/sites/${sitename}/home/page1.html`);
        });

        cy.apollo({
            queryFile: 'jcr/renderUrl.graphql',
            variables: {
                path: `/sites/${sitename}/home/page1/page2`,
                workspace: 'EDIT',
                lang: 'en'
            }
        }).should(resp => {
            expect(resp.data.jcr.nodeByPath.renderUrl).to.exist;
            expect(resp.data.jcr.nodeByPath.renderUrl).to.be.equal(`/cms/render/default/en/sites/${sitename}/home/page1/page2.html`);
        });
    });

    it('Returns correct values for nodes which have vanity urls defined', () => {
        addVanityUrl(`/sites/${sitename}/home/page1`, 'en', '/my-page1');
        publishAndWaitJobEnding(`/sites/${sitename}/home/page1`);

        cy.apollo({
            queryFile: 'jcr/renderUrl.graphql',
            variables: {
                path: `/sites/${sitename}/home/page1`,
                workspace: 'EDIT',
                lang: 'en'
            }
        }).should(resp => {
            expect(resp.data.jcr.nodeByPath.renderUrl).to.exist;
            expect(resp.data.jcr.nodeByPath.renderUrl).to.be.equal(`/cms/render/default/en/sites/${sitename}/home/page1.html`);
        });

        cy.apollo({
            queryFile: 'jcr/renderUrl.graphql',
            variables: {
                path: `/sites/${sitename}/home/page1`,
                workspace: 'LIVE',
                lang: 'en'
            }
        }).should(resp => {
            expect(resp.data.jcr.nodeByPath.renderUrl).to.exist;
            expect(resp.data.jcr.nodeByPath.renderUrl).to.be.equal(`/sites/${sitename}/home/page1.html`);
        });
    });
});
