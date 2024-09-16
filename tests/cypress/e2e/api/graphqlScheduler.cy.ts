import gql from 'graphql-tag';
import {WebSocketLink} from '@apollo/client/link/ws';
import {ApolloClient, InMemoryCache} from '@apollo/client';
import {SubscriptionClient} from 'subscriptions-transport-ws';
import {v4 as uuid4} from 'uuid';

describe('Test graphql scheduler', () => {
    const wsUrl = 'ws://localhost:8080/modules/graphqlws';
    const subscriptionClient = new SubscriptionClient(wsUrl);
    const wsLink = new WebSocketLink(
        subscriptionClient
    );
    let jobName: string;

    before('Generate a random unique job name', () => {
        jobName = 'Test job - ' + uuid4();
    });

    after('Remove the background job', () => {
        const removeJobQuery = gql`
            query($jobName: String!) {
                admin {
                    stopAndDeleteJob(jobName: $jobName)
                }
            }
        `;
        cy.apolloClient()
            .apollo({
                query: removeJobQuery,
                variables: {jobName: jobName}
            })
            .should(result => {
                expect(result.data.admin.stopAndDeleteJob).to.equal(true);
            });
    });

    it('test job subscription', () => {
        const apolloClient = new ApolloClient({
            link: wsLink,
            cache: new InMemoryCache()
        });



        const responses = []; // Keep track of the data read in the WS subscription

        cy.wrap(
            new Cypress.Promise((resolve) => {
                // Subscribe to the background job (that is created later on)
                const query = gql`
                    subscription($jobName: String) {
                        backgroundJobSubscription(filterByNames: [$jobName]) {
                            group,
                            name,
                            duration,
                            siteKey,
                            userKey,
                            jobStatus,
                            jobState,
                            jobLongProperty(name: "duration"),
                            foo:jobStringProperty(name: "foo")
                        }
                    }
                `;
                apolloClient
                    .subscribe({
                        query: query,
                        variables: {jobName: jobName}
                    })
                    .subscribe({
                        next(data) {
                            responses.push(data);
                            if (data?.data?.backgroundJobSubscription?.jobState === 'FINISHED') {
                                subscriptionClient.unsubscribeAll();
                                subscriptionClient.close();
                                resolve(responses);
                            }
                        }
                    });

                // create and start the background job
                const createJobQuery = gql`
                    query($jobName: String!) {
                        admin {
                            createAndStartJob(jobName: $jobName)
                        }
                    }
                `;
                cy.apolloClient()
                    .apollo({
                        query: createJobQuery,
                        variables: {jobName: jobName}
                    })
                    .should(result => {
                        expect(result.data.admin.createAndStartJob).to.equal(true);
                    });
            })
        ).then((responses) => {
            expect(responses).to.have.length(2, 'Exactly 2 notifications should be received');
            expect(responses[0].data.backgroundJobSubscription.name).to.equal(jobName);
            expect(responses[0].data.backgroundJobSubscription.jobState).to.equal('STARTED');
            expect(responses[0].data.backgroundJobSubscription.jobStatus).to.equal('EXECUTING');
            expect(responses[0].data.backgroundJobSubscription.duration).to.equal(-1);
            expect(responses[0].data.backgroundJobSubscription.jobLongProperty).to.be.null;
            expect(responses[0].data.backgroundJobSubscription.foo).to.equal('bar');

            expect(responses[1].data.backgroundJobSubscription.name).to.equal(jobName);
            expect(responses[1].data.backgroundJobSubscription.jobState).to.equal('FINISHED');
            expect(responses[1].data.backgroundJobSubscription.jobStatus).to.equal('SUCCESSFUL');
            expect(responses[1].data.backgroundJobSubscription.duration).to.greaterThan(500);
            expect(responses[1].data.backgroundJobSubscription.jobLongProperty).to.greaterThan(500);
            expect(responses[1].data.backgroundJobSubscription.foo).to.equal('bar');
        });
    });
});

