/* eslint-disable @typescript-eslint/no-explicit-any */
import { grantUserRole, revokeUserRole } from '../../../fixtures/acl'
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
        cy.apollo({
            queryFile: 'acl/getAceNode.graphql',
            variables: { pathOrId },
        }).should((resp) => {
            const acls = resp?.data?.jcr?.nodeByPath?.descendants?.nodes || []
            const userAcl = acls.find((a) => a.principal.value === `u:${user}`)
            expect(userAcl, `Found ACE for user ${user}`).to.be.not.undefined
            expect(userAcl.roles.values).to.contain(role, 'ACE has ${role} role')
            expect(userAcl.aceType.value).equals('GRANT', 'ACE has GRANT permission ${role} role')
        })
    })

    it('remove role on revokeRole operation that has been added on node', () => {
        const role = 'editor'
        const user = 'jay'

        cy.log('Calling revokeRoles graphql api')
        revokeUserRole(pathOrId, role, user)

        cy.log(`Verify role '${role}' has been removed from node ${pathOrId}`)
        cy.apollo({
            queryFile: 'acl/getAceNode.graphql',
            variables: { pathOrId },
        }).should((resp) => {
            const acls = resp?.data?.jcr?.nodeByPath?.descendants?.nodes || []
            const userAcl = acls.find((a) => a.principal.value === `u:${user}`)
            expect(userAcl, `ACE for user ${user} has been removed on node ${pathOrId}`).to.be.undefined
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
        cy.apollo({
            queryFile: 'acl/getAceNode.graphql',
            variables: { pathOrId },
        }).should((resp) => {
            const acls = resp?.data?.jcr?.nodeByPath?.descendants?.nodes || []
            const userAcl = acls.find((a) => a.principal.value === `u:${user}`)
            expect(userAcl, `ACE for user ${user} has been removed`).to.be.not.undefined
            expect(userAcl.roles.values).to.contain(role, 'ACE has ${role} role')
            expect(userAcl.aceType.value).equals('DENY', 'ACE has DENY permission ${role} role')
        })
    })

    it('remove DENY permission on grantRole operation if user has inherited role', () => {
        const role = 'editor-in-chief'
        const user = 'jay'

        cy.log('Grant role on a child node')
        grantUserRole(pathOrId, role, user)

        cy.log(`Verify DENY permission on role '${role}' has been removed on node ${pathOrId}`)
        cy.apollo({
            queryFile: 'acl/getAceNode.graphql',
            variables: { pathOrId },
        }).should((resp) => {
            const acls = resp?.data?.jcr?.nodeByPath?.descendants?.nodes || []
            const userAcl = acls.find((a) => a.principal.value === `u:${user}`)
            expect(userAcl, `ACE for user ${user} has been removed`).to.be.undefined
        })
    })

    it('do nothing on grantRole operation if user has inherited role', () => {
        const role = 'editor-in-chief'
        const user = 'jay'

        cy.log('Grant role on a child node')
        grantUserRole(pathOrId, role, user)

        cy.log(`Verify GRANT role '${role}' not added on node ${pathOrId}`)
        cy.apollo({
            queryFile: 'acl/getAceNode.graphql',
            variables: { pathOrId },
        }).should((resp) => {
            const acls = resp?.data?.jcr?.nodeByPath?.descendants?.nodes || []
            const userAcl = acls.find((a) => a.principal.value === `u:${user}`)
            expect(userAcl, `No grant role added for user ${user}`).to.be.undefined
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
