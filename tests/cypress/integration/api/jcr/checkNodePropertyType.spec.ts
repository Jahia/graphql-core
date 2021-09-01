/* eslint-disable @typescript-eslint/no-explicit-any */
import { apollo } from '../../../support/apollo'
import { DocumentNode } from 'graphql'

describe('Test page properties', () => {
    let GQL_NODE: DocumentNode
    let GQL_ADD_NODE: DocumentNode
    let GQL_DELETE_NODE: DocumentNode

    before('load graphql file and create node', function () {
        GQL_NODE = require(`graphql-tag/loader!../../../fixtures/jcr/pageByPath.graphql`)
        GQL_ADD_NODE = require(`graphql-tag/loader!../../../fixtures/jcr/addNode.graphql`)
        GQL_DELETE_NODE = require(`graphql-tag/loader!../../../fixtures/jcr/deleteNode.graphql`)
        cy.apolloMutate(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                mutation: GQL_ADD_NODE,
                variables: {
                    parentPathOrId: '/sites/systemsite/home',
                    nodeName: 'testPage',
                    nodeType: 'jnt:page',
                    properties: [
                        { name: 'j:templateName', type: 'STRING', value: 'default', language: 'en' },
                        {
                            name: 'j:isHomePage',
                            type: 'BOOLEAN',
                            value: false,
                            language: 'en',
                        },
                    ],
                },
            },
        )
    })

    it('Get a page by path and verify isHomePage has a boolean value', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_NODE,
                variables: { path: '/sites/systemsite/home/testPage' },
            },
        ).should((response: any) => {
            expect(response.data.jcr.nodeByPath).to.exist
            expect(response.data.jcr.nodeByPath.name).to.equal('testPage')
            expect(response.data.jcr.nodeByPath.isHomePage.booleanValue).to.equal(false)
        })
    })

    after('Delete testPage node', function () {
        cy.apolloMutate(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                mutation: GQL_DELETE_NODE,
                variables: {
                    pathOrId: '/sites/systemsite/home/testPage',
                },
            },
        )
    })
})
