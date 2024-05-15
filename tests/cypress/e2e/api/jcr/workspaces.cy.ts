import {addNode, deleteNode, publishAndWaitJobEnding} from '@jahia/cypress';
import gql from 'graphql-tag';

describe('GraphQL Worskspace Test', () => {
    before('Create test nodes', () => {
        addNode({
            parentPathOrId: '/',
            name: 'testList',
            primaryNodeType: 'jnt:contentList'
        })
            .then(() =>
                addNode({
                    parentPathOrId: '/testList',
                    name: 'testSubList1',
                    primaryNodeType: 'jnt:contentList'
                })
            )
            .then(() =>
                addNode({
                    parentPathOrId: '/testList',
                    name: 'testSubList2',
                    primaryNodeType: 'jnt:contentList'
                })
            )
            .then(() => publishAndWaitJobEnding('/testList/testSubList1'))
            .then(() =>
                cy.apollo({
                    query: gql`
                        mutation {
                            jcr(workspace: EDIT) {
                                mutateNode(pathOrId: "/testList/testSubList1") {
                                    rename(name: "testSubList3")
                                }
                            }
                        }
                    `
                })
            );
    });
    it('should retrieve nodes from default', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        testSubList2: nodeByPath(path: "/testList/testSubList2") {
                            name
                        }
                        testSubList3: nodeByPath(path: "/testList/testSubList3") {
                            name
                        }
                    }
                }
            `
        }).should(response => {
            const jcr = response?.data?.jcr;
            const subList2 = jcr?.testSubList2;
            const subList3 = jcr?.testSubList3;
            expect(subList2?.name).to.equal('testSubList2');
            expect(subList3?.name).to.equal('testSubList3');
        });
    });
    it('should retrieve node from live', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr(workspace: LIVE) {
                        testSubList1: nodeByPath(path: "/testList/testSubList1") {
                            name
                        }
                    }
                }
            `
        }).should(result => {
            const subList1 = result?.data?.jcr?.testSubList1;
            expect(subList1?.name).to.equal('testSubList1');
        });
    });
    it('should retrieve live counterpart of node', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        testSubList3: nodeByPath(path: "/testList/testSubList3") {
                            name
                            nodeInWorkspace(workspace: LIVE) {
                                name
                            }
                        }
                    }
                }
            `
        }).should(result => {
            const subList3Default = result?.data?.jcr?.testSubList3;
            const subList1Live = subList3Default?.nodeInWorkspace;
            expect(subList3Default?.name).to.equal('testSubList3');
            expect(subList1Live?.name).to.equal('testSubList1');
        });
    });
    it('should not retrieve live counterpart of node', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        testSubList2: nodeByPath(path: "/testList/testSubList2") {
                            name
                            nodeInWorkspace(workspace: LIVE) {
                                name
                            }
                        }
                    }
                }
            `
        }).should(result => {
            const subList2Default = result?.data?.jcr?.testSubList2;
            expect(subList2Default?.name).to.equal('testSubList2');
            expect(subList2Default?.nodeInWorkspace).to.equal(null);
        });
    });
    it('should retrieve EDIT workspace fields', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        workspace
                        testSubList3: nodeByPath(path: "/testList/testSubList3") {
                            name
                            workspace
                            nodeInWorkspace(workspace: LIVE) {
                                name
                                workspace
                            }
                        }
                    }
                }
            `
        }).should(result => {
            const subList3Default = result?.data?.jcr?.testSubList3;
            const subList1Live = subList3Default?.nodeInWorkspace;

            expect(result?.data?.jcr?.workspace).to.equal('EDIT');
            expect(subList3Default?.workspace).to.equal('EDIT');
            expect(subList1Live?.workspace).to.equal('LIVE');
        });
    });
    it('should retrieve live workspace fields', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr(workspace: LIVE) {
                        workspace
                        testSubList1: nodeByPath(path: "/testList/testSubList1") {
                            name
                            workspace
                            nodeInWorkspace(workspace: EDIT) {
                                name
                                workspace
                            }
                        }
                    }
                }
            `
        }).should(result => {
            const subList1Live = result?.data?.jcr?.testSubList1;
            const subList3Default = subList1Live?.nodeInWorkspace;

            expect(result?.data?.jcr?.workspace).to.equal('LIVE');
            expect(subList1Live?.workspace).to.equal('LIVE');
            expect(subList3Default?.workspace).to.equal('EDIT');
        });
    });
    after('Remove test nodes', () => {
        deleteNode('/testList');
        cy.apollo({
            query: gql`
                mutation {
                    jcr(workspace: LIVE) {
                        deleteNode(pathOrId: "/testList")
                    }
                }
            `
        });
    });
});
