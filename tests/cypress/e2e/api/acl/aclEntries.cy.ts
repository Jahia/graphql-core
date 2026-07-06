
import {getAclEntries} from '../../../fixtures/acl';
import {getJahiaVersion} from '@jahia/cypress';
import {compare} from 'compare-versions';

describe('Test ACL/ACE query endpoint', () => {
    const parentPath = '/sites/digitall/files/images';
    const path = `${parentPath}/placeholder.jpg`;

    it('Get ACL for node', () => {
        getAclEntries(path).should(resp => {
            const acl = resp?.data?.jcr?.nodeByPath?.acl;
            expect(acl).to.exist;
            expect(acl.aclEntries).to.be.not.empty;
            expect(
                acl.aclEntries.filter(a => !a.inherited),
                'All inherited roles'
            ).to.be.empty;
        });
    });

    it(`Mathias has editor role for ${path}`, () => {
        getAclEntries(path).should(resp => {
            const acl = resp?.data?.jcr?.nodeByPath?.acl;
            const userRole = getRole(acl.aclEntries, 'mathias', 'editor');
            expect(userRole, `Mathias has editor role for ${path}`).to.be.not.undefined;
            expect(userRole.inherited).to.be.true;
            expect(userRole.inheritedFrom.path).equals('/sites/digitall');
        });
    });

    it(`Anne has editor-in-chief role for ${path}`, () => {
        getAclEntries(path).should(resp => {
            const acl = resp?.data?.jcr?.nodeByPath?.acl;
            const userRole = getRole(acl.aclEntries, 'anne', 'editor-in-chief');
            expect(userRole, `Anne has editor-in-chief role for ${path}`).to.be.not.undefined;
            expect(userRole.inherited).to.be.true;
            expect(userRole.inheritedFrom.path).equals('/sites/digitall');
        });
    });

    it(`Guest user has reader role for ${path}`, () => {
        getAclEntries(path).then(resp => {
            const acl = resp?.data?.jcr?.nodeByPath?.acl;
            const aclEntry = getRole(acl.aclEntries, 'guest', 'reader');
            expect(aclEntry, `Guest user has reader role for ${path}`).to.be.not.undefined;
            expect(aclEntry.inherited).to.be.true;

            /* Inheritance from /sites only supported >= 8.2.4.0.
             * Before, it was defined on root node `/`
             * https://github.com/Jahia/jahia-private/pull/4893 */
            getJahiaVersion().then(jahiaVersion => {
                const isSupported = compare(jahiaVersion.release.replace('-SNAPSHOT', ''), '8.2.4', '>=');
                const expectedValue = isSupported ? '/sites' : '/';
                expect(aclEntry.inheritedFrom.path, `ACL entry should be inherited from ${expectedValue}`).equals(expectedValue);
            });
        });
    });

    it('Get ACL for specific for user irina', () => {
        const principalFilter = {type: 'USER', name: 'irina'};
        getAclEntries(path, principalFilter).should(resp => {
            const acl = resp?.data?.jcr?.nodeByPath?.acl;
            expect(acl).to.exist;
            expect(acl.aclEntries).to.be.not.empty;
            const aclEntry = getRole(acl.aclEntries, 'irina', 'reviewer');
            expect(aclEntry, `Irina has reviewer role for ${path}`).to.be.not.undefined;
            expect(aclEntry.inherited).to.be.true;
            expect(aclEntry.inheritedFrom.path).equals('/sites/digitall');
        });
    });

    it('Get ACL for unspecified user', () => {
        const principalFilter = {type: 'USER', name: 'not-exist-user'};
        getAclEntries(path, principalFilter).should(resp => {
            const acl = resp?.data?.jcr?.nodeByPath?.acl;
            expect(acl).to.exist;
            expect(acl.aclEntries).to.be.empty;
        });
    });

    it('Get ACL for specific global group', () => {
        const principalFilter = {type: 'GROUP', name: 'users'};
        getAclEntries(path, principalFilter).should(resp => {
            const acl = resp?.data?.jcr?.nodeByPath?.acl;
            expect(acl).to.exist;
            expect(acl.aclEntries).to.be.not.empty;
            const aclEntry = getRole(acl.aclEntries, 'users', 'reader');
            expect(aclEntry, `'users' group has a reader role for ${path}`).to.be.not.undefined;
            expect(aclEntry.inherited).to.be.true;
            expect(aclEntry.inheritedFrom.path, '\'users\' group is defined globally').equals('/');
        });
    });

    function getRole(ace, principalName, roleName) {
        return ace.find(
            a => a.principal?.name === principalName && a.aclEntryType === 'GRANT' && a.role.name === roleName
        );
    }
});
