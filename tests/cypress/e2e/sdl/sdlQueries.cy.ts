import gql from 'graphql-tag';

describe('Node validity graphql test', () => {
    before('create nodes with validity constrains', () => {
        // Setup data for testing
        console.log('run groovy script');
        cy.executeGroovy('groovy/prepareSDLQueriesTest.groovy', {});
    });
    after('clean up test data', () => {
        cy.apollo({
            mutation: gql`
                mutation {
                    jcr {
                        mutateNodes(pathsOrIds: ["/sites/systemsite/contents/sdlQueriesTest"]) {
                            delete
                        }
                    }
                }
            `
        });
        cy.apollo({
            errorPolicy: 'all',
            mutation: gql`
                mutation {
                    jcr {
                        mutateNodes(pathsOrIds: ["/sites/systemsite"]) {
                            publish
                        }
                    }
                }
            `
        });
    });
    it('Finds all offers without Relay', () => {
        cy.apollo({
            query: gql`
               query {
                   allOffers{
                       title
                       subTitle
                       coverages {
                           coverageTitle
                       }
                   }
               }
           `
        }).then(response => {
            cy.log(JSON.stringify(response.data, null, 2));
            expect(response.data.allOffers).to.have.length(10);
            expect(response.data.allOffers[5].coverages).to.have.length(3);
        });
    });

    it('Finds first 5 offers with Relay', () => {
        cy.apollo({
            query: gql`
                query {
                    allOffersConnection(first: 5) {
                        edges {
                            node {    
                                title
                                subTitle
                                coverages {
                                    coverageTitle
                                }
                            }
                        }
                        pageInfo {
                            totalCount
                            nodesCount
                        }
                    }
                }
            `
        }).then(response => {
            cy.log(JSON.stringify(response.data, null, 2));
            expect(response.data.allOffersConnection.edges).to.have.length(5);
            expect(response.data.allOffersConnection.pageInfo.nodesCount).to.equal(5);
            expect(response.data.allOffersConnection.pageInfo.totalCount).to.equal(10);
            expect(response.data.allOffersConnection.edges[3].node.coverages).to.have.length(3);
        });
    });
});
