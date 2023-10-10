/* eslint-disable @typescript-eslint/no-explicit-any */

describe('Test page properties', () => {
    before('load graphql file and create node', () => {
        cy.apollo({
            mutationFile: 'jcr/addNode.graphql',
            variables: {
                parentPathOrId: '/sites/systemsite/home',
                nodeName: 'testPage',
                nodeType: 'jnt:page',
                properties: [
                    {name: 'j:templateName', type: 'STRING', value: 'default', language: 'en'},
                    {name: 'jcr:title', type: 'STRING', value: 'test Page', language: 'en'},
                    {
                        name: 'j:isHomePage',
                        type: 'BOOLEAN',
                        value: false,
                        language: 'en'
                    }
                ]
            }
        });
    });

    it('Get a page by path and verify isHomePage has a boolean value', () => {
        cy.apollo({
            queryFile: 'jcr/pageByPath.graphql',
            variables: {path: '/sites/systemsite/home/testPage'}
        }).should((response: any) => {
            expect(response.data.jcr.nodeByPath).to.exist;
            expect(response.data.jcr.nodeByPath.name).to.equal('testPage');
            expect(response.data.jcr.nodeByPath.isHomePage.booleanValue).to.equal(false);
        });
    });

    after('Delete testPage node', function () {
        cy.apollo({
            mutationFile: 'jcr/deleteNode.graphql',
            variables: {
                pathOrId: '/sites/systemsite/home/testPage'
            }
        });
    });
});
