import {addNode, createSite, createUser, deleteSite, deleteUser, publishAndWaitJobEnding} from '@jahia/cypress';
import {grantUserRole, revokeUserRole} from '../../fixtures/acl';

/**
 * End-to-end tests for the Tag Manager GraphQL API.
 *
 * Coverage:
 *  - Read side: getTags (with sorting), getTaggedContent
 *  - Mutation happy paths: renameTag, deleteTag, renameTagOnNode, deleteTagOnNode
 *  - Dual-workspace propagation (EDIT + LIVE)
 *  - Authorization failures (user without tagManager permission, wrong site key)
 *  - Per-node write rights: a caller holding tagManager still only writes the nodes it may write
 *  - Candidate selection: a tag carrying a quote is matched by the same rule as on the read side
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
            publishAndWaitJobEnding(`/sites/${siteKey}/contents/taggedNodeA`);
            publishAndWaitJobEnding(`/sites/${siteKey}/contents/taggedNodeB`);
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
                const tags = result.data.admin.jahia.tagManager.tags.nodes;
                expect(tags).to.be.an('array').with.length.greaterThan(0);

                // eslint-disable-next-line max-nested-callbacks
                const alpha = tags.find((t: any) => t.name === 'alpha');
                expect(alpha, 'alpha tag should exist').to.exist;
                expect(alpha.occurrences).to.equal(2);

                // eslint-disable-next-line max-nested-callbacks
                const beta = tags.find((t: any) => t.name === 'beta');
                expect(beta, 'beta tag should exist').to.exist;
                expect(beta.occurrences).to.equal(1);
            });
        });

        it('returns tags sorted by occurrences descending', () => {
            cy.apollo({
                queryFile: 'tagManager/getTags.graphql',
                variables: {siteKey, fieldSorter: {fieldName: 'occurrences', sortType: 'DESC'}}
            }).should((result: any) => {
                const tags = result.data.admin.jahia.tagManager.tags.nodes;
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
                    expect(result.data?.admin?.jahia?.tagManager).to.not.exist;
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
                const nodes = result.data.admin.jahia.tagManager.taggedContent.nodes;
                expect(nodes).to.have.length(2);
                // eslint-disable-next-line max-nested-callbacks
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
                const nodes = result.data.admin.jahia.tagManager.taggedContent.nodes;
                expect(nodes).to.have.length(1);
                expect(nodes[0].path).to.equal(`/sites/${siteKey}/contents/taggedNodeA`);
            });
        });

        it('returns empty results for a tag that does not exist', () => {
            cy.apollo({
                queryFile: 'tagManager/getTaggedContent.graphql',
                variables: {siteKey, tag: 'nonExistentTag'}
            }).should((result: any) => {
                const nodes = result.data.admin.jahia.tagManager.taggedContent.nodes;
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
                    expect(result.data?.admin?.jahia?.tagManager).to.not.exist;
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
                const {tag, nodeId, workspaceResults} = result.data.admin.jahia.tagManager.renameTag;
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
                // eslint-disable-next-line max-nested-callbacks
                const names = result.data.admin.jahia.tagManager.tags.nodes.map((t: any) => t.name);
                expect(names).to.not.include('beta');
                expect(names).to.include('beta-renamed');
            });
        });

        // The candidate nodes are selected by binding the tag as a query parameter, the same way the
        // read side does, so a tag carrying a quote is matched by one rule on both sides.
        it('renames a tag containing a quote on all nodes that carry it', () => {
            const quotedTag = 'o\'brien';

            addNode({
                parentPathOrId: `/sites/${siteKey}/contents`,
                name: 'quotedTagNode',
                primaryNodeType: 'jnt:contentList',
                mixins: ['jmix:tagged'],
                properties: [
                    {name: 'j:tagList', values: [quotedTag], type: 'STRING'}
                ]
            });

            cy.apollo({
                mutationFile: 'tagManager/renameTag.graphql',
                variables: {siteKey, tag: quotedTag, newName: 'obrien-renamed'}
            }).should((result: any) => {
                const workspaceResults = result.data.admin.jahia.tagManager.renameTag.workspaceResults;
                // eslint-disable-next-line max-nested-callbacks
                const editResult = workspaceResults.find((wsResult: any) => wsResult.workspace === 'default');
                expect(editResult.processedCount, 'the quoted tag selects its node').to.equal(1);
                expect(editResult.failedCount).to.equal(0);
            });

            cy.apollo({
                queryFile: 'tagManager/getTaggedContent.graphql',
                variables: {siteKey, tag: 'obrien-renamed'}
            }).should((result: any) => {
                // eslint-disable-next-line max-nested-callbacks
                const paths = result.data.admin.jahia.tagManager.taggedContent.nodes.map((node: any) => node.path);
                expect(paths).to.include(`/sites/${siteKey}/contents/quotedTagNode`);
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

        // The stored value is one tag per segment of the separator, each normalized on its own, so a name
        // that is non-blank as a whole can still store an empty tag — or no tag at all, which would drop
        // the renamed tag with nothing in its place.
        for (const newName of [' , ', ',']) {
            it(`returns an error when newName yields no usable tag (${JSON.stringify(newName)})`, () => {
                cy.apollo({
                    mutationFile: 'tagManager/renameTag.graphql',
                    variables: {siteKey, tag: 'alpha', newName},
                    errorPolicy: 'all'
                }).should((result: any) => {
                    expect(result.errors, 'the mutation is refused').to.exist.and.not.be.empty;
                });

                // As root: 'alpha' is still there, so nothing was renamed away
                cy.apollo({
                    queryFile: 'tagManager/getTags.graphql',
                    variables: {siteKey}
                }).should((result: any) => {
                    // eslint-disable-next-line max-nested-callbacks
                    const names = result.data.admin.jahia.tagManager.tags.nodes.map((t: any) => t.name);
                    expect(names).to.include('alpha');
                });
            });
        }

        it('denies bulk rename for a user without tagManager permission', () => {
            cy.apolloClient({username: unauthorizedUser, password})
                .apollo({
                    mutationFile: 'tagManager/renameTag.graphql',
                    variables: {siteKey, tag: 'alpha', newName: 'alpha-new'},
                    errorPolicy: 'all'
                }).should((result: any) => {
                    expect(result.errors).to.exist.and.not.be.empty;
                    expect(result.data?.admin?.jahia?.tagManager).to.not.exist;
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
                const {tag, nodeId, workspaceResults} = result.data.admin.jahia.tagManager.deleteTag;
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
                // eslint-disable-next-line max-nested-callbacks
                const names = result.data.admin.jahia.tagManager.tags.nodes.map((t: any) => t.name);
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
                    expect(result.data?.admin?.jahia?.tagManager).to.not.exist;
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
                const {tag, nodeId, workspaceResults} = result.data.admin.jahia.tagManager.renameTagOnNode;
                expect(tag).to.equal('alpha');
                expect(nodeId).to.equal(nodeAUuid);
                expect(workspaceResults).to.have.length(2);

                for (const wsResult of workspaceResults) {
                    expect(wsResult.processedCount).to.equal(1);
                    expect(wsResult.failedCount).to.equal(0);
                    expect(wsResult.failedPaths).to.be.empty;
                }
            });

            // NodeB still has 'alpha'; nodeA should now have 'alpha-node-renamed'
            cy.apollo({
                queryFile: 'tagManager/getTaggedContent.graphql',
                variables: {siteKey, tag: 'alpha-node-renamed'}
            }).should((result: any) => {
                const nodes = result.data.admin.jahia.tagManager.taggedContent.nodes;
                expect(nodes).to.have.length(1);
                expect(nodes[0].path).to.equal(`/sites/${siteKey}/contents/taggedNodeA`);
            });
        });

        it('completes without error when the tag is absent on the node', () => {
            // NodeA no longer has 'alpha' after the rename above
            cy.apollo({
                mutationFile: 'tagManager/renameTagOnNode.graphql',
                variables: {siteKey, tag: 'alpha', newName: 'alpha-again', nodeId: nodeAUuid}
            }).should((result: any) => {
                const workspaceResults = result.data.admin.jahia.tagManager.renameTagOnNode.workspaceResults;
                for (const wsResult of workspaceResults) {
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
                    expect(result.data?.admin?.jahia?.tagManager).to.not.exist;
                });
        });
    });

    describe('deleteTagOnNode', () => {
        it('removes a tag from a single node in both workspaces', () => {
            // NodeBUuid has 'alpha'; remove it
            cy.apollo({
                mutationFile: 'tagManager/deleteTagOnNode.graphql',
                variables: {siteKey, tag: 'alpha', nodeId: nodeBUuid}
            }).should((result: any) => {
                const {tag, nodeId, workspaceResults} = result.data.admin.jahia.tagManager.deleteTagOnNode;
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
                // eslint-disable-next-line max-nested-callbacks
                const uuids = result.data.admin.jahia.tagManager.taggedContent.nodes.map((n: any) => n.uuid);
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
                    expect(result.data?.admin?.jahia?.tagManager).to.not.exist;
                });
        });
    });

    // ────────────────────────────────────────────────────────────────────────────
    // PER-NODE WRITE RIGHTS
    //
    // Holding tagManager on the site opens the Tag Manager; it does not make every node
    // under the site writable. A node the caller's roles are denied on is out of reach and
    // must keep its tags, in BOTH workspaces — the live copy is updated by the same
    // operation, so the edit workspace decides for both.
    // ────────────────────────────────────────────────────────────────────────────

    describe('per-node write rights', () => {
        const tagManagerUser = 'tagManagerScoped';
        const restrictedNodePath = `/sites/${siteKey}/contents/restrictedNode`;
        let restrictedNodeUuid: string;

        before('Create a tag-manager user, a reachable node and an out-of-reach node', () => {
            createUser(tagManagerUser, password);
            // The server-administrator role carries the admin GraphQL entry permissions
            // (graphqlAdminQuery / graphqlAdminMutation); site-administrator carries tagManager
            // plus jcr:all_default on the site, which a per-node DENY takes back
            grantUserRole('/', 'server-administrator', tagManagerUser);
            grantUserRole(`/sites/${siteKey}`, 'site-administrator', tagManagerUser);

            addNode({
                parentPathOrId: `/sites/${siteKey}/contents`,
                name: 'reachableNode',
                primaryNodeType: 'jnt:contentList',
                mixins: ['jmix:tagged'],
                properties: [
                    {name: 'j:tagList', values: ['scoped'], type: 'STRING'}
                ]
            });

            addNode({
                parentPathOrId: `/sites/${siteKey}/contents`,
                name: 'restrictedNode',
                primaryNodeType: 'jnt:contentList',
                mixins: ['jmix:tagged'],
                properties: [
                    {name: 'j:tagList', values: ['restricted'], type: 'STRING'}
                ]
            }).then((result: any) => {
                restrictedNodeUuid = result.data.jcr.addNode.uuid;
                // Publish first, so the tag exists in both workspaces...
                publishAndWaitJobEnding(restrictedNodePath);
                // ...then DENY both roles on this node only, putting it out of the user's reach
                // while tagManager on the site itself is untouched
                revokeUserRole(restrictedNodePath, 'site-administrator', tagManagerUser);
                revokeUserRole(restrictedNodePath, 'server-administrator', tagManagerUser);
            });
        });

        after('Remove the test user', () => {
            deleteUser(tagManagerUser);
        });

        it('renames a tag the caller may write (site-wide)', () => {
            cy.apolloClient({username: tagManagerUser, password})
                .apollo({
                    mutationFile: 'tagManager/renameTag.graphql',
                    variables: {siteKey, tag: 'scoped', newName: 'scoped-renamed'}
                }).should((result: any) => {
                    const workspaceResults = result.data.admin.jahia.tagManager.renameTag.workspaceResults;
                    // eslint-disable-next-line max-nested-callbacks
                    const editResult = workspaceResults.find((wsResult: any) => wsResult.workspace === 'default');
                    expect(editResult.processedCount).to.equal(1);
                    expect(editResult.failedCount).to.equal(0);
                });

            cy.apollo({
                queryFile: 'tagManager/getTaggedContent.graphql',
                variables: {siteKey, tag: 'scoped-renamed'}
            }).should((result: any) => {
                const nodes = result.data.admin.jahia.tagManager.taggedContent.nodes;
                expect(nodes).to.have.length(1);
                expect(nodes[0].path).to.equal(`/sites/${siteKey}/contents/reachableNode`);
            });
        });

        it('leaves a node the caller may not write untouched on a site-wide rename', () => {
            cy.apolloClient({username: tagManagerUser, password})
                .apollo({
                    mutationFile: 'tagManager/renameTag.graphql',
                    variables: {siteKey, tag: 'restricted', newName: 'restricted-renamed'}
                }).should((result: any) => {
                    const workspaceResults = result.data.admin.jahia.tagManager.renameTag.workspaceResults;
                    // eslint-disable-next-line max-nested-callbacks
                    const processed = workspaceResults.reduce((total: number, wsResult: any) => total + wsResult.processedCount, 0);
                    // eslint-disable-next-line max-nested-callbacks
                    const failed = workspaceResults.reduce((total: number, wsResult: any) => total + wsResult.failedCount, 0);
                    expect(processed, 'no node is written').to.equal(0);
                    expect(failed, 'the out-of-reach node is reported as a failure').to.be.greaterThan(0);
                });

            // As root: the tag is still on the node, under its original name
            cy.apollo({
                queryFile: 'tagManager/getTaggedContent.graphql',
                variables: {siteKey, tag: 'restricted'}
            }).should((result: any) => {
                // eslint-disable-next-line max-nested-callbacks
                const paths = result.data.admin.jahia.tagManager.taggedContent.nodes.map((node: any) => node.path);
                expect(paths).to.include(restrictedNodePath);
            });
        });

        it('leaves a node the caller may not write untouched on a site-wide delete', () => {
            cy.apolloClient({username: tagManagerUser, password})
                .apollo({
                    mutationFile: 'tagManager/deleteTag.graphql',
                    variables: {siteKey, tag: 'restricted'}
                }).should((result: any) => {
                    const workspaceResults = result.data.admin.jahia.tagManager.deleteTag.workspaceResults;
                    // eslint-disable-next-line max-nested-callbacks
                    const processed = workspaceResults.reduce((total: number, wsResult: any) => total + wsResult.processedCount, 0);
                    expect(processed, 'no node is written').to.equal(0);
                });

            cy.apollo({
                queryFile: 'tagManager/getTaggedContent.graphql',
                variables: {siteKey, tag: 'restricted'}
            }).should((result: any) => {
                // eslint-disable-next-line max-nested-callbacks
                const paths = result.data.admin.jahia.tagManager.taggedContent.nodes.map((node: any) => node.path);
                expect(paths).to.include(restrictedNodePath);
            });
        });

        it('rejects renameTagOnNode on a node the caller may not write', () => {
            cy.apolloClient({username: tagManagerUser, password})
                .apollo({
                    mutationFile: 'tagManager/renameTagOnNode.graphql',
                    variables: {siteKey, tag: 'restricted', newName: 'restricted-renamed', nodeId: restrictedNodeUuid},
                    errorPolicy: 'all'
                }).should((result: any) => {
                    expect(result.errors).to.exist.and.not.be.empty;
                });

            cy.apollo({
                queryFile: 'tagManager/getTaggedContent.graphql',
                variables: {siteKey, tag: 'restricted'}
            }).should((result: any) => {
                // eslint-disable-next-line max-nested-callbacks
                const paths = result.data.admin.jahia.tagManager.taggedContent.nodes.map((node: any) => node.path);
                expect(paths).to.include(restrictedNodePath);
            });
        });

        it('rejects deleteTagOnNode on a node the caller may not write', () => {
            cy.apolloClient({username: tagManagerUser, password})
                .apollo({
                    mutationFile: 'tagManager/deleteTagOnNode.graphql',
                    variables: {siteKey, tag: 'restricted', nodeId: restrictedNodeUuid},
                    errorPolicy: 'all'
                }).should((result: any) => {
                    expect(result.errors).to.exist.and.not.be.empty;
                });

            cy.apollo({
                queryFile: 'tagManager/getTaggedContent.graphql',
                variables: {siteKey, tag: 'restricted'}
            }).should((result: any) => {
                // eslint-disable-next-line max-nested-callbacks
                const paths = result.data.admin.jahia.tagManager.taggedContent.nodes.map((node: any) => node.path);
                expect(paths).to.include(restrictedNodePath);
            });
        });
    });
});
