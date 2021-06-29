/* eslint-disable @typescript-eslint/no-explicit-any */
import { apollo } from '../../../support/apollo'
import { DocumentNode } from 'graphql'

describe('Test admin users endpont', () => {
    let GQL_USERS: DocumentNode
    let GQL_USERS_NO_FILTER: DocumentNode

    before('load graphql file', function () {
        GQL_USERS = require(`graphql-tag/loader!../../../fixtures/admin/users.graphql`)
        GQL_USERS_NO_FILTER = require(`graphql-tag/loader!../../../fixtures/admin/usersNoFilter.graphql`)
    })

    it('gets all users without any filtering', () => {
        cy.apolloQuery(apollo(Cypress.config().baseUrl, { username: 'root', password: 'root1234' }), {
            query: GQL_USERS_NO_FILTER,
        }).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.users.pageInfo.nodesCount).to.equal(8)
        })
    })

    it('gets users based on the name', () => {
        cy.apolloQuery(apollo(Cypress.config().baseUrl, { username: 'root', password: 'root1234' }), {
            query: GQL_USERS,
            variables: { limit: 5, offset: 0, field: 'username', value: 'jay' },
        }).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.users.pageInfo.nodesCount).to.equal(1)
            expect(response.data.admin.userAdmin.users.nodes[0].username).to.equal('jay')
            expect(response.data.admin.userAdmin.users.nodes[0].firstname).to.equal('Jay')
            expect(response.data.admin.userAdmin.users.nodes[0].lastname).to.equal('Hawking')
        })
    })

    it('gets users based on the site and limit them', () => {
        cy.apolloQuery(apollo(Cypress.config().baseUrl, { username: 'root', password: 'root1234' }), {
            query: GQL_USERS,
            variables: { limit: 5, offset: 0, field: 'site.name', value: 'systemsite' },
        }).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.users.pageInfo.nodesCount).to.equal(5)
        })
    })

    it('gets users based on the site and offset them', () => {
        cy.apolloQuery(apollo(Cypress.config().baseUrl, { username: 'root', password: 'root1234' }), {
            query: GQL_USERS,
            variables: { limit: 10, offset: 5, field: 'site.name', value: 'systemsite' },
        }).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.users.pageInfo.nodesCount).to.equal(3)
        })
    })

    it('gets users based on the site with no limit and no offset', () => {
        cy.apolloQuery(apollo(Cypress.config().baseUrl, { username: 'root', password: 'root1234' }), {
            query: GQL_USERS,
            variables: { limit: 10, offset: 0, field: 'site.name', value: 'systemsite' },
        }).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.users.pageInfo.nodesCount).to.equal(8)
        })
    })
})
