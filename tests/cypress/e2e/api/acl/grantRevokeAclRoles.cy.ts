/* eslint-disable @typescript-eslint/no-explicit-any */
import { getAclEntries, grantUserRole, revokeUserRole } from '../../../fixtures/acl'
import { validateError } from '../jcr/validateErrors'

describe('Test grant/revoke role mutation on node endpoint', () => {
    const parentPath = '/sites/digitall/files/images'
    const pathOrId = `${parentPath}/placeholder.jpg`

    it('grant role on node', () => {
        const role = 'editor'
        const user = 'jay'

        cy.log('Calling grantRoles graphql api')
        grantUserRole(pathOrId, role, user)

        cy.log(`Verify role '${role}' has been added to node ${pathOrId}`)
        getAclEntries(pathOrId, { type: 'USER', name: user }, false).should((resp) => {
            const aclEntries = resp?.data?.jcr?.nodeByPath?.acl.aclEntries
            const aclEntry = aclEntries.find((ace) => ace.aclEntryType === 'GRANT' && ace.role.name === role)
            expect(aclEntry, `ACE has GRANT permission ${role} role`).to.be.not.undefined
            expect(aclEntry.inherited).to.be.false
            expect(aclEntry.inheritedFrom.path).equals(pathOrId)
        })
    })

    it('remove role on revokeRole operation that has been added on node', () => {
        const role = 'editor'
        const user = 'jay'

        cy.log('Calling revokeRoles graphql api')
        revokeUserRole(pathOrId, role, user)

        cy.log(`Verify role '${role}' has been removed from node ${pathOrId}`)
        getAclEntries(pathOrId, { type: 'USER', name: user }, false).should((resp) => {
            const aclEntries = resp?.data?.jcr?.nodeByPath?.acl.aclEntries
            const aclEntry = aclEntries.find((ace) => ace.role.name === role)
            expect(aclEntry, `editor role for user ${user} has been removed on node ${pathOrId}`).to.be.undefined
        })
    })

    it('add DENY on revokeRole operation if node has inherited permissions', () => {
        const role = 'editor-in-chief'
        const user = 'jay'

        cy.log('Grant role on parent node')
        grantUserRole(parentPath, role, user)

        cy.log('Revoke role on a child node')
        revokeUserRole(pathOrId, role, user)

        cy.log(`Verify role '${role}' has deny permission on node ${pathOrId}`)
        getAclEntries(pathOrId, { type: 'USER', name: user }, false).should((resp) => {
            const aclEntries = resp?.data?.jcr?.nodeByPath?.acl.aclEntries
            const aclEntry = aclEntries.find((ace) => ace.aclEntryType === 'DENY' && ace.role.name === role)
            expect(aclEntry, `ACE has DENY permission on ${role} role`).to.be.not.undefined
            expect(aclEntry.inherited).to.be.false
            expect(aclEntry.inheritedFrom.path).equals(pathOrId)
        })
    })

    it('remove DENY permission on grantRole operation if user has inherited role', () => {
        const role = 'editor-in-chief'
        const user = 'jay'

        cy.log('Grant role on a child node')
        grantUserRole(pathOrId, role, user)

        cy.log(`Verify DENY permission on role '${role}' has been removed on node ${pathOrId}`)
        getAclEntries(pathOrId, { type: 'USER', name: user }, false).should((resp) => {
            const aclEntries = resp?.data?.jcr?.nodeByPath?.acl.aclEntries
            const aclEntry = aclEntries.find((ace) => ace.role.name === role)
            expect(aclEntry, `DENY for user ${user} has been removed on node ${pathOrId}`).to.be.undefined
        })

        cy.log(`Verify inherited role '${role}' still exists for node ${pathOrId}`)
        getAclEntries(pathOrId, { type: 'USER', name: user }, true).should((resp) => {
            const aclEntries = resp?.data?.jcr?.nodeByPath?.acl.aclEntries
            const aclEntry = aclEntries.find((ace) => ace.role.name === role && ace.aclEntryType === 'GRANT')
            expect(aclEntry, `user ${user} still has inherited role on node ${pathOrId}`).to.be.not.undefined
            expect(aclEntry.inherited).to.be.true
            expect(aclEntry.inheritedFrom.path).equals(parentPath)
        })
    })

    it('do nothing on grantRole operation if user has inherited role', () => {
        const role = 'editor-in-chief'
        const user = 'jay'

        cy.log('Grant role on a child node')
        grantUserRole(pathOrId, role, user)

        cy.log(`Verify GRANT role '${role}' not added on node ${pathOrId}`)
        getAclEntries(pathOrId, { type: 'USER', name: user }, false).should((resp) => {
            const aclEntries = resp?.data?.jcr?.nodeByPath?.acl.aclEntries
            expect(aclEntries, `No GRANT added for user ${user} on node ${pathOrId}`).to.be.empty
        })

        cy.log(`Verify inherited role '${role}' still exists for node ${pathOrId}`)
        getAclEntries(pathOrId, { type: 'USER', name: user }, true).should((resp) => {
            const aclEntries = resp?.data?.jcr?.nodeByPath?.acl.aclEntries
            const aclEntry = aclEntries.find((ace) => ace.role.name === role && ace.aclEntryType === 'GRANT')
            expect(aclEntry, `user ${user} still has inherited role on node ${pathOrId}`).to.be.not.undefined
            expect(aclEntry.inherited).to.be.true
            expect(aclEntry.inheritedFrom.path).equals(parentPath)
        })
    })

    it('throws exception when setting role with invalid user', () => {
        cy.apollo({
            mutationFile: 'acl/grantRoles.graphql',
            variables: { pathOrId, roles: ['editor'], pType: 'USER', pName: 'invalidUser' },
            errorPolicy: 'all',
        }).should((resp) => {
            validateError(resp, 'Internal Server Error(s) while executing query')
            expect(resp?.data?.jcr?.mutateNode?.grantRoles, 'Grant role operation should fail').to.be.null
        })
    })

    it('revoke GRANT permission on parent node', () => {
        revokeUserRole(parentPath, 'editor-in-chief', 'jay')
    })
})
