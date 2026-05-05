import {addNode, createSite, createUser, deleteSite, deleteUser, publishNode} from '@jahia/cypress';
import {grantUserRole} from '../../fixtures/acl';
import gql from 'graphql-tag';

/**
 * End-to-end tests for the Tag Manager GraphQL API.
 *
 * Coverage:
 *  - Read side: getTags (with sorting), getTaggedContent
 *  - Mutation happy paths: renameTag, deleteTag, renameTagOnNode, deleteTagOnNode
 *  - Dual-workspace propagation (EDIT + LIVE)
 *  - Authorization failures (user without tagManager permission, wrong site key)
 */
describe('Tag Manager GraphQL API', () => {
    const siteKey = 'tagManagerTestSite';
    const unauthorizedUser = 'tagManagerUnauthorized';
    const password = 'password';

    // UUIDs captured during setup to reuse across tests
    let nodeAUuid: string;
    let nodeBUuid: string;

    before('Create site, tagged nodes, and test users', () => {
        createSite(siteKey);
        createUser(unauthorizedUser, password);
        // Grant editor (but NOT tagManager) on the site to the unauthorized user
        grantUserRole(`/sites/${siteKey}`, 'editor', unauthorizedUser);

        // Create two content nodes tagged with 'alpha' and 'beta'
        addNode({
            parentPathOrId: `/sites/${siteKey}/contents`,
            name: 'taggedNodeA',
            primaryNodeType: 'jnt:contentList',
            mixins: ['jmix:tagged'],
            properties: [
                {name: 'j:tagList', values: ['alpha', 'beta'], type: 'STRING'}
            ]
        }).then((result: any) => {
            nodeAUuid = result.data.jcr.addNode.uuid;
        });

        addNode({
            parentPathOrId: `/sites/${siteKey}/contents`,
            name: 'taggedNodeB',
            primaryNodeType: 'jnt:contentList',
            mixins: ['jmix:tagged'],
            properties: [
                {name: 'j:tagList', values: ['alpha'], type: 'STRING'}
            ]
        }).then((result: any) => {
            nodeBUuid = result.data.jcr.addNode.uuid;
            // Publish both nodes so LIVE workspace is populated
            publishNode(`/sites/${siteKey}/contents/taggedNodeA`);
            publishNode(`/sites/${siteKey}/contents/taggedNodeB`);
        });
    });

    after('Remove test data', () => {
        deleteSite(siteKey);
        deleteUser(unauthorizedUser);
    });

    // ────────────────────────────────────────────────────────────────────────────
    // READ SIDE
    // ────────────────────────────────────────────────────────────────────────────

    describe('getTags', () => {
        it('returns all tags with occurrence counts as root (authorized)', () => {
            cy.apollo({
                queryFile: 'tagManager/getTags.graphql',
                variables: {siteKey}
            }).should((result: any) => {
                const tags = result.data.admin.tagManager.tags.nodes;
                expect(tags).to.be.an('array').with.length.greaterThan(0);

                const alpha = tags.find((t: any) => t.name === 'alpha');
                expect(alpha, 'alpha tag should exist').to.exist;
                expect(alpha.occurrences).to.equal(2);

                const beta = tags.find((t: any) => t.name === 'beta');
                expect(beta, 'beta tag should exist').to.exist;
                expect(beta.occurrences).to.equal(1);
            });
        });

        it('returns tags sorted by occurrences descending', () => {
            cy.apollo({
                queryFile: 'tagManager/getTags.graphql',
                variables: {siteKey, sortBy: 'OCCURRENCES', sortOrder: 'DESC'}
            }).should((result: any) => {
                const tags = result.data.admin.tagManager.tags.nodes;
                for (let i = 1; i < tags.length; i++) {
                    expect(tags[i - 1].occurrences).to.be.greaterThan(tags[i].occurrences - 1);
                }
            });
        });

        it('denies access for a user without tagManager permission', () => {
            cy.apolloClient({username: unauthorizedUser, password})
                .apollo({
                    queryFile: 'tagManager/getTags.graphql',
                    variables: {siteKey},
                    errorPolicy: 'all'
                }).should((result: any) => {
                    expect(result.errors, 'should contain a permission error').to.exist.and.not.be.empty;
                    expect(result.data?.admin?.tagManager).to.be.null;
                });
        });

        it('returns an error for an unknown site key', () => {
            cy.apollo({
                queryFile: 'tagManager/getTags.graphql',
                variables: {siteKey: 'nonExistentSite99'},
                errorPolicy: 'all'
            }).should((result: any) => {
                expect(result.errors, 'should surface a repository or permission error').to.exist.and.not.be.empty;
            });
        });
    });

    describe('getTaggedContent', () => {
        it('returns all nodes carrying a given tag', () => {
            cy.apollo({
                queryFile: 'tagManager/getTaggedContent.graphql',
                variables: {siteKey, tag: 'alpha'}
            }).should((result: any) => {
                const nodes = result.data.admin.tagManager.taggedContent.nodes;
                expect(nodes).to.have.length(2);
                const paths = nodes.map((n: any) => n.path);
                expect(paths).to.include(`/sites/${siteKey}/contents/taggedNodeA`);
                expect(paths).to.include(`/sites/${siteKey}/contents/taggedNodeB`);
            });
        });

        it('returns only the matching node for an exclusive tag', () => {
            cy.apollo({
                queryFile: 'tagManager/getTaggedContent.graphql',
                variables: {siteKey, tag: 'beta'}
            }).should((result: any) => {
                const nodes = result.data.admin.tagManager.taggedContent.nodes;
                expect(nodes).to.have.length(1);
                expect(nodes[0].path).to.equal(`/sites/${siteKey}/contents/taggedNodeA`);
            });
        });

        it('returns empty results for a tag that does not exist', () => {
            cy.apollo({
                queryFile: 'tagManager/getTaggedContent.graphql',
                variables: {siteKey, tag: 'nonExistentTag'}
            }).should((result: any) => {
                const nodes = result.data.admin.tagManager.taggedContent.nodes;
                expect(nodes).to.have.length(0);
            });
        });

        it('denies access for a user without tagManager permission', () => {
            cy.apolloClient({username: unauthorizedUser, password})
                .apollo({
                    queryFile: 'tagManager/getTaggedContent.graphql',
                    variables: {siteKey, tag: 'alpha'},
                    errorPolicy: 'all'
                }).should((result: any) => {
                    expect(result.errors).to.exist.and.not.be.empty;
                    expect(result.data?.admin?.tagManager).to.be.null;
                });
        });
    });

    // ────────────────────────────────────────────────────────────────────────────
    // MUTATIONS — BULK (site-wide)
    // ────────────────────────────────────────────────────────────────────────────

    describe('renameTag (bulk)', () => {
        it('renames the tag on all nodes in both workspaces', () => {
            cy.apollo({
                mutationFile: 'tagManager/renameTag.graphql',
                variables: {siteKey, tag: 'beta', newName: 'beta-renamed'}
            }).should((result: any) => {
                const {tag, nodeId, workspaceResults} = result.data.admin.tagManager.renameTag;
                expect(tag).to.equal('beta');
                expect(nodeId).to.be.null;
                expect(workspaceResults).to.have.length(2);

                for (const wsResult of workspaceResults) {
                    expect(wsResult.processedCount).to.equal(1);
                    expect(wsResult.failedCount).to.equal(0);
                    expect(wsResult.failedPaths).to.be.empty;
                }
            });

            // Verify the old tag is gone and the new one appears
            cy.apollo({
                queryFile: 'tagManager/getTags.graphql',
                variables: {siteKey}
            }).should((result: any) => {
                const names = result.data.admin.tagManager.tags.nodes.map((t: any) => t.name);
                expect(names).to.not.include('beta');
                expect(names).to.include('beta-renamed');
            });
        });

        it('returns an error when newName is blank', () => {
            cy.apollo({
                mutationFile: 'tagManager/renameTag.graphql',
                variables: {siteKey, tag: 'alpha', newName: '   '},
                errorPolicy: 'all'
            }).should((result: any) => {
                expect(result.errors).to.exist.and.not.be.empty;
            });
        });

        it('denies bulk rename for a user without tagManager permission', () => {
            cy.apolloClient({username: unauthorizedUser, password})
                .apollo({
                    mutationFile: 'tagManager/renameTag.graphql',
                    variables: {siteKey, tag: 'alpha', newName: 'alpha-new'},
                    errorPolicy: 'all'
                }).should((result: any) => {
                    expect(result.errors).to.exist.and.not.be.empty;
                    expect(result.data?.admin?.tagManager).to.be.null;
                });
        });
    });

    describe('deleteTag (bulk)', () => {
        // Use the 'beta-renamed' tag left by the preceding renameTag test
        it('removes the tag from all nodes in both workspaces', () => {
            cy.apollo({
                mutationFile: 'tagManager/deleteTag.graphql',
                variables: {siteKey, tag: 'beta-renamed'}
            }).should((result: any) => {
                const {tag, nodeId, workspaceResults} = result.data.admin.tagManager.deleteTag;
                expect(tag).to.equal('beta-renamed');
                expect(nodeId).to.be.null;
                expect(workspaceResults).to.have.length(2);

                for (const wsResult of workspaceResults) {
                    expect(wsResult.processedCount).to.equal(1);
                    expect(wsResult.failedCount).to.equal(0);
                    expect(wsResult.failedPaths).to.be.empty;
                }
            });

            // Verify the tag is gone from the read side
            cy.apollo({
                queryFile: 'tagManager/getTags.graphql',
                variables: {siteKey}
            }).should((result: any) => {
                const names = result.data.admin.tagManager.tags.nodes.map((t: any) => t.name);
                expect(names).to.not.include('beta-renamed');
            });
        });

        it('denies bulk delete for a user without tagManager permission', () => {
            cy.apolloClient({username: unauthorizedUser, password})
                .apollo({
                    mutationFile: 'tagManager/deleteTag.graphql',
                    variables: {siteKey, tag: 'alpha'},
                    errorPolicy: 'all'
                }).should((result: any) => {
                    expect(result.errors).to.exist.and.not.be.empty;
                    expect(result.data?.admin?.tagManager).to.be.null;
                });
        });
    });

    // ────────────────────────────────────────────────────────────────────────────
    // MUTATIONS — SINGLE NODE
    // ────────────────────────────────────────────────────────────────────────────

    describe('renameTagOnNode', () => {
        it('renames the tag on a single node in both workspaces', () => {
            cy.apollo({
                mutationFile: 'tagManager/renameTagOnNode.graphql',
                variables: {siteKey, tag: 'alpha', newName: 'alpha-node-renamed', nodeId: nodeAUuid}
            }).should((result: any) => {
                const {tag, nodeId, workspaceResults} = result.data.admin.tagManager.renameTagOnNode;
                expect(tag).to.equal('alpha');
                expect(nodeId).to.equal(nodeAUuid);
                expect(workspaceResults).to.have.length(2);

                for (const wsResult of workspaceResults) {
                    expect(wsResult.processedCount).to.equal(1);
                    expect(wsResult.failedCount).to.equal(0);
                    expect(wsResult.failedPaths).to.be.empty;
                }
            });

            // nodeB still has 'alpha'; nodeA should now have 'alpha-node-renamed'
            cy.apollo({
                queryFile: 'tagManager/getTaggedContent.graphql',
                variables: {siteKey, tag: 'alpha-node-renamed'}
            }).should((result: any) => {
                const nodes = result.data.admin.tagManager.taggedContent.nodes;
                expect(nodes).to.have.length(1);
                expect(nodes[0].path).to.equal(`/sites/${siteKey}/contents/taggedNodeA`);
            });
        });

        it('returns processedCount 0 when the tag is absent on the node', () => {
            // nodeA no longer has 'alpha' after the rename above
            cy.apollo({
                mutationFile: 'tagManager/renameTagOnNode.graphql',
                variables: {siteKey, tag: 'alpha', newName: 'alpha-again', nodeId: nodeAUuid}
            }).should((result: any) => {
                const workspaceResults = result.data.admin.tagManager.renameTagOnNode.workspaceResults;
                for (const wsResult of workspaceResults) {
                    expect(wsResult.processedCount).to.equal(0);
                    expect(wsResult.failedCount).to.equal(0);
                }
            });
        });

        it('rejects renameTagOnNode when node belongs to a different site', () => {
            // Use the UUID of nodeA against a fabricated different siteKey
            cy.apollo({
                mutationFile: 'tagManager/renameTagOnNode.graphql',
                variables: {siteKey: 'systemsite', tag: 'alpha', newName: 'alpha-x', nodeId: nodeAUuid},
                errorPolicy: 'all'
            }).should((result: any) => {
                expect(result.errors).to.exist.and.not.be.empty;
            });
        });

        it('denies renameTagOnNode for a user without tagManager permission', () => {
            cy.apolloClient({username: unauthorizedUser, password})
                .apollo({
                    mutationFile: 'tagManager/renameTagOnNode.graphql',
                    variables: {siteKey, tag: 'alpha', newName: 'alpha-x', nodeId: nodeAUuid},
                    errorPolicy: 'all'
                }).should((result: any) => {
                    expect(result.errors).to.exist.and.not.be.empty;
                    expect(result.data?.admin?.tagManager).to.be.null;
                });
        });
    });

    describe('deleteTagOnNode', () => {
        it('removes a tag from a single node in both workspaces', () => {
            // nodeBUuid has 'alpha'; remove it
            cy.apollo({
                mutationFile: 'tagManager/deleteTagOnNode.graphql',
                variables: {siteKey, tag: 'alpha', nodeId: nodeBUuid}
            }).should((result: any) => {
                const {tag, nodeId, workspaceResults} = result.data.admin.tagManager.deleteTagOnNode;
                expect(tag).to.equal('alpha');
                expect(nodeId).to.equal(nodeBUuid);
                expect(workspaceResults).to.have.length(2);

                for (const wsResult of workspaceResults) {
                    expect(wsResult.processedCount).to.equal(1);
                    expect(wsResult.failedCount).to.equal(0);
                    expect(wsResult.failedPaths).to.be.empty;
                }
            });

            // Verify nodeB no longer appears in tagged content for 'alpha'
            cy.apollo({
                queryFile: 'tagManager/getTaggedContent.graphql',
                variables: {siteKey, tag: 'alpha'}
            }).should((result: any) => {
                const uuids = result.data.admin.tagManager.taggedContent.nodes.map((n: any) => n.uuid);
                expect(uuids).to.not.include(nodeBUuid);
            });
        });

        it('rejects deleteTagOnNode when node belongs to a different site', () => {
            cy.apollo({
                mutationFile: 'tagManager/deleteTagOnNode.graphql',
                variables: {siteKey: 'systemsite', tag: 'alpha', nodeId: nodeBUuid},
                errorPolicy: 'all'
            }).should((result: any) => {
                expect(result.errors).to.exist.and.not.be.empty;
            });
        });

        it('denies deleteTagOnNode for a user without tagManager permission', () => {
            cy.apolloClient({username: unauthorizedUser, password})
                .apollo({
                    mutationFile: 'tagManager/deleteTagOnNode.graphql',
                    variables: {siteKey, tag: 'alpha', nodeId: nodeBUuid},
                    errorPolicy: 'all'
                }).should((result: any) => {
                    expect(result.errors).to.exist.and.not.be.empty;
                    expect(result.data?.admin?.tagManager).to.be.null;
                });
        });
    });
});
