/* eslint-disable @typescript-eslint/no-explicit-any */
import { DocumentNode } from 'graphql'
import { apollo } from '../../support/apollo'

describe('Validate ability get current User', () => {
    let GQL_APIUSER: DocumentNode

    before('load graphql file', function () {
        GQL_APIUSER = require(`graphql-tag/loader!../../fixtures/currentUser.graphql`)
    })

    it('Get Current user for Authenticated user (jay)', () => {
        cy.apolloQuery(apollo(Cypress.config().baseUrl, { username: 'jay', password: 'password' }), {
            query: GQL_APIUSER,
        }).should((response: any) => {
            expect(response.data.currentUser.name).to.equal('jay')
            expect(response.data.currentUser.username).to.equal('jay')
            expect(response.data.currentUser.firstname).to.equal('Jay')
            expect(response.data.currentUser.lastname).to.equal('Hawking')
            expect(response.data.currentUser.organization).to.be.empty
            expect(response.data.currentUser.language).to.equal('en')
            expect(response.data.currentUser.locked).to.equal(false)
            expect(response.data.currentUser.email).to.be.empty
        })
    })

    it('Get Current user for Authenticated user (root)', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_APIUSER,
            },
        ).should((response: any) => {
            expect(response.data.currentUser.name).to.equal('root')
        })
    })

    it('Get Current user for Authenticated user (root) with an incorrect password', () => {
        cy.apolloQuery(apollo(Cypress.config().baseUrl, { username: 'root', password: 'THIS-IS-INCORRECT' }), {
            query: GQL_APIUSER,
        }).should((response: any) => {
            expect(response.data.currentUser.name).to.equal('guest')
        })
    })

    it('Get Current user for Authenticated user (root) with an incorrect user', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'I-DO-NOT-EXIST', password: 'THIS-IS-INCORRECT' }),
            {
                query: GQL_APIUSER,
            },
        ).should((response: any) => {
            expect(response.data.currentUser.name).to.equal('guest')
        })
    })
})
