import gql from 'graphql-tag';
import {
    addNode,
    createUser,
    deleteUser,
    grantRoles,
    revokeRoles,
    setNodeProperty,
    addUserToGroup
} from '@jahia/cypress';

describe('Test graphql permissions', () => {
    let subList2Cursor: string;

    before('create user and test data', () => {
        createUser('testUser', 'testPassword');
        addUserToGroup('testUser', 'privileged');

        /*
        Test data to create:
        /testList (jnt:contentList) - owner: true
            /testSubList1 (jnt:contentList) - owner: false
            /testSubList2 (jnt:contentList) - owner: true
            /reference1 (jnt:contentReference) - owner: false
                [j:node=ref->/testSubList2]
            /reference2 (jnt:contentReference) - owner: true
                [j:node=ref->/testSubList2]
         */

        addNode({
            parentPathOrId: '/',
            name: 'testList',
            primaryNodeType: 'jnt:contentList',
            children: [
                {
                    name: 'testSubList1',
                    primaryNodeType: 'jnt:contentList'
                },
                {
                    name: 'testSubList2',
                    primaryNodeType: 'jnt:contentList'
                },
                {
                    name: 'reference1',
                    primaryNodeType: 'jnt:contentReference'
                },
                {
                    name: 'reference2',
                    primaryNodeType: 'jnt:contentReference'
                }
            ]
        })
            .then(response => {
                // Keep reference to testSubList2
                subList2Cursor = response.data.jcr.addNode.addChildrenBatch[1].uuid;
                setNodeProperty('/testList/reference1', 'j:node', subList2Cursor, 'en');
                setNodeProperty('/testList/reference2', 'j:node', subList2Cursor, 'en');
            })
            .then(() => {
                // Tweak the permissions
                grantRoles('/testList', ['owner'], 'testUser', 'USER');
                grantRoles('/testList', ['privileged'], 'testUser', 'USER');
                revokeRoles('/testList/testSubList1', ['owner'], 'testUser', 'USER');
                revokeRoles('/testList/reference1', ['owner'], 'testUser', 'USER');
            }
            );
    });

    after('Delete user and test data', () => {
        deleteUser('testUser');
        cy.apolloClient() // Use root user to perform cleanup operations
            .apollo({
                mutationFile: 'jcr/deleteNode.graphql',
                variables: {
                    pathOrId: '/testList'
                }
            });
    });

    it('Should get error not retrieve protected node', () => {
        cy.apolloClient({username: 'testUser', password: 'testPassword'})
            .apollo({
                query: gql`
                query {
                    jcr {
                        nodeByPath(path: "/testList/testSubList1") {
                            uuid
                        }
                    }
                }
            `
            })
            .should(result => {
                expect(result.graphQLErrors).to.exist;
                expect(result.graphQLErrors).to.have.length(1);
                expect(result.graphQLErrors[0].message).to.contain('Permission denied');
            });
    });

    it('Should retrieve filtered child nodes', () => {
        cy.apolloClient({username: 'testUser', password: 'testPassword'})
            .apollo({
                query: gql`
                query {
                    jcr {
                        nodeByPath(path: "/testList") {
                            children {
                                nodes {
                                    name
                                }
                            }
                        }
                    }
                }
            `
            }).should(result => {
                const children = result?.data?.jcr?.nodeByPath?.children?.nodes as Array<{ name: string }>;
                const filteredChildren = filterNodes(children);
                expect(filteredChildren).to.be.length(2);
                expect(filteredChildren[0].name).to.equal('testSubList2');
                expect(filteredChildren[1].name).to.equal('reference2');
            });
    });

    it('Should retrieve filtered descendant', () => {
        cy.apolloClient({username: 'testUser', password: 'testPassword'})
            .apollo({
                query: gql`
                query {
                    jcr {
                        nodeByPath(path: "/testList") {
                            descendants {
                                nodes {
                                    name
                                }
                            }
                        }
                    }
                }
            `
            }).should(result => {
                const descendants = result?.data?.jcr?.nodeByPath?.descendants?.nodes as Array<{ name: string }>;
                const filteredDescendants = filterNodes(descendants);
                expect(filteredDescendants).to.be.length(2);
                expect(filteredDescendants[0].name).to.equal('testSubList2');
                expect(filteredDescendants[1].name).to.equal('reference2');
            });
    });

    it('Should retrieve filtered references', () => {
        cy.apolloClient({username: 'testUser', password: 'testPassword'})
            .apollo({
                query: gql`
                query {
                    jcr {
                        nodeByPath(path: "/testList/testSubList2") {
                            references {
                                nodes {
                                    node {
                                        name
                                    }
                                }
                            }
                        }
                    }
                }
            `
            }).should(result => {
                const references = result?.data?.jcr?.nodeByPath?.references?.nodes as Array<{ node: { name: string } }>;
                expect(references).to.be.length(1);
                expect(references[0].node.name).to.equal('reference2');
            });
    });
});

function filterNodes(children: Array<{ name: string }>) {
    return children.filter(n => n.name !== 'j:acl' && !n.name.startsWith('GRANT'));
}
