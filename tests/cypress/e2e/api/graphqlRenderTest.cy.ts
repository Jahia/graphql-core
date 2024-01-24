import {addNode, createSite, deleteNode, deleteSite} from '@jahia/cypress';

const sitename = 'graphql_test_render';
describe('Test graphql rendering', () => {

    before('Create a site', () => {
        createSite(sitename);
    });

    after('Delete the site', () => {
        deleteSite(sitename);
    });

    it('Check if node (page) is displayable', () => {
        addNode({
            parentPathOrId: '/sites/' + sitename + '/home',
            name: 'page1',
            primaryNodeType: 'jnt:page',
            properties: [
                {name: 'jcr:title', value: 'page1'},
                {name: 'j:templateName', value: 'simple'}
            ]
        })
        cy.apollo({
            queryFile: 'jcr/displayableNode.graphql',
            variables: {
                path: '/sites/' + sitename + '/home/page1'
            }
        }).should(result => {
            const property = result?.data?.jcr?.nodeByPath?.isDisplayableNode;
            expect(property).true;
        });
        deleteNode('/sites/' + sitename + '/home/page1');
    });

    it('Check if node (list) is NOT displayable', () => {
        addNode({
            parentPathOrId: '/sites/' + sitename + '/home',
            name: 'listA',
            primaryNodeType: 'jnt:containerList'
        })
        cy.apollo({
            queryFile: 'jcr/displayableNode.graphql',
            variables: {
                path: '/sites/' + sitename + '/home/listA'
            }
        }).should(result => {
            const property = result?.data?.jcr?.nodeByPath?.isDisplayableNode;
            expect(property).false;
        });
        deleteNode('/sites/' + sitename + '/home/listA');
    });
});
