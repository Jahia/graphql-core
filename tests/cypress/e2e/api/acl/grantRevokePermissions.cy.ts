import {createSite, createUser, deleteSite, deleteUser} from '@jahia/cypress';
import {grantUserRole} from '../../../fixtures/acl';

describe('roles API permissions test', () => {
    const adminRoleUser = {username: 'adminRoleUser', password: 'password'};
    const editorUser = {username: 'editorUser', password: 'password'};
    const testUser = {username: 'testUser', password: 'password'};
    const siteKey = 'roleSite';

    before(() => {
        createSite(siteKey);

        // Set up users
        [adminRoleUser, editorUser, testUser]
            .forEach(({username, password}) => createUser(username, password));
        grantUserRole(`/sites/${siteKey}`, 'editor', editorUser.username);
        // Server-administrator role has required adminRoles permission enabled
        grantUserRole('/', 'server-administrator', adminRoleUser.username);
    });

    after(() => {
        [adminRoleUser, editorUser, testUser]
            .forEach(({username}) => deleteUser(username));
        deleteSite(siteKey);
    });

    const rolesApiCall = (apiUser, mutationFile, variables) => {
        return cy.apolloClient(apiUser)
            .apollo({mutationFile, variables});
    };

    it('checks proper permission to grant/revoke roles', () => {
        const gqlParams = {
            pathOrId: `/sites/${siteKey}/files`,
            roles: ['editor'],
            pType: 'USER',
            pName: testUser.username
        };

        cy.log('Grant user should fail without proper permission');
        rolesApiCall(editorUser, 'acl/grantRoles.graphql', gqlParams)
            .should(resp => {
                expect(resp.graphQLErrors, 'Errors exist').to.have.length(1);
                expect(resp.graphQLErrors[0].errorType, 'Grant role request denied').to.equal('GqlAccessDeniedException');
            });

        cy.log('Grant user should succeed with adminRoles permission');
        rolesApiCall(adminRoleUser, 'acl/grantRoles.graphql', gqlParams)
            .should(resp => {
                expect(resp?.data?.jcr?.mutateNode?.grantRoles, 'Grant role request OK').to.be.true;
            });

        cy.log('Revoke user should fail without proper permission');
        rolesApiCall(editorUser, 'acl/revokeRoles.graphql', gqlParams).should(resp => {
            expect(resp.graphQLErrors, 'Errors exist').to.have.length(1);
            expect(resp.graphQLErrors[0].errorType, 'Grant role request denied').to.equal(
                'GqlAccessDeniedException'
            );
        });

        cy.log('Revoke user should succeed with adminRoles permission');
        rolesApiCall(adminRoleUser, 'acl/revokeRoles.graphql', gqlParams).should(resp => {
            expect(resp?.data?.jcr?.mutateNode?.revokeRoles, 'Grant role request OK').to.be.true;
        });
    });
});
