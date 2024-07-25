import {addNode, addVanityUrl, createSite, deleteSite, publishAndWaitJobEnding, editSite} from '@jahia/cypress';

const sitename = 'graphql_test_render_url';

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

    // Note that for this to work you must run it with a host other than localhost (e.g. http://jahia:8080)
    it('Returns correct values for nodes which have vanity urls defined', () => {
        const vanity = '/my-page1';
        editSite(sitename, {serverName: 'jahia'});
        addVanityUrl(`/sites/${sitename}/home/page1`, 'en', vanity);
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
            expect(resp.data.jcr.nodeByPath.renderUrl).to.be.equal(`/cms/render/default${vanity}`);
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
            expect(resp.data.jcr.nodeByPath.renderUrl).to.be.equal(`${vanity}`);
        });
    });
});
