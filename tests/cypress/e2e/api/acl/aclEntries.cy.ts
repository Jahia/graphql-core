/* eslint-disable @typescript-eslint/no-explicit-any */

import {getAclEntries} from "../../../fixtures/acl";

describe('Test ACL/ACE query endpoint', () => {

    const parentPath = '/sites/digitall/files/images'
    const path = `${parentPath}/placeholder.jpg`

    it('Get ACL for node', () => {
        getAclEntries(path).should(resp => {
            const acl = resp?.data?.jcr?.nodeByPath?.acl
            expect(acl).to.exist
            expect(acl.aclEntries).to.be.not.empty
            expect(acl.aclEntries.filter(a => !a.inherited), "All inherited roles").to.be.empty
        })
    })

    it(`Mathias has editor role for ${path}`, () => {
        getAclEntries(path).should(resp => {
            const acl = resp?.data?.jcr?.nodeByPath?.acl
            let userRole = getRole(acl.aclEntries, 'mathias', 'editor')
            expect(userRole, `Mathias has editor role for ${path}`).to.be.not.undefined
            expect(userRole.inherited).to.be.true
            expect(userRole.inheritedFrom.path).equals('/sites/digitall')
        })
    })

    it(`Anne has editor-in-chief role for ${path}`, () => {
        getAclEntries(path).should(resp => {
            const acl = resp?.data?.jcr?.nodeByPath?.acl
            let userRole = getRole(acl.aclEntries, 'anne', 'editor-in-chief')
            expect(userRole, `Anne has editor-in-chief role for ${path}`).to.be.not.undefined
            expect(userRole.inherited).to.be.true
            expect(userRole.inheritedFrom.path).equals('/sites/digitall')
        })
    })

    it(`Guest user has reader role for ${path}`, () => {
        getAclEntries(path).should(resp => {
            const acl = resp?.data?.jcr?.nodeByPath?.acl
            let aclEntry = getRole(acl.aclEntries, 'guest', 'reader')
            expect(aclEntry, `Anne has editor-in-chief role for ${path}`).to.be.not.undefined
            expect(aclEntry.inherited).to.be.true
            expect(aclEntry.inheritedFrom.path).equals('/')
        })
    })

    it('Get ACL for specific for user irina', () => {
        const principalFilter = {type: 'USER', name: 'irina'}
        getAclEntries(path, principalFilter).should(resp => {
            const acl = resp?.data?.jcr?.nodeByPath?.acl
            expect(acl).to.exist
            expect(acl.aclEntries).to.be.not.empty
            let aclEntry = getRole(acl.aclEntries, 'irina', 'reviewer')
            expect(aclEntry, `Irina has reviewer role for ${path}`).to.be.not.undefined
            expect(aclEntry.inherited).to.be.true
            expect(aclEntry.inheritedFrom.path).equals('/sites/digitall')
        })
    })

    it('Get ACL for unspecified user', () => {
        const principalFilter = {type: 'USER', name: 'not-exist-user'}
        getAclEntries(path, principalFilter).should(resp => {
            const acl = resp?.data?.jcr?.nodeByPath?.acl
            expect(acl).to.exist
            expect(acl.aclEntries).to.be.empty
        })
    })

    function getRole(ace, userName, roleName) {
        return ace.find(a => a.principal?.name === userName && a.aclEntryType === 'GRANT' && a.role.name === roleName)
    }
})
