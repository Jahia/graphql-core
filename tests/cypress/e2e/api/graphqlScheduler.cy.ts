import gql from 'graphql-tag';
import {WebSocketLink} from '@apollo/client/link/ws';
import {ApolloClient, InMemoryCache, NormalizedCacheObject} from '@apollo/client';
import {SubscriptionClient} from 'subscriptions-transport-ws';
import {v4 as uuid4} from 'uuid';

describe('Test graphql scheduler', () => {
    const url = new URL(Cypress.env('JAHIA_URL'));
    const wsProtocol = url.protocol === 'https:' ? 'wss' : 'ws';
    const wsUrl = `${wsProtocol}://${url.host}/modules/graphqlws`;

    const SUBSCRIPTION = gql`
        subscription($jobName: String) {
            backgroundJobSubscription(filterByNames: [$jobName]) {
                group, name, duration, siteKey, userKey, jobStatus, jobState,
                jobLongProperty(name: "duration"),
                foo:jobStringProperty(name: "foo")
            }
        }
    `;
    const createJobQuery = gql`
        query($jobName: String!) {
            admin { createAndStartJobForGraphQLSchedulerCypressTest(jobName: $jobName) }
        }
    `;
    const removeJobQuery = gql`
        query($jobName: String!) {
            admin { stopAndDeleteJobForGraphQLSchedulerCypressTest(jobName: $jobName) }
        }
    `;

    // Open a fresh lazy WS subscription. lazy:true → the socket only opens on the first
    // subscribe(), which we defer into a cy.then so it runs AFTER the login/clearCookies command
    // that sets (or clears) the session cookie the handshake will carry. Resolves with everything
    // the connection received: data payloads and/or errors.
    const collect = (jobName: string) => {
        const client = new SubscriptionClient(wsUrl, {lazy: true, reconnect: false});
        const apollo: ApolloClient<NormalizedCacheObject> = new ApolloClient({
            link: new WebSocketLink(client),
            cache: new InMemoryCache()
        });
        const data: any[] = [];
        const errors: any[] = [];
        const promise = new Cypress.Promise(resolve => {
            const finish = () => {
                try {
                    client.unsubscribeAll();
                    client.close();
                } catch (e) {
                    // Ignore teardown errors: the connection may already be closed.
                }

                resolve({data, errors});
            };

            apollo.subscribe({query: SUBSCRIPTION, variables: {jobName}}).subscribe({
                next(value: any) {
                    data.push(value);
                    if (value?.data?.backgroundJobSubscription?.jobState === 'FINISHED') {
                        finish();
                    }
                },
                error(err: any) {
                    errors.push(err);
                    finish();
                }
            });
            // Safety net so a stuck subscription surfaces as an assertion failure, not a hang.
            setTimeout(finish, 20000);
        });
        return promise;
    };

    it('delivers background job events to an authenticated subscriber', () => {
        const jobName = 'Test job auth - ' + uuid4();
        // Authenticate first: the WS handshake is a plain HTTP GET, so the browser attaches the
        // session cookie automatically → the connection is privileged and authorized.
        cy.login();

        let sub: Cypress.Promise<any>;
        cy.then(() => {
            sub = collect(jobName);
        });

        cy.apolloClient()
            .apollo({query: createJobQuery, variables: {jobName}})
            .should((result: any) => {
                expect(result.data.admin.createAndStartJobForGraphQLSchedulerCypressTest).to.equal(true);
            });

        cy.then(() => sub).then((res: any) => {
            expect(res.errors, 'authenticated subscriber must not be denied').to.have.length(0);
            const responses = res.data;
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
            expect(responses[1].data.backgroundJobSubscription.jobLongProperty).to.greaterThan(500);
            expect(responses[1].data.backgroundJobSubscription.duration).to.greaterThan(500);
            expect(responses[1].data.backgroundJobSubscription.foo).to.equal('bar');
        });

        cy.apolloClient()
            .apollo({query: removeJobQuery, variables: {jobName}})
            .should((result: any) => {
                expect(result.data.admin.stopAndDeleteJobForGraphQLSchedulerCypressTest).to.equal(true);
            });
    });

    it('denies background job events to an unauthenticated (guest) subscriber', () => {
        const jobName = 'Test job guest - ' + uuid4();
        // No login: the handshake carries no session cookie, so the connection is not a privileged
        // user and (in a non-hosted context) not same-origin → the subscription is rejected.
        cy.clearCookies();

        let sub: Cypress.Promise<any>;
        cy.then(() => {
            sub = collect(jobName);
        });

        // Trigger the job as admin over HTTP (independent of the guest WS) so an event would fire
        // for an authorized subscriber — proving the guest is denied the data, not merely idle.
        cy.apolloClient()
            .apollo({query: createJobQuery, variables: {jobName}})
            .should((result: any) => {
                expect(result.data.admin.createAndStartJobForGraphQLSchedulerCypressTest).to.equal(true);
            });

        cy.then(() => sub).then((res: any) => {
            expect(res.data, 'guest must receive no background-job data').to.have.length(0);
            expect(res.errors, 'guest subscription must be rejected').to.have.length.greaterThan(0);
            expect(JSON.stringify(res.errors)).to.match(/Permission denied|AccessDenied/i);
        });

        // Clean up the job we started (as admin).
        cy.apolloClient().apollo({query: removeJobQuery, variables: {jobName}});
    });
});
