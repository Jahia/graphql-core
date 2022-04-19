import gql from 'graphql-tag'

describe('Node validity graphql test', () => {
    before('create nodes with validity constrains', () => {
        // Setup data for testing
        cy.apollo({
            mutationFile: 'jcr/createNodesForValidity.graphql',
        })

        // With visibility constraint
        cy.apollo({
            mutation: gql`
                mutation {
                    jcr {
                        mutateNodes(pathsOrIds: ["/sites/systemsite/testValidity/visibility"]) {
                            addChild(name: "j:conditionalVisibility", primaryNodeType: "jnt:conditionalVisibility") {
                                addChild(
                                    name: "jnt:startEndDateCondition1650027997279"
                                    primaryNodeType: "jnt:startEndDateCondition"
                                    properties: [
                                        { name: "start", value: "1980-03-29T15:06:00.000Z" }
                                        { name: "end", value: "2020-03-31T15:06:00.000Z" }
                                    ]
                                ) {
                                    uuid
                                }
                            }
                        }
                    }
                }
            `,
        })
        // Part of inactive language at node level
        cy.apollo({
            mutation: gql`
                mutation {
                    jcr {
                        mutateNodes(pathsOrIds: ["/sites/systemsite/testValidity/with-inactive-language"]) {
                            m1: mutateProperty(name: "j:invalidLanguages") {
                                setValues(values: ["en"])
                            }
                            m2: mutateProperty(name: "text") {
                                setValue(language: "fr", value: "text in french")
                            }
                            m3: mutateProperty(name: "text") {
                                setValue(language: "en", value: "text in english")
                            }
                        }
                    }
                }
            `,
        })
        // Publish everything
        cy.apollo({
            mutation: gql`
                mutation {
                    jcr {
                        mutateNodes(pathsOrIds: ["/sites/systemsite/testValidity"]) {
                            publish(languages: ["en", "fr"], publishSubNodes: true, includeSubTree: true)
                        }
                    }
                }
            `,
        })
        // Unpublish node
        cy.apollo({
            mutation: gql`
                mutation {
                    jcr {
                        mutateNodes(pathsOrIds: ["/sites/systemsite/testValidity/unpublished"]) {
                            unpublish(languages: ["en", "fr"])
                        }
                    }
                }
            `,
        })
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
