/* eslint-disable @typescript-eslint/no-explicit-any */

import gql from "graphql-tag";

describe('Test page properties', () => {
    let nodeUuid: string
    let nodeTitleFr: string = "text FR";
    let nodeTitleEn: string = "text EN";
    let subNodeUuid1: string
    let subNodeUuid2: string

    before('load graphql file and create nodes', () => {
        cy.apollo({
            mutation: gql`mutation {
                jcr {
                    addNode(parentPathOrId: "/", name: "testList", primaryNodeType: "jnt:contentList", properties: [
                        {
                            name: "jcr:title",
                            value: ${nodeTitleEn},
                            language: "en",
                        }
                        {
                            name: "jcr:title",
                            value: ${nodeTitleFr},
                            language: "fr",
                        }
                    ]) {
                        child1:addChild(name: "testSubList1", primaryNodeType: "jnt:contentList") {
                            uuid
                        }
                        child2:addChild(name: "testSubList2", primaryNodeType: "jnt:contentList") {
                            uuid
                        }
                        uuid
                    }
                }
            }`
        }).then((response: any) => {
            subNodeUuid1 = response.data.jcr.addNode.child1.uuid
            subNodeUuid1 = response.data.jcr.addNode.child2.uuid
        })
    })

    it('Get main node', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeByPath(path: "/testList") {
                            name
                            path
                            uuid
                            displayName
                            titleen:property(name: "jcr:title" language:"en") {
                                value
                            }
                            titlefr:property(name: "jcr:title" language:"fr") {
                                value
                            }
                        }
                    }
                }`
            }).should((response: any) => {
                expect(response.data.jcr.nodeByPath).to.exist
                expect(response.data.jcr.nodeByPath.name).to.equal('testList')
                expect(response.data.jcr.nodeByPath.path).to.equal('/testList')
                expect(response.data.jcr.nodeByPath.uuid).to.equal(nodeUuid)
                expect(response.data.jcr.nodeByPath.displayName).to.equal('testList')
                expect(response.data.jcr.nodeByPath.titlefr).to.equal(nodeTitleFr)
                expect(response.data.jcr.nodeByPath.titleen).to.equal(nodeTitleEn)
            })
    })

    it('Get child node by path', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeByPath(path: "/testList/testSubList2") {
                            name
                        }
                    }
                }`
        }).should((response: any) => {
            expect(response.data.jcr.nodeByPath).to.exist
            expect(response.data.jcr.nodeByPath.name).to.equal('testSubList2')
        })
    })

    it('Get an error when trying to get child node with wrong path', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeByPath(path: "/testList/wrongPath") {
                            name
                        }
                    }
                }`
        }).should(result => {
            validateError(result, `javax.jcr.PathNotFoundException: /testList/wrongPath`)
        })
    })

    it('Get an error when trying to get child node in live', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr (workspace: LIVE) {
                        nodeByPath(path: "/testList/testSubList2") {
                            name
                        }
                    }
                }`
        }).should(result => {
            validateError(result, `javax.jcr.PathNotFoundException: /testList/testSubList2`)
        })
    })

    it('Get child nodes by path', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodesByPath(paths: ["/testList/testSubList2", "/testList/testSubList1"]) {
                            name
                        }
                    }
                }`
        }).should(result => {
            const nodes = result?.data?.jcr?.nodesByPath?.name
            expect(nodes).to.have.length(2)
            validateNode(nodes[0], "testSubList2")
            validateNode(nodes[1], "testSubList1")
        })
    })

    it('Get an error when trying to get child nodes by path with a wrong path', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodesByPath(paths: ["/testList/testSubList2", "/testList/wrongPath"]) {
                            name
                        }
                    }
                }`
        }).should(result => {
            validateError(result, `javax.jcr.PathNotFoundException: /testList/wrongPath`)
        })
    })

    it('Get an error when trying to get child nodes in live', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr (workspace: LIVE) {
                        nodesByPath(paths: ["/testList/testSubList2", "/testList/testSubList1"]) {
                            name
                        }
                    }
                }`
        }).should(result => {
            validateErrors(result, ["javax.jcr.PathNotFoundException: /testList/testSubList2","javax.jcr.PathNotFoundException: /testList/testSubList1"])
        })
    })

    it('Get node by id', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeById(uuid: ${subNodeUuid2}) {
                            name
                        }
                    }
                }`
        }).should(result => {
            const nodes = result?.data?.jcr?.nodeById?.name
            validateNode(node, "testSubList2")
        })
    })

    it('Get an error trying to get a node with wrong id', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeById(uuid: "badId") {
                            name
                        }
                    }
                }`
        }).should(result => {
            validateError(result, `javax.jcr.ItemNotFoundException: badId`)
        })
    })

    it('Get an error trying to get a node in live', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr (workspace: LIVE) {
                        nodeById(uuid: ${subNodeUuid2}) {
                            name
                        }
                    }
                }`
        }).should(result => {
            validateError(result, `javax.jcr.ItemNotFoundException: ${subNodeUuid2}`)
        })
    })

    it('Get child nodes by id', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodesById(uuids: [${subNodeUuid2}, ${subNodeUuid1}]) {
                            name
                        }
                    }
                }`
        }).should(result => {
            const nodes = result?.data?.jcr?.nodesByPath?.name
            expect(nodes).to.have.length(2)
            validateNode(nodes[0], "testSubList2")
            validateNode(nodes[1], "testSubList1")
        })
    })

    it('Get an error trying to get child nodes with wrong id', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodesById(uuids: [${subNodeUuid2}, "wrongId"]) {
                            name
                        }
                    }
                }`
        }).should(result => {
            validateError(result, `javax.jcr.ItemNotFoundException: wrongId`)
        })
    })

    it('Get an error trying to get child nodes with wrong id', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr (workspace: LIVE) {
                        nodesById(uuids: [${subNodeUuid2}, ${subNodeUuid1}]) {
                            name
                        }
                    }
                }`
        }).should(result => {
            validateErrors(result, [`javax.jcr.ItemNotFoundException: ${subNodeUuid2}`, `javax.jcr.ItemNotFoundException:  ${subNodeUuid1}`])
        })
    })

    after('Delete testList node', function () {
        cy.apollo({
            mutationFile: 'jcr/deleteNode.graphql',
            variables: {
                pathOrId: '/testList',
            }
        });
    })
})
