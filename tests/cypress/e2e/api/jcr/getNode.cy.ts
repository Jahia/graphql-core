
import gql from 'graphql-tag';
import {validateErrors} from './validateErrors';
import {validateError} from './validateErrors';
import {validateNode} from './validateNode';

describe('Get node graphql test', () => {
    let nodeUuid: string;
    const nodeTitleFr = 'text FR';
    const nodeTitleEn = 'text EN';
    let subNodeUuid1: string;
    let subNodeUuid2: string;

    before('load graphql file and create nodes', () => {
        cy.apollo({
            mutationFile: 'jcr/deleteNode.graphql',
            variables: {
                pathOrId: '/testList'
            }
        });
        cy.apollo({
            mutationFile: 'jcr/addNode.graphql',
            variables: {
                parentPathOrId: '/',
                nodeName: 'testList',
                nodeType: 'jnt:contentList',
                properties: [
                    {name: 'jcr:title', value: nodeTitleEn, language: 'en'},
                    {name: 'jcr:title', value: nodeTitleFr, language: 'fr'}
                ],
                children: [
                    {name: 'testSubList1', primaryNodeType: 'jnt:contentList'},
                    {name: 'testSubList2', primaryNodeType: 'jnt:contentList'}
                ]
            }
        }).then((response: any) => {
            nodeUuid = response.data.jcr.addNode.uuid;
            subNodeUuid1 = response.data.jcr.addNode.addChildrenBatch[0].uuid;
            subNodeUuid2 = response.data.jcr.addNode.addChildrenBatch[1].uuid;
        });
        // Create sub nodes
        cy.apollo({
            mutationFile: 'jcr/addNode.graphql',
            variables: {
                parentPathOrId: '/testList/testSubList1',
                nodeName: 'testSubList1-1',
                nodeType: 'jnt:contentList',
                children: [
                    {name: 'testSubList1-1-1', primaryNodeType: 'jnt:contentList'},
                    {name: 'testSubList1-1-2', primaryNodeType: 'jnt:contentList'}
                ]
            }
        });
    });

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
                            titleen: property(name: "jcr:title", language: "en") {
                                value
                            }
                            titlefr: property(name: "jcr:title", language: "fr") {
                                value
                            }
                        }
                    }
                }
            `
        }).should((response: any) => {
            expect(response.data.jcr.nodeByPath).to.exist;
            expect(response.data.jcr.nodeByPath.name).to.equal('testList');
            expect(response.data.jcr.nodeByPath.path).to.equal('/testList');
            expect(response.data.jcr.nodeByPath.uuid).to.equal(nodeUuid);
            expect(response.data.jcr.nodeByPath.displayName).to.equal('testList');
            expect(response.data.jcr.nodeByPath.titlefr.value).to.equal(nodeTitleFr);
            expect(response.data.jcr.nodeByPath.titleen.value).to.equal(nodeTitleEn);
        });
    });

    it('Get child node by path', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeByPath(path: "/testList/testSubList2") {
                            name
                        }
                    }
                }
            `
        }).should((response: any) => {
            expect(response.data.jcr.nodeByPath).to.exist;
            expect(response.data.jcr.nodeByPath.name).to.equal('testSubList2');
        });
    });

    it('Get an error when trying to get child node with wrong path', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeByPath(path: "/testList/wrongPath") {
                            name
                        }
                    }
                }
            `,
            errorPolicy: 'all'
        }).should(result => {
            validateError(result, 'javax.jcr.PathNotFoundException: /testList/wrongPath');
        });
    });

    it('Get an error when trying to get child node in live', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr(workspace: LIVE) {
                        nodeByPath(path: "/testList/testSubList2") {
                            name
                        }
                    }
                }
            `,
            errorPolicy: 'all'
        }).should(result => {
            validateError(result, 'javax.jcr.PathNotFoundException: /testList/testSubList2');
        });
    });

    it('Get child nodes by path', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodesByPath(paths: ["/testList/testSubList2", "/testList/testSubList1"]) {
                            name
                        }
                    }
                }
            `
        }).should(result => {
            const nodes = result?.data?.jcr?.nodesByPath;
            expect(nodes).to.have.length(2);
            validateNode(nodes[0], 'testSubList2');
            validateNode(nodes[1], 'testSubList1');
        });
    });

    it('Get an error when trying to get child nodes by path with a wrong path', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodesByPath(paths: ["/testList/testSubList2", "/testList/wrongPath"]) {
                            name
                        }
                    }
                }
            `,
            errorPolicy: 'all'
        }).should(result => {
            validateError(result, 'javax.jcr.PathNotFoundException: /testList/wrongPath');
        });
    });

    it('Get an error when trying to get child nodes in live', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr(workspace: LIVE) {
                        nodesByPath(paths: ["/testList/testSubList2", "/testList/testSubList1"]) {
                            name
                        }
                    }
                }
            `,
            errorPolicy: 'all'
        }).should(result => {
            validateErrors(result, [
                'javax.jcr.PathNotFoundException: /testList/testSubList2',
                'javax.jcr.PathNotFoundException: /testList/testSubList1'
            ]);
        });
    });

    it('Get node by id', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeById(uuid: "${subNodeUuid2}") {
                            name
                        }
                    }
                }`
        }).should(result => {
            const node = result?.data?.jcr?.nodeById;
            validateNode(node, 'testSubList2');
        });
    });

    it('Get an error trying to get a node with wrong id', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeById(uuid: "badId") {
                            name
                        }
                    }
                }
            `,
            errorPolicy: 'all'
        }).should(result => {
            validateError(result, 'javax.jcr.ItemNotFoundException: badId');
        });
    });

    it('Get an error trying to get a node in live', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr (workspace: LIVE) {
                        nodeById(uuid: "${subNodeUuid2}") {
                            name
                        }
                    }
                }`,
            errorPolicy: 'all'
        }).should(result => {
            validateError(result, `javax.jcr.ItemNotFoundException: ${subNodeUuid2}`);
        });
    });

    it('Get child nodes by id', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodesById(uuids: ["${subNodeUuid2}", "${subNodeUuid1}"]) {
                            name
                        }
                    }
                }`
        }).should(result => {
            const nodes = result?.data?.jcr?.nodesById;
            expect(nodes).to.have.length(2);
            validateNode(nodes[0], 'testSubList2');
            validateNode(nodes[1], 'testSubList1');
        });
    });

    it('Get an error trying to get child nodes with wrong id', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodesById(uuids: ["${subNodeUuid2}", "wrongId"]) {
                            name
                        }
                    }
                }`,
            errorPolicy: 'all'
        }).should(result => {
            validateError(result, 'javax.jcr.ItemNotFoundException: wrongId');
        });
    });

    it('Get an error trying to get child nodes in live', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr (workspace: LIVE) {
                        nodesById(uuids: ["${subNodeUuid2}", "${subNodeUuid1}"]) {
                            name
                        }
                    }
                }`,
            errorPolicy: 'all'
        }).should(result => {
            validateErrors(result, [
                `javax.jcr.ItemNotFoundException: ${subNodeUuid2}`,
                `javax.jcr.ItemNotFoundException: ${subNodeUuid1}`
            ]);
        });
    });

    it('Get descendant nodes with a given maxDepth', () => {
        cy.apollo({
            query: gql`
                {
                  jcr {
                    result: nodeByPath(path: "/testList") {
                      descendants(maxDepth: 2) {
                        nodes {
                          name
                        }
                      }
                    }
                  }
                }`,
            errorPolicy: 'all'
        }).should(result => {
            const nodes = result?.data?.jcr?.result?.descendants?.nodes;
            expect(nodes).to.have.length(3);
            expect(nodes[0].name).to.equal('testSubList1');
            expect(nodes[1].name).to.equal('testSubList1-1');
            expect(nodes[2].name).to.equal('testSubList2');
        });
    });

    it('Get ALL descendant nodes with a 0 or negative maxDepth', () => {
        [0, -1].forEach(depth => cy.apollo({
            query: gql`
                {
                  jcr {
                    result: nodeByPath(path: "/testList") {
                      descendants(maxDepth: ${depth}) {
                        nodes {
                          name
                        }
                      }
                    }
                  }
                }`,
            errorPolicy: 'all'
        }).should(result => {
            const nodes = result?.data?.jcr?.result?.descendants?.nodes;
            expect(nodes).to.have.length(5);
            expect(nodes[0].name).to.equal('testSubList1');
            expect(nodes[1].name).to.equal('testSubList1-1');
            expect(nodes[2].name).to.equal('testSubList1-1-1');
            expect(nodes[3].name).to.equal('testSubList1-1-2');
            expect(nodes[4].name).to.equal('testSubList2');
        }));
    });

    it.only('Get node depth', () => {
        const results = [
            {depth: 0, path: '/'},
            {depth: 1, path: '/testList'},
            {depth: 2, path: '/testList/testSubList1'},
            {depth: 3, path: '/testList/testSubList1/testSubList1-1'},
            {depth: 4, path: '/testList/testSubList1/testSubList1-1/testSubList1-1-1'}
        ];
        results.forEach(result => cy.apollo({
            query: gql`
                {
                  jcr {
                    result: nodeByPath(path: "${result.path}") {
                      depth
                    }
                  }
                }`,
            errorPolicy: 'all'
        }).should(response => {
            expect(response?.data?.jcr?.result?.depth).to.equal(result.depth);
        }));
    });

    after('Delete testList node', function () {
        cy.apollo({
            mutationFile: 'jcr/deleteNode.graphql',
            variables: {
                pathOrId: '/testList'
            }
        });
    });
});

