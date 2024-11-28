import gql from 'graphql-tag';

describe('Jobs GraphQL endpoint', () => {
    it('Background job list should not be empty', function () {
        cy.apollo({
            query: gql`
                query {
                    admin {
                        jahia {
                            scheduler {
                                jobs {
                                    nodes {
                                        group
                                        name
                                    }
                                }
                            }
                        }
                    }
                }
            `
        }).should(response => {
            const result = response.data.admin.jahia.scheduler.jobs.nodes;
            expect(result.length).to.gt(0);
        });
    });
});
