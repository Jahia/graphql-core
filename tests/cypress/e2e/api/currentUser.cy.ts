/* eslint-disable @typescript-eslint/no-explicit-any */

describe('Validate ability get current User', () => {
    it('Get Current user for Authenticated user (irina)', () => {
        cy.apolloClient({username: 'irina', password: 'password'})
            .apollo({queryFile: 'currentUser.graphql'})
            .should((response: any) => {
                expect(response.data.currentUser.name).to.equal('irina');
                expect(response.data.currentUser.username).to.equal('irina');
                expect(response.data.currentUser.firstname).to.equal('Irina');
                expect(response.data.currentUser.lastname).to.equal('Pasteur');
                expect(response.data.currentUser.organization).to.equal('Acme Space');
                expect(response.data.currentUser.language).to.equal('en');
                expect(response.data.currentUser.locked).to.equal(false);
                expect(response.data.currentUser.email).to.be.empty;
            });
    });

    it('Get Current user for Authenticated user (root)', () => {
        cy.apollo({queryFile: 'currentUser.graphql'}).should((response: any) => {
            expect(response.data.currentUser.name).to.equal('root');
        });
    });

    it('Get Current user for Authenticated user (root) with an incorrect password', () => {
        cy.apolloClient({username: 'root', password: 'THIS-IS-INCORRECT'})
            .apollo({queryFile: 'currentUser.graphql', errorPolicy: 'all'})
            .should((response: any) => {
                expect(response.data.currentUser).to.be.null;
            });
    });

    it('Get Current user for Authenticated user (root) with an incorrect user', () => {
        cy.apolloClient({username: 'I-DO-NOT-EXIST', password: 'THIS-IS-INCORRECT'})
            .apollo({queryFile: 'currentUser.graphql', errorPolicy: 'all'})
            .should((response: any) => {
                expect(response.data.currentUser).to.be.null;
            });
    });
});
