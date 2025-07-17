import gql from 'graphql-tag';
import {validateError} from './validateErrors';

type ExtendsType = {
    name: string;
    extends : {
        nodes : {
            name: string;
            abstract: boolean;
            __typename: string;
        }
    }
};
type ExtendedByType = {
    name: string;
    extendedBy : {
        nodes : {
            name: string;
            abstract: boolean;
            __typename: string;
        }
    }
};

describe('Node types graphql test', () => {
    before('Create nodes', () => {
        cy.apollo({
            mutation: gql`
                mutation {
                    jcr {
                        addNode(
                            parentPathOrId: "/"
                            name: "testList"
                            primaryNodeType: "jnt:contentList"
                            mixins: ["jmix:renderable"]
                        ) {
                            addChild(name: "testSubList", primaryNodeType: "jnt:contentList") {
                                addChild(name: "testSubSubList", primaryNodeType: "jnt:contentList") {
                                    uuid
                                }
                                uuid
                            }
                            uuid
                        }
                    }
                }
            `
        });
    });

    it('Get all node types', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeByPath(path: "/testList") {
                            primaryNodeType {
                                name
                                mixin
                                hasOrderableChildNodes
                                queryable
                                systemId
                            }
                            allowedChildNodeTypes(includeSubTypes: false) {
                                name
                            }
                            mixinTypes {
                                name
                                mixin
                            }
                        }
                    }
                }
            `
        }).should(response => {
            expect(response.data.jcr.nodeByPath).to.exist;

            const primaryNodeType = response?.data?.jcr?.nodeByPath.primaryNodeType;
            expect(primaryNodeType.name).to.equal('jnt:contentList');
            expect(primaryNodeType.mixin).to.equal(false);
            expect(primaryNodeType.hasOrderableChildNodes).to.equal(true);
            expect(primaryNodeType.queryable).to.equal(true);
            expect(primaryNodeType.systemId).to.equal('system-jahia');

            const mixinTypes = response?.data?.jcr?.nodeByPath.mixinTypes;
            expect(mixinTypes).to.have.length(1);
            expect(mixinTypes[0].name).to.equal('jmix:renderable');
            expect(mixinTypes[0].mixin).to.equal(true);

            const allowedChildNodeTypes = response?.data?.jcr?.nodeByPath.allowedChildNodeTypes;
            expect(allowedChildNodeTypes).to.have.length(1);
            expect(allowedChildNodeTypes[0].name).to.equal('jmix:droppableContent');
        });
    });

    it('Check node type', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeByPath(path: "/testList") {
                            test1: isNodeType(type: { types: ["jnt:contentList"] })
                            test2: isNodeType(type: { types: ["jmix:renderable"] })
                            test3: isNodeType(type: { types: ["jnt:content", "jnt:virtualsite"], multi: ALL })
                            test4: isNodeType(type: { types: ["jnt:content", "jnt:virtualsite"], multi: ANY })
                            test5: isNodeType(type: { types: ["wrongInput"], multi: ANY })
                        }
                    }
                }
            `
        }).should(response => {
            expect(response.data.jcr.nodeByPath).to.exist;

            const node = response?.data?.jcr?.nodeByPath;
            expect(node.test1).to.equal(true);
            expect(node.test2).to.equal(true);
            expect(node.test3).to.equal(false);
            expect(node.test4).to.equal(true);
            expect(node.test5).to.equal(false);
        });
    });

    it('Get node definition', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeByPath(path: "/testList") {
                            definition {
                                declaringNodeType {
                                    name
                                }
                                name
                            }
                        }
                    }
                }
            `
        }).should(response => {
            expect(response.data.jcr.nodeByPath).to.exist;

            const definition = response?.data?.jcr?.nodeByPath.definition;
            expect(definition.name).to.equal('*');
            expect(definition.declaringNodeType.name).to.equal('nt:unstructured');
        });
    });

    it('Get property definition', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeByPath(path: "/testList") {
                            property(name: "jcr:created") {
                                definition {
                                    declaringNodeType {
                                        name
                                    }
                                    name
                                }
                            }
                        }
                    }
                }
            `
        }).should(response => {
            expect(response.data.jcr.nodeByPath).to.exist;

            const definition = response?.data?.jcr?.nodeByPath.property.definition;
            expect(definition.name).to.equal('jcr:created');
            expect(definition.declaringNodeType.name).to.equal('mix:created');
        });
    });

    it('Get property definition with constraints', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeByPath(path: "/j:acl/GRANT_g_users") {
                            uuid
                            path
                            name
                            aceType: property(name: "j:aceType") {
                                definition {
                                    declaringNodeType {
                                        name
                                    }
                                    name
                                    constraints
                                }
                                name
                            }
                            principal: property(name: "j:principal") {
                                definition {
                                    declaringNodeType {
                                        name
                                    }
                                    name
                                    constraints
                                }
                                name
                            }
                        }
                    }
                }
            `
        }).should(response => {
            expect(response.data.jcr.nodeByPath).to.exist;

            const aceTypeDefinition = response?.data?.jcr?.nodeByPath.aceType.definition;
            expect(aceTypeDefinition.name).to.equal('j:aceType');
            expect(aceTypeDefinition.declaringNodeType.name).to.equal('jnt:ace');
            expect(aceTypeDefinition.constraints).to.have.length(2);
            expect(aceTypeDefinition.constraints[0]).to.equal('GRANT');
            expect(aceTypeDefinition.constraints[1]).to.equal('DENY');

            const principalDefinition = response?.data?.jcr?.nodeByPath.principal.definition;
            expect(principalDefinition.name).to.equal('j:principal');
            expect(principalDefinition.declaringNodeType.name).to.equal('jnt:ace');
            expect(principalDefinition.constraints).to.have.length(0);
        });
    });

    it('Get node type', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeTypeByName(name: "jmix:editorialContent") {
                            name
                            displayName(language: "en")
                        }
                    }
                }
            `
        }).should(response => {
            expect(response.data.jcr.nodeTypeByName).to.exist;
            expect(response.data.jcr.nodeTypeByName.name).to.equal('jmix:editorialContent');
            expect(response.data.jcr.nodeTypeByName.displayName).to.equal('Editorial content');
        });
    });

    it('Get node types', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeTypesByNames(names: ["jmix:editorialContent", "jmix:siteContent"]) {
                            name
                            displayName(language: "en")
                        }
                    }
                }
            `
        }).should(response => {
            expect(response.data.jcr.nodeTypesByNames).to.exist;
            const nodes = response.data.jcr.nodeTypesByNames;
            const names: string[] = [];
            const displayNames: string[] = [];
            for (let n = 0; n < nodes.length; n++) {
                names[n] = nodes[n].name;
                displayNames[n] = nodes[n].displayName;
            }

            expect(nodes).to.have.length(2);
            expect(names).to.contain('jmix:editorialContent');
            expect(displayNames).to.contain('Editorial content');
            expect(names).to.contain('jmix:siteContent');
            expect(displayNames).to.contain('siteContent');
        });
    });

    it('Get an error with wrong node type name', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeTypeByName(name: "jmix:wrong") {
                            name
                            displayName(language: "en")
                        }
                    }
                }
            `,
            errorPolicy: 'all'
        }).should(result => {
            validateError(result, 'javax.jcr.nodetype.NoSuchNodeTypeException: Unknown type : jmix:wrong');
        });
    });

    it('Get node types from a module', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeTypes(filter: { modules: ["default"] }) {
                            nodes {
                                name
                                systemId
                            }
                        }
                    }
                }
            `
        }).should(response => {
            expect(response.data.jcr.nodeTypes).to.exist;
            const nodes = response.data.jcr.nodeTypes.nodes;
            const names: string[] = [];
            for (let n = 0; n < nodes.length; n++) {
                names[n] = nodes[n].name;
            }

            expect(names).to.contain('jnt:text');
            expect(names).to.not.contain('nt:base');
        });
    });

    it('Get an error with wrong module name', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeTypes(filter: { modules: ["wrongModule"] }) {
                            nodes {
                                name
                                systemId
                            }
                        }
                    }
                }
            `
        }).should(response => {
            expect(response.data.jcr.nodeTypes.nodes).to.have.length(0);
        });
    });

    it('Get mixins', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeTypes(filter: { includeNonMixins: false }) {
                            nodes {
                                name
                                mixin
                            }
                        }
                    }
                }
            `
        }).should(response => {
            expect(response.data.jcr.nodeTypes).to.exist;
            const nodes = response.data.jcr.nodeTypes.nodes;
            const names: string[] = [];
            for (let n = 0; n < nodes.length; n++) {
                names[n] = nodes[n].name;
                expect(nodes[n].mixin).to.equal(true);
            }

            expect(names).to.contain('mix:created');
            expect(names).to.not.contain('nt:base');
        });
    });

    it('Get included and not excluded node types', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeTypes(
                            filter: {
                                includeMixins: false
                                siteKey: "systemsite"
                                includeTypes: ["jmix:editorialContent"]
                                excludeTypes: ["jmix:studioOnly", "jmix:hiddenType"]
                            }
                        ) {
                            nodes {
                                isEditorialContent: isNodeType(type: { types: ["jmix:editorialContent"] })
                                isStudioOnly: isNodeType(type: { types: ["jmix:studioOnly"] })
                                isHiddenType: isNodeType(type: { types: ["jmix:hiddenType"] })
                            }
                        }
                    }
                }
            `
        }).should(response => {
            expect(response.data.jcr.nodeTypes).to.exist;
            const nodes = response.data.jcr.nodeTypes.nodes;
            expect(nodes).to.have.length.gt(0);
            let n = 0;
            while (n < nodes.length) {
                expect(nodes[n].isEditorialContent).to.equal(true);
                expect(nodes[n].isStudioOnly).to.equal(false);
                expect(nodes[n].isHiddenType).to.equal(false);
                n++;
            }
        });
    });

    it('Get \'extends\' field', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeTypes(filter: {includeTypes: ["jmix:internalLink","jmix:categorized"]}) {
                            nodes {
                                name
                                extends {
                                    nodes {
                                        name
                                        abstract
                                    }
                                }
                            }
                        }
                    }
                }
            `
        }).should(response => {
            expect(response.data.jcr.nodeTypes).to.exist;
            const nodes = response.data.jcr.nodeTypes.nodes;
            expect(nodes).to.have.length(2);

            const categorizedNode = nodes.find((node: ExtendsType) => node.name === 'jmix:categorized');
            const internalLinkNode = nodes.find((node: ExtendsType) => node.name === 'jmix:internalLink');

            // Single extend:
            expect(internalLinkNode).to.exist;
            expect(internalLinkNode.extends.nodes).to.exist;
            expect(internalLinkNode.extends.nodes).to.have.length(1);
            expect(internalLinkNode.extends.nodes[0].name).to.equal('jnt:content');
            expect(internalLinkNode.extends.nodes[0].abstract).to.equal(false);
            expect(internalLinkNode.extends.nodes[0].__typename).to.equal('JCRNodeType');

            // Multiple extends:
            expect(categorizedNode).to.exist;
            expect(categorizedNode.extends.nodes).to.exist;
            expect(categorizedNode.extends.nodes).to.have.length(3);
            expect(categorizedNode.extends.nodes[0].name).to.equal('nt:hierarchyNode');
            expect(categorizedNode.extends.nodes[0].abstract).to.equal(true);
            expect(categorizedNode.extends.nodes[0].__typename).to.equal('JCRNodeType');
            expect(categorizedNode.extends.nodes[1].name).to.equal('jnt:content');
            expect(categorizedNode.extends.nodes[1].abstract).to.equal(false);
            expect(categorizedNode.extends.nodes[1].__typename).to.equal('JCRNodeType');
            expect(categorizedNode.extends.nodes[2].name).to.equal('jnt:page');
            expect(categorizedNode.extends.nodes[2].abstract).to.equal(false);
            expect(categorizedNode.extends.nodes[2].__typename).to.equal('JCRNodeType');
        });
    });

    it('Get \'extends\' field with filtering', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeTypes(filter: {includeTypes: ["jmix:internalLink","jmix:categorized"]}) {
                            nodes {
                                name
                                extends(filter:{excludeTypes:["jnt:content"], includeAbstract:false}) {
                                    nodes {
                                        name
                                        abstract
                                    }
                                }
                            }
                        }
                    }
                }
            `
        }).should(response => {
            expect(response.data.jcr.nodeTypes).to.exist;
            const nodes = response.data.jcr.nodeTypes.nodes;
            expect(nodes).to.have.length(2);

            const categorizedNode = nodes.find((node: ExtendsType) => node.name === 'jmix:categorized');
            const internalLinkNode = nodes.find((node: ExtendsType) => node.name === 'jmix:internalLink');

            // No match for 'jmix:internalLink'
            expect(internalLinkNode).to.exist;
            expect(internalLinkNode.extends.nodes).to.exist;
            expect(internalLinkNode.extends.nodes).to.be.empty;

            // Only one match for 'jmix:categorized'
            expect(categorizedNode).to.exist;
            expect(categorizedNode.extends.nodes).to.exist;
            expect(categorizedNode.extends.nodes).to.have.length(1);
            expect(categorizedNode.extends.nodes[0].name).to.equal('jnt:page');
            expect(categorizedNode.extends.nodes[0].abstract).to.equal(false);
            expect(categorizedNode.extends.nodes[0].__typename).to.equal('JCRNodeType');
        });
    });

    it('Get \'extendBy\' field', () => {
        // Use considerSubTypes:false to only get "nt:hierarchyNode" and "jnt:content", not the node types inheriting from them
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeTypes(filter: {includeTypes: ["nt:hierarchyNode", "jnt:content"], considerSubTypes:false}) {
                            nodes {
                                name
                                extendedBy {
                                    nodes {
                                        name
                                        abstract
                                    }
                                }
                            }
                        }
                    }
                }
            `
        }).should(response => {
            expect(response.data.jcr.nodeTypes).to.exist;
            const nodes = response.data.jcr.nodeTypes.nodes;
            expect(nodes).to.have.length(2);

            const hierarchyNode = nodes.find((node: ExtendedByType) => node.name === 'nt:hierarchyNode');
            const contentNode = nodes.find((node: ExtendedByType) => node.name === 'jnt:content');

            // 'nt:hierarchyNode':
            expect(hierarchyNode).to.exist;
            expect(hierarchyNode.extendedBy.nodes).to.exist;
            expect(hierarchyNode.extendedBy.nodes).to.have.length(3);
            expect(hierarchyNode.extendedBy.nodes[0].name).to.equal('jmix:categorized');
            expect(hierarchyNode.extendedBy.nodes[1].name).to.equal('jmix:keywords');
            expect(hierarchyNode.extendedBy.nodes[2].name).to.equal('jmix:tagged');

            // 'jnt:content':
            expect(contentNode).to.exist;
            expect(contentNode.extendedBy.nodes).to.exist;
            expect(contentNode.extendedBy.nodes).to.have.length(8);
            expect(contentNode.extendedBy.nodes[0].name).to.equal('jmix:internalLink');
            expect(contentNode.extendedBy.nodes[1].name).to.equal('jmix:categorized');
            expect(contentNode.extendedBy.nodes[2].name).to.equal('jmix:renderable');
            expect(contentNode.extendedBy.nodes[3].name).to.equal('jmix:cache');
            expect(contentNode.extendedBy.nodes[4].name).to.equal('jmix:externalLink');
            expect(contentNode.extendedBy.nodes[5].name).to.equal('jmix:keywords');
            expect(contentNode.extendedBy.nodes[6].name).to.equal('jmix:tagged');
            expect(contentNode.extendedBy.nodes[7].name).to.equal('jmix:requiredPermissions');
        });
    });

    it('Get \'extendedBy\' field with filtering', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeTypes(filter: {includeTypes: [ "jnt:content"], considerSubTypes:false}) {
                            nodes {
                                name
                                extendedBy(filter:{includeTypes:["jmix:keywords"]}) {
                                    nodes {
                                        name
                                        abstract
                                    }
                                }
                            }
                        }
                    }
                }
            `
        }).should(response => {
            expect(response.data.jcr.nodeTypes).to.exist;
            const nodes = response.data.jcr.nodeTypes.nodes;
            expect(nodes).to.have.length(1);
            const contentNode = nodes.find((node: ExtendedByType) => node.name === 'jnt:content');
            expect(contentNode).to.exist;
            expect(contentNode.extendedBy.nodes).to.exist;
            expect(contentNode.extendedBy.nodes).to.have.length(1);
            expect(contentNode.extendedBy.nodes[0].name).to.equal('jmix:keywords');
        });
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
