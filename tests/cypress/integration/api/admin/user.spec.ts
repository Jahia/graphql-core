/* eslint-disable @typescript-eslint/no-explicit-any */

describe('Test admin user endpoint', () => {
    it('gets a user', () => {
        cy.apollo({
            queryFile: 'admin/user.graphql',
            variables: { username: 'irina', group: '' },
        }).should((response: any) => {
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
        cy.apollo({
            queryFile: 'admin/user.graphql',
            variables: { username: 'noob', group: '' },
        }).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user).to.be.null
        })
    })

    it('gets a user name', () => {
        cy.apollo({
            queryFile: 'admin/user.graphql',
            variables: { username: 'bill', group: '' },
        }).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user.username).to.equal('bill')
            expect(response.data.admin.userAdmin.user.displayName).to.equal('Bill Galileo')
        })
    })

    it('gets a user name', () => {
        cy.apollo({
            queryFile: 'admin/user.graphql',
            variables: { username: 'bill', group: 'site-administrators', site1: 'digitall', site2: 'systemsite' },
        }).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user.yes).to.equal(true)
            expect(response.data.admin.userAdmin.user.no).to.equal(false)
        })
    })

    it('tests membership list', () => {
        cy.apollo({
            queryFile: 'admin/userGroupMembershipBasic.graphql',
            variables: { username: 'bill', site: 'digitall' },
        }).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user.groupMembership.pageInfo.totalCount).to.be.greaterThan(2)
            expect(response.data.admin.userAdmin.user.groupMembership.nodes.map((n) => n.name)).to.contains(
                'site-administrators',
            )
        })
    })

    it('tests membership list for a site', () => {
        cy.apollo({
            queryFile: 'admin/userGroupMembershipBasic.graphql',
            variables: { username: 'bill', site: 'digitall' },
        }).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user.groupMembership.pageInfo.totalCount).to.equal(3)
        })
    })

    it('tests membership list with filter', () => {
        cy.apollo({
            queryFile: 'admin/userGroupMembershipFilter.graphql',
            variables: { username: 'bill', field: 'site.name', value: 'digitall' },
        }).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user.groupMembership.pageInfo.totalCount).to.equal(3)
        })
    })

    it('tests members list', () => {
        cy.apollo({
            queryFile: 'admin/group.graphql',
            variables: { groupName: 'site-administrators', site: 'digitall' },
        }).should((response: any) => {
            expect(response.data.admin.userGroup).to.exist
            expect(response.data.admin.userGroup.group.members.nodes.map((n) => n.name)).to.contains('bill')
        })
    })
})
