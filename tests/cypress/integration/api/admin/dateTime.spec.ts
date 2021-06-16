/* eslint-disable @typescript-eslint/no-explicit-any */
import { DocumentNode } from 'graphql'
import { isValid } from 'date-fns'
import { apollo } from '../../../support/apollo'

describe('admin.datetime - Jahia Server time', () => {
    let GQL_DATETIME: DocumentNode

    before('load graphql file', function () {
        GQL_DATETIME = require(`graphql-tag/loader!../../../fixtures/admin/datetime.graphql`)
    })

    it('Get Jahia server current time (as root)', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_DATETIME,
            },
        ).should((response: any) => {
            cy.log(`Date on the server is: ${response.data.admin.datetime}`)
            expect(isValid(new Date(response.data.admin.datetime))).to.be.true
        })
    })

    // Note: Should we allow guest to query server time ?
    it('Get Jahia server current time (as guest)', () => {
        cy.apolloQuery(apollo(Cypress.config().baseUrl, { username: 'guest', password: 'I-DO-NOT-EXIST' }), {
            query: GQL_DATETIME,
        }).should((response: any) => {
            expect(response.errors).to.be.not.empty
        })
    })
})
