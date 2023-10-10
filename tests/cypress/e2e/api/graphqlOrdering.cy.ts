import gql from 'graphql-tag';

describe('Test graphql ordering', () => {
    before('create a list with 3 children', () => {
        cy.apollo({
            mutation: gql`
                mutation {
                    jcr(workspace: EDIT) {
                        addNode(parentPathOrId: "/", name: "testList", primaryNodeType: "jnt:contentList") {
                            subnode1: addChild(name: "Hello", primaryNodeType: "jnt:bigText") {
                                uuid
                            }
                            subnode2: addChild(name: "Bonjour", primaryNodeType: "jnt:press") {
                                uuid
                            }
                            subnode3: addChild(name: "Hola", primaryNodeType: "jnt:linkList") {
                                uuid
                            }
                        }
                    }
                }
            `
        });
    });

    after('Delete list created in the before', function () {
        cy.apollo({
            mutationFile: 'jcr/deleteNode.graphql',
            variables: {
                pathOrId: '/testList'
            }
        });
    });

    it('Should order by nodeType', () => {
        cy.apollo({
            queryFile: 'orderingList.graphql',
            variables: {
                orderType: 'ASC',
                nodeType: 'jnt:content',
                property: 'jcr:primaryType'
            }
        }).should(result => {
            const orderedList1 = result?.data?.jcr?.nodesByCriteria?.nodes;
            expect(orderedList1).to.have.length(3);
            expect(orderedList1[0].primaryNodeType).to.have.property('name', 'jnt:bigText');
            expect(orderedList1[1].primaryNodeType).to.have.property('name', 'jnt:linkList');
            expect(orderedList1[2].primaryNodeType).to.have.property('name', 'jnt:press');
        });

        cy.apollo({
            queryFile: 'orderingList.graphql',
            variables: {
                orderType: 'DESC',
                nodeType: 'jnt:content',
                property: 'jcr:primaryType'
            }
        }).should(result => {
            const orderedList1 = result?.data?.jcr?.nodesByCriteria?.nodes;
            expect(orderedList1).to.have.length(3);
            expect(orderedList1[0].primaryNodeType).to.have.property('name', 'jnt:press');
            expect(orderedList1[1].primaryNodeType).to.have.property('name', 'jnt:linkList');
            expect(orderedList1[2].primaryNodeType).to.have.property('name', 'jnt:bigText');
        });
    });
});
