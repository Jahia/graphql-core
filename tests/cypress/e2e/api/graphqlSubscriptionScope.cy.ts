describe('GraphQL subscription scopes', () => {
    const jahiaUrl = new URL(Cypress.env('JAHIA_URL'));
    const wsUrl = `${jahiaUrl.protocol === 'https:' ? 'wss' : 'ws'}://${jahiaUrl.host}/modules/graphqlws`;
    const start = (id: string, query: string) => ({type: 'start', id, payload: {query, variables: {}}});

    let cookie: string;

    // A connection that declares no origin is not granted the scope of the Jahia interfaces, so its scopes are the
    // ones that apply to every request. Karaf reads a scope file some time after it is written, so the handshake is
    // repeated until the connection is acknowledged, or closed once the file is removed.
    const waitForHandshake = (acknowledged: boolean) => cy.waitUntil(
        () => cy.task('wsHandshake', {url: wsUrl, cookie})
            .then((res: any) => res.messages.includes('connection_ack') === acknowledged),
        {timeout: 30000, interval: 1000}
    );

    before(() => {
        cy.login();
        cy.getCookie('JSESSIONID').then(c => {
            expect(c, 'session cookie').to.not.be.null;
            cookie = `JSESSIONID=${c.value}`;
        });
        cy.executeGroovy('groovy/addSubscriptionScopeConfig.groovy');
        waitForHandshake(true);
    });

    after(() => {
        cy.executeGroovy('groovy/cleanupSubscriptionScopeConfig.groovy');
        waitForHandshake(false);
    });

    it('runs only the subscription fields that the scopes of the connection grant', () => {
        // The root user may run backgroundJobSubscription, so only the scope of the connection denies it.
        const send = [
            start('granted', 'subscription { workflowEvent { activeWorkflowTaskCountForUser } }'),
            start('denied', 'subscription { backgroundJobSubscription(filterByNames: ["graphql-subscription-scope-test"]) { name } }')
        ];
        cy.task('wsHandshake', {url: wsUrl, cookie, send, wait: 2000}).then((res: any) => {
            const received = JSON.stringify(res.received);
            expect(res.status).to.equal(101);
            expect(res.closeCode, res.error).to.be.null;
            expect(res.received.filter((m: any) => m.id === 'granted' && m.type === 'error'), received).to.have.length(0);
            const denied = res.received.filter((m: any) => m.id === 'denied');
            expect(JSON.stringify(denied), received).to.match(/Permission denied/);
        });
    });
});
