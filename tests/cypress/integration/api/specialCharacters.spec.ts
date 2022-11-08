import gql from 'graphql-tag'
import {validateError} from "./jcr/validateErrors";

describe('Test GraphQL special characters', () => {
    before('create a list with 2 sub children', () => {
        cy.apollo({
            mutation: gql`
                mutation {
                    jcr(workspace: EDIT) {
                        addNode(parentPathOrId: "/", name: "testCharsList", primaryNodeType: "jnt:contentList") {
                            subnode1: addChild(name: "testCharSubList", primaryNodeType: "jnt:contentList") {
                                subSubnode1: addChild(name: "testCharSubSubList", primaryNodeType: "jnt:contentList") {
                                    uuid
                                }
                            }
                        }
                    }
                }
            `,
        })
    })

    after('Delete list created in the before', function () {
        cy.apollo({
            mutationFile: 'jcr/deleteNode.graphql',
            variables: {
                pathOrId: '/testCharsList',
            },
        })
    })

    it('Test []*|/%', () => {
        cy.apollo({
            mutation: gql`
                mutation {
                    jcr(workspace: EDIT) {
                        addNode(parentPathOrId: "/testCharsList/testCharSubList", name: "[]*|/%", primaryNodeType: "jnt:contentList") {
                            node {
                                path
                            }
                        }
                    }
                }
            `,
        }).should((result) => {
            const path = result?.data?.jcr?.addNode?.node?.path
            expect(path).to.be.equal("/testCharsList/testCharSubList/%5B%5D%2A%7C %");
        })

    })

    it('Test .', () => {
        cy.apollo({
            mutation: gql`
                mutation {
                    jcr(workspace: EDIT) {
                        addNode(parentPathOrId: "/testCharsList/testCharSubList", name: ".", primaryNodeType: "jnt:contentList") {
                            node {
                                path
                            }
                        }
                    }
                }
            `,
            errorPolicy: 'all'
        }).should((result) => {
            validateError(
                result,
                `javax.jcr.ItemExistsException: This node already exists: /testCharsList/testCharSubList`,
            )
        })

    })

    it('Test ..', () => {
        cy.apollo({
            mutation: gql`
                mutation {
                    jcr(workspace: EDIT) {
                        addNode(parentPathOrId: "/testCharsList/testCharSubList/testCharSubSubList", name: ".", primaryNodeType: "jnt:contentList") {
                            node {
                                path
                            }
                        }
                    }
                }
            `,
            errorPolicy: 'all'
        }).should((result) => {
            validateError(
                result,
                `javax.jcr.ItemExistsException: This node already exists: /testCharsList/testCharSubList`,
            )
        })

    })

})
