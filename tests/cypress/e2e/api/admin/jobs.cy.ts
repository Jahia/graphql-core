import gql from 'graphql-tag';

describe('Jobs graphQL endpoint', () => {
    it('background job list should not be empty', function () {
        cy.apollo({
            query: gql`
                query {
                    admin {
                        jahia {
                            scheduler {
                                jobs {
                                    group
                                    name
                                }
                            }
                        }
                    }
                }
            `
        }).should(response => {
            const result = response.data.admin.jahia.scheduler.jobs;
            expect(result.length).to.gt(0);
        });
    });
});
