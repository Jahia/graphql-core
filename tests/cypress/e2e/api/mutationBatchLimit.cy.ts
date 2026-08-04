import gql from 'graphql-tag';

/*
 * Mutation batch bound (graphql.mutation.batch.limit): how many nodes one request may ask a mutation to operate on.
 *
 * The list is supplied through a GraphQL variable throughout, because that is how a client of this size normally sends
 * one and the bound has to hold for it just the same.
 *
 * The spec uses the shipped default (5000) rather than reconfiguring anything, so it also asserts that the default is
 * in force. The bound is applied before any path is resolved, so one existing path repeated to the required length is
 * enough - no fixture content is needed.
 */
describe('GraphQL mutation batch bound', () => {
    const existingPath = '/sites/systemsite';
    const shippedLimit = 5000;

    const overSizedPaths = Array.from({length: shippedLimit + 1}, () => existingPath);
    const overSizedNodes = Array.from({length: shippedLimit + 1}, (_, index) => ({
        parentPathOrId: existingPath,
        name: `batched${index}`,
        primaryNodeType: 'jnt:contentList'
    }));

    const mutateNodesQuery = gql`
        mutation ($paths: [String]!) {
            jcr {
                mutateNodes(pathsOrIds: $paths) {
                    uuid
                }
            }
        }
    `;

    const addNodesBatchQuery = gql`
        mutation ($nodes: [InputJCRNodeWithParent]!) {
            jcr {
                addNodesBatch(nodes: $nodes) {
                    uuid
                }
            }
        }
    `;

    const expectRejectedForSize = (response: any, batchSize: number) => {
        const messages = (response?.errors ?? []).map((error: any) => error.message);
        expect(messages.join(' | ')).to.contain(
            `maximum mutation batch size exceeded ${batchSize} > ${shippedLimit}`
        );
        expect(response?.data?.jcr?.mutateNodes ?? null).to.be.null;
        expect(response?.data?.jcr?.addNodesBatch ?? null).to.be.null;
    };

    it('refuses a mutateNodes batch over the bound when the list arrives through a variable', () => {
        cy.apollo({
            mutation: mutateNodesQuery,
            variables: {paths: overSizedPaths},
            errorPolicy: 'all'
        }).then((response: any) => {
            expectRejectedForSize(response, overSizedPaths.length);
        });
    });

    it('refuses an addNodesBatch batch over the bound when the list arrives through a variable', () => {
        cy.apollo({
            mutation: addNodesBatchQuery,
            variables: {nodes: overSizedNodes},
            errorPolicy: 'all'
        }).then((response: any) => {
            expectRejectedForSize(response, overSizedNodes.length);
            // Refused on size alone, so nothing may have been created along the way.
            cy.apollo({
                query: gql`
                    query {
                        jcr {
                            nodeByPath(path: "${existingPath}") {
                                children(names: ["batched0"]) {
                                    nodes {
                                        name
                                    }
                                }
                            }
                        }
                    }
                `
            }).then((check: any) => {
                expect(check.data.jcr.nodeByPath.children.nodes).to.have.length(0);
            });
        });
    });

    it('refuses aliased batches that are individually within the bound but exceed it together', () => {
        // Each alias asks for two thirds of the bound, so a per-call check would admit all three and the request would
        // operate on twice the bound. The guard measures the whole document, so aliasing cannot multiply it.
        const perAlias = Math.ceil((shippedLimit * 2) / 3);
        const aliasedMutation = gql`
            mutation ($paths: [String]!) {
                jcr {
                    a: mutateNodes(pathsOrIds: $paths) { uuid }
                    b: mutateNodes(pathsOrIds: $paths) { uuid }
                    c: mutateNodes(pathsOrIds: $paths) { uuid }
                }
            }
        `;

        cy.apollo({
            mutation: aliasedMutation,
            variables: {paths: Array.from({length: perAlias}, () => existingPath)},
            errorPolicy: 'all'
        }).then((response: any) => {
            expectRejectedForSize(response, perAlias * 3);
            expect(response?.data?.jcr?.a ?? null).to.be.null;
        });
    });

    it('refuses items buried inside a recursive input type', () => {
        // InputJCRNodeWithParent.children is a list of nodes that can itself nest, so the outer list length says
        // nothing about how many nodes the request actually creates. Three outer entries here describe 6003.
        const childrenPerNode = Math.ceil((shippedLimit * 2) / 3);
        const nested = Array.from({length: 3}, (_, i) => ({
            parentPathOrId: existingPath,
            name: `nested${i}`,
            primaryNodeType: 'jnt:contentList',
            children: Array.from({length: childrenPerNode}, (_child, j) => ({
                name: `nestedChild${i}_${j}`,
                primaryNodeType: 'jnt:contentList'
            }))
        }));

        cy.apollo({
            mutation: addNodesBatchQuery,
            variables: {nodes: nested},
            errorPolicy: 'all'
        }).then((response: any) => {
            expectRejectedForSize(response, 3 + (3 * childrenPerNode));
        });
    });

    it('still accepts a batch within the bound through the same variable path', () => {
        cy.apollo({
            mutation: mutateNodesQuery,
            variables: {paths: [existingPath]},
            errorPolicy: 'all'
        }).then((response: any) => {
            expect(response?.errors ?? []).to.have.length(0);
            expect(response.data.jcr.mutateNodes).to.have.length(1);
        });
    });
});
