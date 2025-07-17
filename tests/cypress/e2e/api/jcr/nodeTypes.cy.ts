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
                        nodeTypes(filter: {includeTypes: ["extendtest:sample1", "extendtest:sample3"]}) {
                            nodes {
                                name
                                extends {
                                    nodes {
                                        name
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

            const sample1 = nodes.find((node: ExtendsType) => node.name === 'extendtest:sample1');
            const sample3 = nodes.find((node: ExtendsType) => node.name === 'extendtest:sample3');

            // Single extend:
            expect(sample1).to.exist;
            expect(sample1.extends.nodes).to.exist;
            expect(sample1.extends.nodes).to.have.length(1);
            expect(sample1.extends.nodes[0].name).to.equal('extendtest:base1');

            // Multiple extends:
            expect(sample3).to.exist;
            expect(sample3.extends.nodes).to.exist;
            expect(sample3.extends.nodes).to.have.length(2);
            expect(sample3.extends.nodes[0].name).to.equal('extendtest:base1');
            expect(sample3.extends.nodes[0].__typename).to.equal('JCRNodeType');
            expect(sample3.extends.nodes[1].name).to.equal('extendtest:base2');
            expect(sample3.extends.nodes[1].__typename).to.equal('JCRNodeType');
        });
    });

    it('Get \'extends\' field with filtering', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeTypes(filter: {includeTypes: ["extendtest:sample1", "extendtest:sample3"]}) {
                            nodes {
                                name
                                extends(filter:{excludeTypes:["extendtest:base1"]}) {
                                    nodes {
                                        name
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

            const sample1 = nodes.find((node: ExtendsType) => node.name === 'extendtest:sample1');
            const sample3 = nodes.find((node: ExtendsType) => node.name === 'extendtest:sample3');

            // No match:
            expect(sample1).to.exist;
            expect(sample1.extends.nodes).to.exist;
            expect(sample1.extends.nodes).to.be.empty;

            // Only one match for 'jmix:categorized'
            expect(sample3).to.exist;
            expect(sample3.extends.nodes).to.exist;
            expect(sample3.extends.nodes).to.have.length(1);
            expect(sample3.extends.nodes[0].name).to.equal('extendtest:base2');
            expect(sample3.extends.nodes[0].__typename).to.equal('JCRNodeType');
        });
    });

    it('Get \'extendBy\' field', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeTypes(filter: {includeTypes: ["extendtest:base1", "extendtest:base2"]}) {
                            nodes {
                                name
                                extendedBy {
                                    nodes {
                                        name
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

            const base1 = nodes.find((node: ExtendedByType) => node.name === 'extendtest:base1');
            const base2 = nodes.find((node: ExtendedByType) => node.name === 'extendtest:base2');

            expect(base1).to.exist;
            expect(base1.extendedBy.nodes).to.exist;
            const base1ExtendedByNames = base1.extendedBy.nodes.map((n: {name: string}) => n.name);
            expect(base1ExtendedByNames).to.have.length(3);
            expect(base1ExtendedByNames).to.have.members(['extendtest:sample1', 'extendtest:sample2', 'extendtest:sample3']);

            expect(base2).to.exist;
            expect(base2.extendedBy.nodes).to.exist;
            const base2ExtendedByNames = base2.extendedBy.nodes.map((n: {name: string}) => n.name);
            expect(base2ExtendedByNames).to.have.length(2);
            expect(base2ExtendedByNames).to.have.members(['extendtest:sample3', 'extendtest:sample4']);
        });
    });

    it('Get \'extendedBy\' field with filtering', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        nodeTypes(filter: {includeTypes: ["extendtest:base1", "extendtest:base2"]}) {
                            nodes {
                                name
                                extendedBy(filter:{includeTypes:["extendtest:sample3", "extendtest:sample4"]}) {
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

            const base1 = nodes.find((node: ExtendedByType) => node.name === 'extendtest:base1');
            const base2 = nodes.find((node: ExtendedByType) => node.name === 'extendtest:base2');

            expect(base1).to.exist;
            expect(base1.extendedBy.nodes).to.exist;
            const base1ExtendedByNames = base1.extendedBy.nodes.map((n: {name: string}) => n.name);
            expect(base1ExtendedByNames).to.have.length(1);
            expect(base1ExtendedByNames).to.have.members(['extendtest:sample3']);

            expect(base2).to.exist;
            expect(base2.extendedBy.nodes).to.exist;
            const base2ExtendedByNames = base2.extendedBy.nodes.map((n: {name: string}) => n.name);
            expect(base2ExtendedByNames).to.have.length(2);
            expect(base2ExtendedByNames).to.have.members(['extendtest:sample3', 'extendtest:sample4']);
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
