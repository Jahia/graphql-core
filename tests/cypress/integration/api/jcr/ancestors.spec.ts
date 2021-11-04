import gql from "graphql-tag";
import {validateNode} from "./validateNode";
import {validateError} from "./validateErrors";

describe('Ancestors graphql test', () => {

    before(() => {
        cy.apollo({
            mutation: gql`mutation {
                jcr {
                    addNode(parentPathOrId: "/", name: "testList", primaryNodeType: "jnt:contentList") {
                        addChild(name: "testSubList", primaryNodeType: "jnt:contentList") {
                            addChild(name: "testSubSubList", primaryNodeType: "jnt:contentList") {
                                uuid
                            }
                            uuid
                        }
                        uuid
                    }
                }
            }`
        })
    })

    after(() => {
        cy.apollo({
            mutation: gql`mutation {
                jcr {
                    deleteNode(pathOrId: "/testList")
                }
            }`
        })
    })

    it('should retrieve parent', () => {
        cy.apollo({
            queryFile: 'jcr/getParentAndAncestors.graphql',
            variables: {path: '/testList/testSubList/testSubSubList'}
        }).should(result => {
            const parent = result?.data?.jcr?.nodeByPath?.parent
            validateNode(parent, 'testSubList');
        })
    })

    it('should retrieve all ancestors', () => {
        cy.apollo({
            queryFile: 'jcr/getParentAndAncestors.graphql',
            variables: {path: '/testList/testSubList/testSubSubList'}
        }).should(result => {
            const ancestors = result?.data?.jcr?.nodeByPath?.ancestors
            expect(ancestors).to.have.length(3)
            validateNode(ancestors[0], '');
            validateNode(ancestors[1], 'testList');
            validateNode(ancestors[2], 'testSubList');
        })
    })

    it('should ancestors up to path', () => {
        cy.apollo({
            queryFile: 'jcr/getParentAndAncestors.graphql',
            variables: {path: '/testList/testSubList/testSubSubList', upToPath: '/testList'}
        }).should(result => {
            const ancestors = result?.data?.jcr?.nodeByPath?.ancestors
            expect(ancestors).to.have.length(2)
            validateNode(ancestors[0], 'testList');
            validateNode(ancestors[1], 'testSubList');
        })
    })

    it('should get error not retrieve ancestors when upToPath is empty', () => {
        cy.apollo({
            queryFile: 'jcr/getParentAndAncestors.graphql',
            variables: {path: '/testList/testSubList/testSubSubList', upToPath: ''},
            errorPolicy: 'all'
        }).should(result => {
            validateError(result, `'' is not a valid node path`);
        })
    })

    it('should get error not retrieve ancestors when upToPath is not ancestor path', () => {
        cy.apollo({
            queryFile: 'jcr/getParentAndAncestors.graphql',
            variables: {path: '/testList/testSubList/testSubSubList', upToPath: '/nonExistingPath'},
            errorPolicy: 'all'
        }).should(result => {
            validateError(result, `'/nonExistingPath' does not reference an ancestor node of '/testList/testSubList/testSubSubList'`);
        })
    })

    it('should get error not retrieve ancestors when upToPath Is this node path', () => {
        cy.apollo({
            queryFile: 'jcr/getParentAndAncestors.graphql',
            variables: {path: '/testList/testSubList/testSubSubList', upToPath: '/testList/testSubList/testSubSubList'},
            errorPolicy: 'all'
        }).should(result => {
            validateError(result, `'/testList/testSubList/testSubSubList' does not reference an ancestor node of '/testList/testSubList/testSubSubList'`);
        })
    })
})