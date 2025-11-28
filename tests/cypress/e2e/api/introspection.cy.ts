/* eslint-disable @typescript-eslint/no-explicit-any */

import {addUserToGroup, createUser, deleteUser} from '@jahia/cypress';

describe('GraphQL Introspection Authorization', () => {
    const user = {
        username: 'introspectionUser',
        password: 'password'
    };

    before('Create test user', () => {
        // Create a regular user that can query but without introspection permission
        createUser(user.username, user.password);
        addUserToGroup(user.username, 'privileged');
    });

    after('Cleanup test user', () => {
        deleteUser(user.username);
    });

    it('Should allow introspection for user (root) with permission', () => {
        cy.apolloClient()
            .apollo({queryFile: 'introspectionSchema.graphql'})
            .should((response: any) => {
                expect(response.data.__type).to.not.be.null;
                expect(response.data.__type.name).to.equal('Query');
                expect(response.data.__type.kind).to.equal('OBJECT');
                expect(response.data.__type.fields).to.be.an('array');
                expect(response.data.__schema).to.not.be.null;
                expect(response.data.__schema.queryType).to.not.be.null;
                expect(response.data.__schema.queryType.name).to.equal('Query');
                expect(response.data.__schema.types).to.be.an('array');
                expect(response.data.__schema.types.length).to.be.greaterThan(0);
            });
    });

    it('Should allow regular queries for user without introspection permission', () => {
        // Verify that disabling introspection doesn't affect regular queries
        cy.apolloClient(user)
            .apollo({queryFile: 'currentUser.graphql'})
            .should((response: any) => {
                expect(response.data.currentUser).to.not.be.null;
                expect(response.data.currentUser.username).to.equal(user.username);
            });
    });

    // `introspectionCheckEnabled=true` is set in jahia.properties on docker-compose
    it('Should block __schema introspection for regular user without permission', () => {
        cy.apolloClient(user)
            .apollo({queryFile: 'introspectionSchema.graphql', errorPolicy: 'all'})
            .should((response: any) => {
                // When introspection is disabled, the whole graphql query is rejected
                expect(response.data).to.be.null;
                // May also have errors depending on GraphQL Java version
                if (response.errors) {
                    expect(response.errors).to.have.length.greaterThan(0);
                    expect(response.errors[0].message).to.equal('Introspection has been disabled for this request');
                }
            });
    });
});
