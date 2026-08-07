import {addUserToGroup, createUser, deleteUser} from '@jahia/cypress';
import {getGqlConfig} from '../../fixtures/admin/configuration';

// Regression: introspection access should be restricted out of the box. The shipped
// default configuration now sets `introspectionCheckEnabled=true`, so a privileged user
// who does not hold the `developerToolsAccess` permission cannot run schema introspection
// without any explicit configuration. (The toggled behaviour is covered separately by
// introspection.cy.ts; this spec guards the shipped default itself.)
describe('GraphQL introspection - secure default', () => {
    const user = {
        username: 'introspectionDefaultUser',
        password: 'password'
    };

    before('Create a privileged user without the developer-tools permission', () => {
        createUser(user.username, user.password);
        addUserToGroup(user.username, 'privileged');
    });

    after('Cleanup test user', () => {
        deleteUser(user.username);
    });

    it('ships introspectionCheckEnabled=true in the default configuration', () => {
        getGqlConfig('introspectionCheckEnabled').should((value: string) => {
            expect(value).to.equal('true');
        });
    });

    it('blocks schema introspection for a privileged user without the permission, with no explicit configuration', () => {
        cy.apolloClient(user)
            .apollo({queryFile: 'introspectionSchema.graphql', errorPolicy: 'all'})
            .should((response: any) => {
                // When introspection is disabled the whole query is rejected.
                expect(response.data).to.be.null;
                if (response.errors) {
                    expect(response.errors).to.have.length.greaterThan(0);
                    expect(response.errors[0].message).to.equal('Introspection has been disabled for this request');
                }
            });
    });

    it('still allows regular queries for that user', () => {
        cy.apolloClient(user)
            .apollo({queryFile: 'currentUser.graphql'})
            .should((response: any) => {
                expect(response.data.currentUser).to.not.be.null;
                expect(response.data.currentUser.username).to.equal(user.username);
            });
    });
});
