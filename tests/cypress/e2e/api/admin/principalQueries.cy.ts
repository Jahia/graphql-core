import {createSite, createUser, deleteSite, deleteUser} from '@jahia/cypress';
import {isValid} from 'date-fns';
import {grantUserRole} from '../../../fixtures/acl';

/**
 * The contract of the principal endpoints under `admin`: the permission they require, and the property
 * names the principals they return publish.
 *
 * Both sides are covered, so an endpoint wired to refuse every caller would satisfy no more of this
 * suite than one that refuses none. The site editor is a privileged user whose session reaches
 * `admin`, and the first check of its group asserts exactly that — so what the checks after it read is
 * each endpoint's own permission, and not the session, the scope, or the gate on `admin` itself.
 */
describe('admin principal queries', () => {
    const siteKey = 'principalQueryTestSite';
    const siteEditor = 'principalQueryEditor';
    const subject = 'principalQuerySubject';
    const organization = 'Jahia Solutions';
    const password = 'password';

    before('Create a site, a site editor and a user to read', () => {
        createSite(siteKey);
        createUser(siteEditor, password);
        grantUserRole(`/sites/${siteKey}`, 'editor', siteEditor);
        createUser(subject, password, [{name: 'j:organization', value: organization}]);
    });

    after('Remove test data', () => {
        deleteSite(siteKey);
        deleteUser(siteEditor);
        deleteUser(subject);
    });

    describe('a caller holding the admin query permission', () => {
        it('lists users', () => {
            cy.apollo({queryFile: 'admin/usersNoFilter.graphql'}).should((response: any) => {
                expect(response.data.admin.userAdmin.users.pageInfo.nodesCount).to.be.greaterThan(0);
            });
        });

        it('reads a group and its members', () => {
            cy.apollo({
                queryFile: 'admin/group.graphql',
                variables: {groupName: 'administrators', site: null}
            }).should((response: any) => {
                expect(response.data.admin.userGroup.group.members.nodes).to.not.be.empty;
            });
        });

        it('reads the property names a user publishes, and those alone', () => {
            cy.apollo({
                queryFile: 'admin/userProperties.graphql',
                variables: {username: subject}
            }).should((response: any) => {
                const user = response.data.admin.userAdmin.user;
                expect(user.username, 'the queried user must be resolved for this check to mean anything').to.equal(subject);
                expect(user.published).to.equal(organization);
                expect(user.withheld).to.be.null;
            });
        });
    });

    describe('a caller without the admin query permission', () => {
        const asSiteEditor = () => cy.apolloClient({username: siteEditor, password});

        it('reaches the admin query root', () => {
            asSiteEditor()
                .apollo({queryFile: 'admin/datetime.graphql'})
                .should((response: any) => {
                    expect(isValid(new Date(response.data.admin.datetime))).to.be.true;
                });
        });

        it('does not reach the user administration endpoint', () => {
            asSiteEditor()
                .apollo({queryFile: 'admin/usersNoFilter.graphql', errorPolicy: 'all'})
                .should((response: any) => {
                    expect(response.errors, 'should contain a permission error').to.exist.and.not.be.empty;
                    expect(response.data?.admin?.userAdmin).to.not.exist;
                });
        });

        it('does not reach the user group endpoint', () => {
            asSiteEditor()
                .apollo({
                    queryFile: 'admin/group.graphql',
                    variables: {groupName: 'administrators', site: null},
                    errorPolicy: 'all'
                })
                .should((response: any) => {
                    expect(response.errors, 'should contain a permission error').to.exist.and.not.be.empty;
                    expect(response.data?.admin?.userGroup).to.not.exist;
                });
        });
    });
});
