/* eslint-disable @typescript-eslint/no-explicit-any */
import { DocumentNode } from 'graphql'
import { isValid } from 'date-fns'

describe('admin.datetime - Jahia Server time', () => {
    let GQL_DATETIME: DocumentNode

    before('load graphql file', function () {
        GQL_DATETIME = require(`graphql-tag/loader!../../../fixtures/admin/datetime.graphql`)
    })

    it('Get Jahia server current time (as root)', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') },
            query: GQL_DATETIME,
        }).then(async (response: any) => {
            cy.log(JSON.stringify(response))
            cy.log(`Date on the server is: ${response.data.admin.datetime}`)
            expect(isValid(new Date(response.data.admin.datetime))).to.be.true
        })
    })

    // Note: Should we allow guest to query server time ?
    it('Get Jahia server current time (as guest)', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'guest', password: 'I-DO-NOT-EXIST' },
            query: GQL_DATETIME,
        }).then(async (response: any) => {
            cy.log(JSON.stringify(response))
            cy.log(`Date on the server is: ${response.data.admin.datetime}`)
            expect(response.data.currentUser.name).to.equal('guest')
            expect(isValid(new Date(response.data.admin.datetime))).to.be.true
        })
    })
})
