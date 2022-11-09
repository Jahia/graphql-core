/* eslint-disable @typescript-eslint/no-explicit-any */

import gql from 'graphql-tag'

describe('Test field grouping functionality', () => {
    before('load graphql file and create node', () => {
        cy.apollo({
            mutationFile: 'jcr/addNode.graphql',
            variables: {
                parentPathOrId: '/',
                nodeName: 'groupingRoot',
                nodeType: 'jnt:contentFolder',
                children: [
                    { name: 'nodeGroup1-0', primaryNodeType: 'jnt:text' },
                    { name: 'nodeGroup2-1', primaryNodeType: 'jnt:contentList' },
                    { name: 'noGroup1-0', primaryNodeType: 'jnt:bigText' },
                    { name: 'nodeGroup2-0', primaryNodeType: 'jnt:contentList' },
                    { name: 'noGroup1-1', primaryNodeType: 'jnt:bigText' },
                    { name: 'nodeGroup1-1', primaryNodeType: 'jnt:text' },
                ],
            },
        })
    })

    it('should group nodes with grouping type start without sort', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeByPath(path: "/groupingRoot") {
                            result: descendants(
                                fieldGrouping: {
                                    fieldName: "type.value"
                                    groups: ["jnt:text", "jnt:fakeName", "jnt:contentList"]
                                    groupingType: START
                                }
                            ) {
                                nodes {
                                    type: property(name: "jcr:primaryType") {
                                        value
                                    }
                                    name
                                }
                            }
                        }
                    }
                }
            `,
        }).should((response: any) => {
            const nodes = response?.data?.jcr?.nodeByPath?.result?.nodes
            expect(nodes.length).to.equals(6)
            expect(nodes[0].name).to.equals('nodeGroup1-0')
            expect(nodes[1].name).to.equals('nodeGroup1-1')
            expect(nodes[2].name).to.equals('nodeGroup2-1')
            expect(nodes[3].name).to.equals('nodeGroup2-0')
            expect(nodes[4].name).to.equals('noGroup1-0')
            expect(nodes[5].name).to.equals('noGroup1-1')
        })
    })

    it('should group nodes with grouping type end without sort', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeByPath(path: "/groupingRoot") {
                            result: descendants(
                                fieldGrouping: {
                                    fieldName: "type.value"
                                    groups: ["jnt:fakeName", "jnt:text", "jnt:contentList"]
                                    groupingType: END
                                }
                            ) {
                                nodes {
                                    type: property(name: "jcr:primaryType") {
                                        value
                                    }
                                    name
                                }
                            }
                        }
                    }
                }
            `,
        }).should((response: any) => {
            const nodes = response?.data?.jcr?.nodeByPath?.result?.nodes
            expect(nodes.length).to.equals(6)
            expect(nodes[0].name).to.equals('noGroup1-0')
            expect(nodes[1].name).to.equals('noGroup1-1')
            expect(nodes[2].name).to.equals('nodeGroup1-0')
            expect(nodes[3].name).to.equals('nodeGroup1-1')
            expect(nodes[4].name).to.equals('nodeGroup2-1')
            expect(nodes[5].name).to.equals('nodeGroup2-0')
        })
    })

    it('should group nodes with grouping type start with sort', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeByPath(path: "/groupingRoot") {
                            result: descendants(
                                fieldGrouping: {
                                    fieldName: "type.value"
                                    groups: ["jnt:contentList", "jnt:text", "jnt:fakeName"]
                                    groupingType: START
                                }
                                fieldSorter: { sortType: ASC, fieldName: "name" }
                            ) {
                                nodes {
                                    type: property(name: "jcr:primaryType") {
                                        value
                                    }
                                    name
                                }
                            }
                        }
                    }
                }
            `,
        }).should((response: any) => {
            const nodes = response?.data?.jcr?.nodeByPath?.result?.nodes
            expect(nodes.length).to.equals(6)
            expect(nodes[0].name).to.equals('nodeGroup2-0')
            expect(nodes[1].name).to.equals('nodeGroup2-1')
            expect(nodes[2].name).to.equals('nodeGroup1-0')
            expect(nodes[3].name).to.equals('nodeGroup1-1')
            expect(nodes[4].name).to.equals('noGroup1-0')
            expect(nodes[5].name).to.equals('noGroup1-1')
        })
    })

    after('Delete testPage node', function () {
        cy.apollo({
            mutationFile: 'jcr/deleteNode.graphql',
            variables: {
                pathOrId: '/groupingRoot',
            },
        })
    })
})
