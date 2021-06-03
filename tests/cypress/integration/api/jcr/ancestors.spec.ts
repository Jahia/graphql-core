/* eslint-disable @typescript-eslint/no-explicit-any */
import { DocumentNode } from 'graphql'
import gql from 'graphql-tag'

describe('Test admin user endpoint', () => {
    let GQL_DELETENODE: DocumentNode
    let GQL_NODEBYPATHPARENT: DocumentNode
    let GQL_NODEBYPATHANCESTORS: DocumentNode

    before('load graphql file and create test dataset', () => {
        GQL_DELETENODE = require(`graphql-tag/loader!../../../fixtures/jcr/deleteNode.graphql`)
        GQL_NODEBYPATHPARENT = require(`graphql-tag/loader!../../../fixtures/jcr/nodeByPathParentAncestors.graphql`)
        GQL_NODEBYPATHANCESTORS = require(`graphql-tag/loader!../../../fixtures/jcr/nodeByPathAncestors.graphql`)

        cy.log('Preparing the test suite dataset: createList')
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            mode: 'mutate',
            query: gql`
                mutation {
                    jcr(workspace: EDIT) {
                        addNode(parentPathOrId: "/", name: "testList", primaryNodeType: "jnt:contentList") {
                            uuid
                            addChild(name: "testSubList", primaryNodeType: "jnt:contentList") {
                                uuid
                                addChild(name: "testSubSubList", primaryNodeType: "jnt:contentList") {
                                    uuid
                                }
                            }
                        }
                    }
                }
            `,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.addNode.uuid).not.to.be.null
            expect(response.data.jcr.addNode.addChild.uuid).not.to.be.null
            expect(response.data.jcr.addNode.addChild.addChild.uuid).not.to.be.null
        })
    })

    after('Clear the created dataset', () => {
        cy.log('Preparing the test suite dataset: createList')
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            mode: 'mutate',
            variables: {
                pathOrId: '/testList',
            },
            query: GQL_DELETENODE,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.deleteNode).to.be.true
        })
    })

    it('shouldRetrieveParent', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                path: '/testList/testSubList/testSubSubList',
            },
            query: GQL_NODEBYPATHPARENT,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodeByPath.parent.name).to.equal('testSubList')
        })
    })

    it('shouldRetrieveAllAncestors', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                path: '/testList/testSubList/testSubSubList',
            },
            query: GQL_NODEBYPATHPARENT,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodeByPath.ancestors[0].name).to.equal('')
            expect(response.data.jcr.nodeByPath.ancestors[1].name).to.equal('testList')
            expect(response.data.jcr.nodeByPath.ancestors[2].name).to.equal('testSubList')
        })
    })

    it('shouldRetrieveAncestorsUpToPath', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                path: '/testList/testSubList/testSubSubList',
                upToPath: '/testList',
            },
            query: GQL_NODEBYPATHANCESTORS,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodeByPath.ancestors[0].name).to.equal('testList')
            expect(response.data.jcr.nodeByPath.ancestors[1].name).to.equal('testSubList')
        })
    })

    it('shouldGetErrorNotRetrieveAncestorsWhenUpToPathIsEmpty', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                path: '/testList/testSubList/testSubSubList',
                upToPath: '',
            },
            query: GQL_NODEBYPATHANCESTORS,
        }).then((response: any) => {
            console.log(response)
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodeByPath).to.be.null
            expect(response.errors[0].message).to.equal("'' is not a valid node path")
        })
    })

    it('shouldGetErrorNotRetrieveAncestorsWhenUpToPathIsNotAncestorPath', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                path: '/testList/testSubList/testSubSubList',
                upToPath: '/nonExistingPath',
            },
            query: GQL_NODEBYPATHANCESTORS,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodeByPath).to.be.null
            expect(response.errors[0].message).to.equal(
                "'/nonExistingPath' does not reference an ancestor node of '/testList/testSubList/testSubSubList'",
            )
        })
    })
    it('shouldGetErrorNotRetrieveAncestorsWhenUpToPathIsThisNodePath', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                path: '/testList/testSubList/testSubSubList',
                upToPath: '/testList/testSubList/testSubSubList',
            },
            query: GQL_NODEBYPATHANCESTORS,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodeByPath).to.be.null
            expect(response.errors[0].message).to.equal(
                "'/testList/testSubList/testSubSubList' does not reference an ancestor node of '/testList/testSubList/testSubSubList'",
            )
        })
    })
})
