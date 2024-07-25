import {addNode, addVanityUrl, createSite, deleteSite} from '@jahia/cypress';
import gql from 'graphql-tag';

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
            parentPathOrId: '/sites/' + sitename + '/home/page2',
            name: 'page2',
            primaryNodeType: 'jnt:page',
            properties: [
                {name: 'jcr:title', value: 'page2'},
                {name: 'jcr:title', value: 'page2'},
                {name: 'j:templateName', value: 'simple'}
            ]
        });

        cy.apollo({
            mutation: gql`
                mutation publishSite {
                    jcr {
                        mutateNode(
                            pathOrId: '/sites/${sitename}'
                        ) {
                            publish(publishSubNodes: true)
                        }
                    }
                }
            `
        });
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
                workspace: 'LIVE',
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
                workspace: 'LIVE',
                lang: 'en'
            }
        }).should(resp => {
            expect(resp.data.jcr.nodeByPath.renderUrl).to.exist;
            expect(resp.data.jcr.nodeByPath.renderUrl).to.be.equal(`/cms/render/default/en/sites/${sitename}/home/page1/page2.html`);
        });
    });
});
