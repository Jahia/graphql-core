/* eslint-disable @typescript-eslint/no-explicit-any */
import {isValid} from 'date-fns';

describe('admin.datetime - Jahia Server time', () => {
    it('Get Jahia server current time (as root)', () => {
        cy.apollo({queryFile: 'admin/datetime.graphql'}).should((response: any) => {
            expect(isValid(new Date(response.data.admin.datetime))).to.be.true;
        });
    });

    // Note: Should we allow guest to query server time ?
    it('Get Jahia server current time (as guest)', () => {
        cy.apolloClient({username: 'guest', password: 'I-DO-NOT-EXIST'})
            .apollo({queryFile: 'admin/datetime.graphql', errorPolicy: 'all'})
            .should((response: any) => {
                expect(response.errors).to.be.not.empty;
            });
    });
});
