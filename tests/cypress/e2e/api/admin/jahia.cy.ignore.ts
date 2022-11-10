/* eslint-disable @typescript-eslint/no-explicit-any */
import { DocumentNode } from 'graphql'
import { isValid } from 'date-fns'
import { apollo } from '../../../support/apollo'

describe('admin.jahia - Jahia Server details', () => {
    let GQL_QUERY: DocumentNode

    before('load graphql file', function () {
        GQL_QUERY = require(`graphql-tag/loader!../../../fixtures/admin/jahia.graphql`)
    })

    // Add check to ensure guest user cannot get server details
    it('Verify guest user cannot get any data on admin.jahia', () => {
        cy.task('apolloNode', {
            baseUrl: Cypress.config().baseUrl,
            authMethod: { username: 'guest', password: 'I-DO-NOT-EXIST' },
            query: GQL_QUERY,
        }).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.currentUser.name).to.equal('guest')
            // VERIFY GUEST CANNOT ACCESS NODE DETAILS
        })
    })

    it('Build number validation', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_QUERY,
            },
        ).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.currentUser.name).to.equal('root')
            // expect(response.data.admin.jahia.build.length).to.equal(7)
            expect(response.data.admin.jahia.build.match(/^[a-f0-9]+$/).length).to.be.greaterThan(0)
        })
    })

    it('Build date validation', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_QUERY,
            },
        ).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.currentUser.name).to.equal('root')
            expect(isValid(new Date(response.data.admin.jahia.buildDate))).to.be.true
            //Verify jahia was build at an earlier date than current date
            expect(new Date()).to.be.greaterThan(new Date(response.data.admin.jahia.buildDate))
        })
    })

    it('isSnapshot validation', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_QUERY,
            },
        ).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.currentUser.name).to.equal('root')

            // If Snapshot is present in the title, then the isSnapshot boolean should be true
            expect(response.data.admin.jahia.isSnapshot).to.equal(
                response.data.admin.jahia.release.includes('SNAPSHOT'),
            )
        })
    })

    it('Release validation', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_QUERY,
            },
        ).then((response: any) => {
            cy.log(JSON.stringify(response))
            expect(response.data.currentUser.name).to.equal('root')
            expect(parseInt(response.data.admin.jahia.build.replace(/[^0-9]/g, ''))).to.be.greaterThan(8000)
        })
    })
})
