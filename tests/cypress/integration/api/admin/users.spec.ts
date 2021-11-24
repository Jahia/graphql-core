/* eslint-disable @typescript-eslint/no-explicit-any */

let remainingAttempts = 10

function waitUntilLdapRegistered() {
    cy.visit('/cms/adminframe/default/en/settings.manageUsers.html')
    const $items = Cypress.$('td:contains("Filibert Alfred")')
    if ($items.length) {
        return $items
    }

    if (--remainingAttempts) {
        cy.reload()
        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(1000).then(() => {
            waitUntilLdapRegistered()
        })
    } else {
        throw Error('LDAP users not found.')
    }
}

describe('Test admin users endpont', () => {
    before('load graphql file', () => {
        cy.runProvisioningScript({ fileName: 'admin/addLDAPConfigurationFile.json' })
        cy.login()
        waitUntilLdapRegistered()
    })

    it('gets all users without any filtering', () => {
        cy.apollo({
            queryFile: 'admin/usersNoFilter.graphql',
        }).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.users.pageInfo.nodesCount).to.equal(108)
        })
    })

    it('gets users based on the name', () => {
        cy.apollo({
            queryFile: 'admin/users.graphql',
            variables: { limit: 5, offset: 0, field: 'username', value: 'jay' },
        }).should((response: any) => {
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
        cy.apollo({
            queryFile: 'admin/users.graphql',
            variables: { limit: 20, offset: 0, field: 'site.name', value: 'systemsite' },
        }).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.users.pageInfo.nodesCount).to.equal(20)
        })
    })

    it('gets users based on the organization and limit them', () => {
        cy.apollo({
            queryFile: 'admin/users.graphql',
            variables: { limit: 15, offset: 0, field: 'organization', value: 'Product Development' },
        }).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.users.pageInfo.nodesCount).to.equal(13)
        })
    })

    it('gets users based on the site and offset them', () => {
        cy.apollo({
            queryFile: 'admin/users.graphql',
            variables: { limit: 10, offset: 100, field: 'site.name', value: 'systemsite' },
        }).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.users.pageInfo.nodesCount).to.equal(8)
        })
    })

    it('gets users based on the site with huge limit and no offset', () => {
        cy.apollo({
            queryFile: 'admin/users.graphql',
            variables: { limit: 1000, offset: 0, field: 'site.name', value: 'systemsite' },
        }).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.users.pageInfo.nodesCount).to.equal(108)
        })
    })
})
