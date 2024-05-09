import gql from 'graphql-tag';
import {addNode, deleteNode, setNodeProperty} from '@jahia/cypress';

describe('GraphQL References Test', () => {
    before('Create test nodes', () => {
        addNode({
            parentPathOrId: '/',
            name: 'testList',
            primaryNodeType: 'jnt:contentList',
            mixins: ['jmix:liveProperties'],
            properties: [{name: 'j:liveProperties', values: ['/testList/testSubList1', '/testList/testSubList2']}],
            children: [
                {
                    name: 'testSubList1',
                    primaryNodeType: 'jnt:contentList',
                    properties: [{name: 'jcr:title', value: '/testList/testSubList2', language: 'en'}]
                },
                {
                    name: 'testSubList2',
                    primaryNodeType: 'jnt:contentList',
                    properties: [{name: 'jcr:title', value: '/nonExistingPath', language: 'en'}],
                    mixins: ['jmix:unstructured']
                },
                {
                    name: 'reference1',
                    primaryNodeType: 'jnt:contentReference'
                },
                {
                    name: 'reference2',
                    primaryNodeType: 'jnt:contentReference'
                }
            ]
        }).then(response => {
            const subNode1Uuid = response.data.jcr.addNode.addChildrenBatch[0].uuid;
            setNodeProperty('/testList/reference1', 'j:node', subNode1Uuid, 'en');
            setNodeProperty('/testList/reference2', 'j:node', subNode1Uuid, 'en');
            addNode({
                parentPathOrId: '/testList/testSubList2',
                primaryNodeType: 'nt:linkedFile',
                name: 'reference3',
                properties: [{name: 'jcr:content', value: subNode1Uuid}]
            });
        });
    });

    it('should retrieve references', () => {
        cy.apollo({
            queryFile: 'jcr/getNodeReferencesByPath.graphql',
            variables: {
                path: '/testList/testSubList1'
            }
        }).should(result => {
            const references = result?.data?.jcr?.nodeByPath?.references?.nodes as Array<{node: {name: string}}>;
            const referenceNames = references.map(r => r.node.name);
            expect(referenceNames).to.contain('reference1');
            expect(referenceNames).to.contain('reference2');
            expect(referenceNames).to.contain('reference3');
        });
    });

    it('should retrieve referenced node by reference', () => {
        cy.apollo({
            queryFile: 'jcr/getReferencedNodeByProperty.graphql',
            variables: {
                path: '/testList/reference1',
                property: 'j:node'
            }
        }).should(result => {
            const refNodeName = result.data.jcr.nodeByPath.property.refNode.name;
            expect(refNodeName).to.equal('testSubList1');
        });
    });

    it('should retrieve referenced node by uuid string', () => {
        cy.apollo({
            queryFile: 'jcr/getReferencedNodeByProperty.graphql',
            variables: {
                path: '/testList/reference2',
                property: 'jcr:uuid'
            }
        }).should(result => {
            const refNodeName = result.data.jcr.nodeByPath.property.refNode.name;
            expect(refNodeName).to.equal('reference2');
        });
    });

    it('should retrieve referenced node by path string', () => {
        cy.apollo({
            queryFile: 'jcr/getReferencedNodeByProperty.graphql',
            variables: {
                path: '/testList/testSubList1',
                property: 'jcr:title',
                language: 'en'
            }
        }).should(result => {
            const refNodeName = result.data.jcr.nodeByPath.property.refNode.name;
            expect(refNodeName).to.equal('testSubList2');
        });
    });

    it('should not retrieve referenced node from property of wrong type', () => {
        cy.apollo({
            queryFile: 'jcr/getReferencedNodeByProperty.graphql',
            variables: {
                path: '/testList',
                property: 'jcr:lastModified'
            }
        }).should(result => {
            const refNode = result.data.jcr.nodeByPath.property.refNode;
            expect(refNode).to.equal(null);
        });
    });

    it('should not retrieve referenced node by wrong path string', () => {
        cy.apollo({
            queryFile: 'jcr/getReferencedNodeByProperty.graphql',
            variables: {
                path: '/testList/testSubList2',
                property: 'jcr:title',
                language: 'en'
            }
        }).should(result => {
            const refNode = result.data.jcr.nodeByPath.property.refNode;
            expect(refNode).to.equal(null);
        });
    });

    it('should not retrieve referenced node from multiple valued property', () => {
        cy.apollo({
            queryFile: 'jcr/getReferencedNodeByProperty.graphql',
            variables: {
                path: '/testList',
                property: 'j:liveProperties'
            }
        }).should(result => {
            const refNode = result.data.jcr.nodeByPath.property.refNode;
            expect(refNode).to.equal(null);
        });
    });

    it('should retrieve referenced nodes', () => {
        cy.apollo({
            queryFile: 'jcr/getReferencedNodesByProperty.graphql',
            variables: {
                path: '/testList',
                property: 'j:liveProperties'
            }
        }).should(result => {
            const refNodeNames = result.data.jcr.nodeByPath.property.refNodes.map(r => r.name);
            expect(refNodeNames).to.have.lengthOf(2);
            expect(refNodeNames).to.contain('testSubList1');
            expect(refNodeNames).to.contain('testSubList2');
        });
    });

    it('should not retrieve referenced nodes from single valued property', () => {
        cy.apollo({
            queryFile: 'jcr/getReferencedNodesByProperty.graphql',
            variables: {
                path: '/testList/reference1',
                property: 'j:node'
            }
        }).should(result => {
            const refNodes = result.data.jcr.nodeByPath.property.refNodes;
            expect(refNodes).to.equal(null);
        });
    });

    after('Delete test nodes', () => {
        deleteNode('/testList');
    });
});
