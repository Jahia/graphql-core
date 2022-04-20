import gql from 'graphql-tag'

describe('Node validity graphql test', () => {
    before('create nodes with validity constrains', () => {
        // Setup data for testing
        console.log('run groovy script')
        cy.executeGroovy('groovy/prepareValidityTest.groovy', {})
    })
    after('clean up test data', () => {
        cy.apollo({
            mutation: gql`
                mutation {
                    jcr {
                        mutateNodes(pathsOrIds: ["/sites/systemsite/testValidity"]) {
                            delete
                        }
                    }
                }
            `,
        })
        cy.apollo({
            errorPolicy: 'all',
            mutation: gql`
                mutation {
                    jcr {
                        mutateNodes(pathsOrIds: ["/sites/systemsite"]) {
                            publish
                        }
                    }
                }
            `,
        })
    })
    const invalidPath = [
        '/sites/systemsite/testValidity/visibility',
        '/sites/systemsite/testValidity/unpublished',
        '/sites/systemsite/testValidity/with-inactive-language',
    ]

    invalidPath.forEach((path: string) =>
        it(`[nodeByPath] should not return a node ${path}`, function () {
            // getNodeByPath
            cy.apollo({
                errorPolicy: 'all',
                query: gql`
                    query {
                        jcr(workspace: LIVE) {
                            nodeByPath(path: "${path}", validInLanguage: "en") {
                                path
                            }
                        }
                    }
                `,
            }).should((response) => {
                expect(response.errors[0].message).to.contain('javax.jcr.PathNotFoundException')
            })
        }),
    )

    it('[children] should return only valid nodes in EN', function () {
        cy.apollo({
            query: gql`
                query {
                    jcr(workspace: LIVE) {
                        nodeByPath(path: "/sites/systemsite/testValidity") {
                            children(validInLanguage: "en") {
                                nodes {
                                    path
                                }
                            }
                        }
                    }
                }
            `,
        }).should((response) => {
            const result = response.data.jcr.nodeByPath.children.nodes
            expect(result.length).to.equal(1)
            expect(result[0].path).to.equal('/sites/systemsite/testValidity/controlNode')
        })
    })
    it('[children] should return only valid nodes in FR', function () {
        cy.apollo({
            query: gql`
                query {
                    jcr(workspace: LIVE) {
                        nodeByPath(path: "/sites/systemsite/testValidity") {
                            children(validInLanguage: "fr") {
                                nodes {
                                    path
                                }
                            }
                        }
                    }
                }
            `,
        }).should((response) => {
            const result = response.data.jcr.nodeByPath.children.nodes
            expect(result.length).to.equal(2)
            expect(result[0].path).to.equal('/sites/systemsite/testValidity/controlNode')
            expect(result[1].path).to.equal('/sites/systemsite/testValidity/with-inactive-language')
        })
    })

    it('[descendants] should return only valid nodes in EN', function () {
        cy.apollo({
            query: gql`
                query {
                    jcr(workspace: LIVE) {
                        nodeByPath(path: "/sites/systemsite/testValidity") {
                            descendants(validInLanguage: "en") {
                                nodes {
                                    path
                                }
                            }
                        }
                    }
                }
            `,
        }).should((response) => {
            const result = response.data.jcr.nodeByPath.descendants.nodes
            expect(result.length).to.equal(1)
            expect(result[0].path).to.equal('/sites/systemsite/testValidity/controlNode')
        })
    })
    it('[descendants] should return only valid nodes in FR', function () {
        cy.apollo({
            query: gql`
                query {
                    jcr(workspace: LIVE) {
                        nodeByPath(path: "/sites/systemsite/testValidity") {
                            descendants(validInLanguage: "fr") {
                                nodes {
                                    path
                                }
                            }
                        }
                    }
                }
            `,
        }).should((response) => {
            const result = response.data.jcr.nodeByPath.descendants.nodes
            expect(result.length).to.equal(2)
            expect(result[0].path).to.equal('/sites/systemsite/testValidity/controlNode')
            expect(result[1].path).to.equal('/sites/systemsite/testValidity/with-inactive-language')
        })
    })
})
