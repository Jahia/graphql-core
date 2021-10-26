/* eslint-disable @typescript-eslint/no-explicit-any */
import { apollo } from '../../../support/apollo'
import { DocumentNode } from 'graphql'

describe('Test admin users endpont', () => {
    let GQL_USERS: DocumentNode
    let GQL_USERS_NO_FILTER: DocumentNode
    let LDAP_CONFIG

    before('load graphql file', function () {
        GQL_USERS = require(`graphql-tag/loader!../../../fixtures/admin/users.graphql`)
        GQL_USERS_NO_FILTER = require(`graphql-tag/loader!../../../fixtures/admin/usersNoFilter.graphql`)
        LDAP_CONFIG = require('../../../fixtures/admin/addLDAPConfigurationFile.json')
        cy.runProvisioningScript(LDAP_CONFIG)

        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(2000)
    })

    it('gets all users without any filtering', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_USERS_NO_FILTER,
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.users.pageInfo.nodesCount).to.equal(108)
        })
    })

    it('gets users based on the name', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_USERS,
                variables: { limit: 5, offset: 0, field: 'username', value: 'jay' },
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.users.pageInfo.nodesCount).to.equal(1)
            expect(response.data.admin.userAdmin.users.nodes[0].username).to.equal('jay')
            expect(response.data.admin.userAdmin.users.nodes[0].firstname).to.equal('Jay')
            expect(response.data.admin.userAdmin.users.nodes[0].lastname).to.equal('Hawking')
            expect(response.data.admin.userAdmin.users.nodes[0].organization).to.exist
            expect(response.data.admin.userAdmin.users.nodes[0].locked).to.exist
        })
    })

    it('gets users based on the site and limit them', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_USERS,
                variables: { limit: 20, offset: 0, field: 'site.name', value: 'systemsite' },
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.users.pageInfo.nodesCount).to.equal(20)
        })
    })

    it('gets users based on the organization and limit them', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_USERS,
                variables: { limit: 15, offset: 0, field: 'organization', value: 'Product Development' },
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.users.pageInfo.nodesCount).to.equal(13)
        })
    })

    it('gets users based on the site and offset them', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_USERS,
                variables: { limit: 10, offset: 100, field: 'site.name', value: 'systemsite' },
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.users.pageInfo.nodesCount).to.equal(8)
        })
    })

    it('gets users based on the site with huge limit and no offset', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_USERS,
                variables: { limit: 1000, offset: 0, field: 'site.name', value: 'systemsite' },
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.users.pageInfo.nodesCount).to.equal(108)
        })
    })
})
