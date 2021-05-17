/* eslint-disable @typescript-eslint/no-explicit-any */
import { DocumentNode } from 'graphql'
import { sub } from 'date-fns'
import { createNodes } from '../../../support/createNodes'

interface uuidNode {
    name: string
    uuid: string
}

describe('GraphQLCriteriaTest', () => {
    let GQL_ADDNODE: DocumentNode
    let GQL_ADDMIXINSPROPERTIES: DocumentNode
    let GQL_DELETENODE: DocumentNode
    let GQL_NODEBYCRITERIA: DocumentNode

    const yesterday = sub(new Date(), { days: 1 })
    // This object contains two things:
    // - Data to be submitted to the API during validation
    // - Data to be used to validate data coming from the API
    let nodes = [
        {
            name: 'testList',
            path: '/testList',
            parent: { path: '/' },
        },
        {
            name: 'testSubList1',
            path: '/testList/testSubList1',
            parent: { path: '/testList' },
            mixins: ['jmix:liveProperties', 'jmix:keywords', 'jmix:size'],
            properties: [
                { name: 'jcr:title', value: 'text EN - subList1', language: 'en' },
                { name: 'jcr:title', value: 'text FR - subList1', language: 'fr_FR' },
                { name: 'j:liveProperties', values: ['liveProperty1', 'liveProperty2'] },
                { name: 'j:keywords', values: ['keyword 1', 'keyword 2', 'keyword'] },
                { name: 'j:height', value: 100 },
                { name: 'j:lastPublished', value: new Date().toISOString() },
            ],
        },
        {
            name: 'testSubList2',
            path: '/testList/testSubList2',
            parent: { path: '/testList' },
            mixins: ['jmix:tagged', 'jmix:size'],
            properties: [
                { name: 'jcr:title', value: 'text EN - subList2', language: 'en' },
                { name: 'jcr:title', value: 'text FR - subList2', language: 'fr_FR' },
                { name: 'j:tagList', values: ['sometag', 'keyword'] },
                { name: 'j:height', value: 200 },
                { name: 'j:lastPublished', value: yesterday },
            ],
        },
        {
            name: 'testSubList3',
            path: '/testList/testSubList3',
            parent: { path: '/testList' },
            mixins: ['jmix:size'],
            properties: [
                { name: 'j:height', value: 300 },
                { name: 'j:lastPublished', value: sub(new Date(), { days: 2 }) },
            ],
        },
        {
            name: 'testSubList4',
            path: '/testList/testSubList4',
            parent: { path: '/testList' },
            mixins: ['jmix:liveProperties'],
            properties: [
                { name: 'j:liveProperties', values: ['liveProperty3', 'liveProperty4'] },
                { name: 'j:lastPublished', value: sub(new Date(), { days: 3 }) },
            ],
        },
        {
            name: 'testSubList4_1',
            path: '/testList/testSubList4/testSubList4_1',
            parent: { path: '/testList/testSubList4' },
        },
        {
            name: 'testSubList4_2',
            path: '/testList/testSubList4/testSubList4_2',
            parent: { path: '/testList/testSubList4' },
        },
        {
            name: 'testSubList4_3',
            path: '/testList/testSubList4/testSubList4_3',
            parent: { path: '/testList/testSubList4' },
        },
    ]

    const createNodes = (parentPath) => {
        for (const node of nodes.filter((n) => n.parent.path === parentPath)) {
            cy.task('apolloNode', {
                baseUrl: Cypress.config().baseUrl,
                authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
                mode: 'mutate',
                variables: {
                    parentPathOrId: node.parent.path,
                    nodeName: node.name,
                    nodeType: 'jnt:contentList',
                },
                query: GQL_ADDNODE,
            }).then((response: any) => {
                cy.log(
                    `Created node: ${node.name} in path: ${node.parent.path}, UUID: ${response.data.jcr.addNode.uuid}`,
                    JSON.stringify(response),
                )
                expect(response.data.jcr.addNode.uuid).not.to.be.null
                // Not ideal, but this mutate the nodes array to add the newly created uuid
                nodes = nodes.map((n) => {
                    if (n.name === node.name) {
                        return {
                            ...n,
                            uuid: response.data.jcr.addNode.uuid,
                        }
                    }
                    return n
                })
                if (node.properties !== undefined && node.mixins !== undefined) {
                    cy.task('apolloNode', {
                        baseUrl: Cypress.config().baseUrl,
                        authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
                        mode: 'mutate',
                        variables: {
                            pathOrId: `${node.parent.path}/${node.name}`,
                            mixins: node.mixins,
                            properties: node.properties,
                        },
                        query: GQL_ADDMIXINSPROPERTIES,
                    }).then((response: any) => {
                        cy.log(
                            `Added mixins and properties to: ${node.parent.path}${node.name}`,
                            JSON.stringify(response),
                        )
                        expect(response.data.jcr.mutateNode.addMixins.length).to.equal(node.mixins.length)
                        expect(response.data.jcr.mutateNode.setPropertiesBatch.length).to.equal(node.properties.length)
                    })
                }
                // Check if there are any nodes having the currernt created node as parent, if yes, create those nodes
                const childrenPath =
                    node.parent.path === '/' ? `${node.parent.path}${node.name}` : `${node.parent.path}/${node.name}`
                if (nodes.filter((n) => n.parent.path === childrenPath).length > 0) {
                    createNodes(childrenPath)
                }
            })
        }
    }

    // Validates a node received as an API response towards the source dataset
    const validateNodes = (nodes, validateNodeNames, responseNodes) => {
        // Make sure reponseNodes only contains nodes in validateNodeNames
        const responseNodesNames = responseNodes.map((n) => n.name)
        expect(JSON.stringify(validateNodeNames.sort())).to.equal(JSON.stringify(responseNodesNames.sort()))

        for (const responseNode of responseNodes) {
            const currentNode = nodes.find((n) => n.name === responseNode.name)
            for (const key of Object.keys(responseNode).filter((k) => k !== '__typename')) {
                if (currentNode[key] !== undefined) {
                    if (key === 'parent') {
                        expect(currentNode[key].path).to.equal(responseNode[key].path)
                    } else {
                        expect(currentNode[key]).to.equal(responseNode[key])
                    }
                }
            }
        }
    }

    before('load graphql file and create test dataset', () => {
        GQL_ADDNODE = require(`graphql-tag/loader!../../../fixtures/jcr/addNode.graphql`)
        GQL_ADDMIXINSPROPERTIES = require(`graphql-tag/loader!../../../fixtures/jcr/addMixinsProperties.graphql`)
        GQL_DELETENODE = require(`graphql-tag/loader!../../../fixtures/jcr/deleteNode.graphql`)
        GQL_NODEBYCRITERIA = require(`graphql-tag/loader!../../../fixtures/jcr/nodeByCriteria.graphql`)
        createNodes('/')
    })

    after('Clear the created dataset', () => {
        cy.log('Preparing the test suite dataset: createList')
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            mode: 'mutate',
            variables: {
                pathOrId: '/testList',
            },
            query: GQL_DELETENODE,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.deleteNode).to.be.true
        })
    })

    it('shouldRetrieveDescendantNodesByAncestorPath', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: { paths: ['/testList'], nodeType: 'jnt:contentList' },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(7)
            const valideNodesNames = [
                'testSubList1',
                'testSubList2',
                'testSubList3',
                'testSubList4',
                'testSubList4_1',
                'testSubList4_2',
                'testSubList4_3',
            ]
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldRetrieveDescendantNodesWithoutPath', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    nodeConstraint: { property: 'jcr:title', contains: 'SUBLIST1' },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(1)
            const valideNodesNames = ['testSubList1']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldRetrieveChildNodesByParentPath', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: { paths: ['/testList'], pathType: 'PARENT', nodeType: 'jnt:contentList' },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(4)
            const valideNodesNames = ['testSubList1', 'testSubList2', 'testSubList3', 'testSubList4']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldRetrieveNodesByPaths', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    paths: ['/testList', '/testList/testSubList2', '/testList/testSubList4/testSubList4_2'],
                    pathType: 'OWN',
                    nodeType: 'jnt:contentList',
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(3)
            const valideNodesNames = ['testList', 'testSubList2', 'testSubList4_2']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldRetrieveNodesByPropertyContainsExpression', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: { property: 'jcr:title', contains: 'SUBLIST1' },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(1)
            const valideNodesNames = ['testSubList1']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldRetrieveNodesByNodeContainsExpression', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    pathType: 'ANCESTOR',
                    nodeConstraint: {
                        any: [{ contains: 'keyword' }, { contains: 'keyword', property: 'j:tagList' }],
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(2)
            const valideNodesNames = ['testSubList1', 'testSubList2']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldRetrieveNodesByLikeExpression', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: { property: 'jcr:title', like: '%subList%' },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(2)
            const valideNodesNames = ['testSubList1', 'testSubList2']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldRetrieveNodeByEqualsExpression', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    pathType: 'ANCESTOR',
                    nodeConstraint: { property: 'jcr:title', equals: 'text EN - subList1' },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(1)
            const valideNodesNames = ['testSubList1']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldRetrieveNodeByNotEqualsExpression', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    language: 'en',
                    paths: '/testList',
                    pathType: 'ANCESTOR',
                    nodeConstraint: { property: 'jcr:title', notEquals: 'text EN - subList1' },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(1)
            const valideNodesNames = ['testSubList2']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldRetrieveNodeByLessThanExpression', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: { property: 'j:height', lt: 200 },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(1)
            const valideNodesNames = ['testSubList1']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldRetrieveNodeByLessThanForDateExpression', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: {
                        property: 'j:lastPublished',
                        lt: yesterday.toISOString(),
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(2)
            const valideNodesNames = ['testSubList3', 'testSubList4']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldRetrieveNodeByLessThanOrEqualsToExpression', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: {
                        property: 'j:height',
                        lte: 200,
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(2)
            const valideNodesNames = ['testSubList1', 'testSubList2']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldRetrieveNodeByLessThanOrEqualsToForDateExpression', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: {
                        property: 'j:lastPublished',
                        lte: yesterday.toISOString(),
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(3)
            const valideNodesNames = ['testSubList2', 'testSubList3', 'testSubList4']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldRetrieveNodeByGreaterThanExpression', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: {
                        property: 'j:height',
                        gt: 200,
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(1)
            const valideNodesNames = ['testSubList3']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldRetrieveNodeByGreaterThanForDateExpression', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: {
                        property: 'j:lastPublished',
                        gt: yesterday.toISOString(),
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(1)
            const valideNodesNames = ['testSubList1']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldRetrieveNodeByGreaterThanOrEqualsToExpression', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: {
                        property: 'j:height',
                        gte: 200,
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(2)
            const valideNodesNames = ['testSubList2', 'testSubList3']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldRetrieveNodeByGreaterThanOrEqualsToForDateExpression', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: {
                        property: 'j:lastPublished',
                        gte: yesterday.toISOString(),
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(2)
            const valideNodesNames = ['testSubList1', 'testSubList2']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldRetrieveNodesByExistsExpression', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: {
                        property: 'j:liveProperties',
                        exists: true,
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(2)
            const valideNodesNames = ['testSubList1', 'testSubList4']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldRetrieveNodesByLastDaysExpression', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: {
                        property: 'j:lastPublished',
                        lastDays: 2,
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(2)
            const valideNodesNames = ['testSubList1', 'testSubList2']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldGetErrorRetrieveNodesByLastDaysExpression', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: {
                        property: 'j:lastPublished',
                        lastDays: '-1',
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria).to.be.null
            expect(response.errors[0].message).to.equal('lastDays value should not be negative')
        })
    })

    it('shouldRetrieveNodesByExistsExpressionWhenPropertyDoesNotExist', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    pathType: 'PARENT',
                    nodeConstraint: {
                        property: 'j:liveProperties',
                        exists: false,
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(2)
            const valideNodesNames = ['testSubList2', 'testSubList3']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldRetrieveNodeByAllConstraints', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: {
                        all: [
                            { property: 'j:liveProperties', exists: true },
                            { property: 'j:keywords', exists: true },
                        ],
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(1)
            const valideNodesNames = ['testSubList1']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldGetErrorNotRetrieveNodesByAllConstraintsWhenPropertyIsEmpty', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: {
                        all: [{ property: 'j:liveProperties', exists: true }, { like: '%subList1%' }],
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria).to.be.null
            expect(response.errors[0].message).to.equal("'property' field is required")
        })
    })

    it('shouldRetrieveNodeByAnyConstraints', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: {
                        any: [
                            { property: 'j:liveProperties', exists: true },
                            { property: 'j:keywords', exists: true },
                        ],
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(2)
            const valideNodesNames = ['testSubList1', 'testSubList4']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldGetErrorNotRetrieveNodesByAnyConstraintsWhenPropertyIsEmpty', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: {
                        any: [{ property: 'j:liveProperties', exists: true }, { like: '%subList1%' }],
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria).to.be.null
            expect(response.errors[0].message).to.equal("'property' field is required")
        })
    })

    it('shouldRetrieveNodeByNoneConstraints', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: {
                        none: [
                            { property: 'j:liveProperties', exists: true },
                            { property: 'j:keywords', exists: true },
                        ],
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(5)
            const valideNodesNames = [
                'testSubList2',
                'testSubList3',
                'testSubList4_1',
                'testSubList4_2',
                'testSubList4_3',
            ]
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldGetErrorNotRetrieveNodesByNoneConstraintsWhenPropertyIsEmpty', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: {
                        none: [{ property: 'j:liveProperties', exists: true }, { like: '%subList1%' }],
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria).to.be.null
            expect(response.errors[0].message).to.equal("'property' field is required")
        })
    })

    it('shouldRetrieveNodeByAllAnyNoneConstraints', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: {
                        all: [
                            {
                                any: [
                                    { property: 'j:keywords', exists: true },
                                    { property: 'j:tagList', exists: true },
                                ],
                            },
                            {
                                none: [
                                    { property: 'name', function: 'NODE_NAME', equals: 'landing' },
                                    { property: 'j:height', gte: 300 },
                                ],
                            },
                        ],
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(2)
            const valideNodesNames = ['testSubList1', 'testSubList2']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldGetErrorNotRetrieveNodeByAllAnyNoneConstraints', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    paths: '/testList',
                    nodeConstraint: {
                        all: [
                            {
                                any: [{ property: 'j:keywords', exists: true }, { like: '%subList1%' }],
                            },
                            {
                                none: [
                                    { property: 'name', function: 'NODE_NAME', equals: 'landing' },
                                    { property: 'j:height', gte: 300 },
                                ],
                            },
                        ],
                    },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria).to.be.null
            expect(response.errors[0].message).to.equal("'property' field is required")
        })
    })

    it('shouldGetErrorNotRetrieveNodesByLikeExpressionWhenPropertyIsEmpty', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: { nodeType: 'jnt:contentList', paths: '/testList', nodeConstraint: { like: '%subList1%' } },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria).to.be.null
            expect(response.errors[0].message).to.equal("'property' field is required")
        })
    })

    it('shouldGetErrorNotRetrieveNodesByNodeConstraintWhenNoComparisonSpecified', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: { nodeType: 'jnt:contentList', paths: '/testList', nodeConstraint: { property: 'property' } },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria).to.be.null
            expect(response.errors[0].message).to.equal(
                'At least one of the following constraint field is expected: contains,like,lastDays,equals,notEquals,lt,gte,exists,lte,gt',
            )
        })
    })

    it('shouldRetrieveNodesByInternationalizedPropertyValuePassingLanguage', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    language: 'fr_FR',
                    paths: '/testList',
                    nodeConstraint: { property: 'jcr:title', like: '%subList2%' },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(1)
            const valideNodesNames = ['testSubList2']
            validateNodes(nodes, valideNodesNames, response.data.jcr.nodesByCriteria.nodes)
        })
    })

    it('shouldNotRetrieveNodesByInternationalizedPropertyValuePassingDifferentLanguage', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            variables: {
                criteria: {
                    nodeType: 'jnt:contentList',
                    language: 'fr_FR',
                    paths: '/testList',
                    nodeConstraint: { property: 'jcr:title', like: 'SUBLIST3' },
                },
            },
            query: GQL_NODEBYCRITERIA,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.jcr.nodesByCriteria.nodes.length).to.equal(0)
        })
    })
})
