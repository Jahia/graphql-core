/* eslint-disable @typescript-eslint/no-explicit-any */
import gql from 'graphql-tag'
import { apollo } from '../../../support/apollo'
import { DocumentNode } from 'graphql'

describe('Test admin user endpoint', () => {
    let GQL_USER: DocumentNode
    let GQL_USER_GROUPMEMBERSHIP_FILTER: DocumentNode
    let GQL_USER_GROUPMEMBERSHIP_BASIC: DocumentNode
    let GQL_GROUP: DocumentNode

    before('load graphql file', function () {
        GQL_USER = require(`graphql-tag/loader!../../../fixtures/admin/user.graphql`)
        GQL_GROUP = require(`graphql-tag/loader!../../../fixtures/admin/group.graphql`)
        GQL_USER_GROUPMEMBERSHIP_FILTER = require(`graphql-tag/loader!../../../fixtures/admin/userGroupMembershipFilter.graphql`)
        GQL_USER_GROUPMEMBERSHIP_BASIC = require(`graphql-tag/loader!../../../fixtures/admin/userGroupMembershipBasic.graphql`)
    })

    it('gets a user', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_USER,
                variables: { userName: 'irina' },
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user.name).to.equal('irina')
            expect(response.data.admin.userAdmin.user.username).to.equal('irina')
            expect(response.data.admin.userAdmin.user.firstname).to.equal('Irina')
            expect(response.data.admin.userAdmin.user.lastname).to.equal('Pasteur')
            expect(response.data.admin.userAdmin.user.organization).not.to.be.undefined
            expect(response.data.admin.userAdmin.user.language).to.equal('en')
            expect(response.data.admin.userAdmin.user.locked).to.equal(false)
            expect(response.data.admin.userAdmin.user.email).not.to.be.undefined
        })
    })

    it('gets a non existing user', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_USER,
                variables: { userName: 'noob' },
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user).to.be.null
        })
    })

    it('gets a user name', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_USER,
                variables: { userName: 'bill' },
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user.username).to.equal('bill')
            expect(response.data.admin.userAdmin.user.displayName).to.equal('Bill Galileo')
        })
    })

    it('tests membership', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_USER,
                variables: { userName: 'bill', group: 'site-administrators', site1: 'digitall', site2: 'systemsite' },
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user.yes).to.equal(true)
            expect(response.data.admin.userAdmin.user.no).to.equal(false)
        })
    })

    it('tests membership list', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_USER_GROUPMEMBERSHIP_BASIC,
                variables: { userName: 'bill', site: 'digitall' },
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user.groupMembership.pageInfo.totalCount).to.be.greaterThan(2)
            expect(response.data.admin.userAdmin.user.groupMembership.nodes.map((n) => n.name)).to.contains(
                'site-administrators',
            )
        })
    })

    it('tests membership list for a site', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_USER_GROUPMEMBERSHIP_BASIC,
                variables: { userName: 'bill', site: 'digitall' },
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user.groupMembership.pageInfo.totalCount).to.equal(3)
        })
    })

    it('tests membership list with filter', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_USER_GROUPMEMBERSHIP_FILTER,
                variables: { userName: 'bill', field: 'site.name', value: 'digitall' },
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user.groupMembership.pageInfo.totalCount).to.equal(3)
        })
    })

    it('tests members list', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_GROUP,
                variables: { groupName: 'site-administrators', site: 'digitall' },
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.group.members.nodes.map((n) => n.name)).to.contains('bill')
        })
    })
})
