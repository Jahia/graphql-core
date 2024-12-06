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

    it('Paginated job list should not be empty', function () {
        cy.apollo({
            query: gql`
                query {
                    admin {
                        jahia {
                            scheduler {
                                paginatedJobs {
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
            const result = response.data.admin.jahia.scheduler.paginatedJobs.nodes;
            expect(result.length).to.gt(0);
        });
    });
});
