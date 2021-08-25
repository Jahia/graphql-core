/* eslint-disable @typescript-eslint/no-explicit-any */
import gql from 'graphql-tag'
import { apollo } from '../../../support/apollo'
import { DocumentNode } from 'graphql'

describe('Test getNodeByPath', () => {
    let GQL_NODE: DocumentNode

    before('load graphql file', function () {
        GQL_NODE = require(`graphql-tag/loader!../../../fixtures/jcr/imageByPath.graphql`)
    })

    it('get an image', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_NODE,
                variables: { path: '/sites/digitall/files/images/people/user.jpg' },
            },
        ).should((response: any) => {
            expect(response.data.jcr.nodeByPath).to.exist
            expect(response.data.jcr.nodeByPath.name).to.equal('user.jpg')
            expect(response.data.jcr.nodeByPath.published.booleanValue).to.equal(true)
            expect(response.data.jcr.nodeByPath.height.longValue).to.equal(200)
        })
    })
})
