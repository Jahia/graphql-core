describe('GraphQL subscription connection protocol', () => {
    const jahiaUrl = new URL(Cypress.env('JAHIA_URL'));
    const wsUrl = `${jahiaUrl.protocol === 'https:' ? 'wss' : 'ws'}://${jahiaUrl.host}/modules/graphqlws`;
    const subscription = {
        query: 'subscription { backgroundJobSubscription(filterByNames: ["graphql-subscription-connection-test"]) { name } }',
        variables: {}
    };

    let cookie: string;

    before(() => {
        cy.login();
        cy.getCookie('JSESSIONID').then(c => {
            expect(c, 'session cookie').to.not.be.null;
            cookie = `JSESSIONID=${c.value}`;
        });
    });

    it('closes a connection that sends no connection_init in time', () => {
        cy.task('wsHandshake', {url: wsUrl, origin: jahiaUrl.origin, cookie, init: false, timeout: 20000}, {timeout: 25000})
            .then((res: any) => {
                expect(res.status).to.equal(101);
                expect(res.closeCode, res.error).to.equal(4408);
                expect(res.messages).to.not.include('connection_ack');
            });
    });

    it('answers a start for an operation id already running with an error', () => {
        const start = {type: 'start', id: 'op', payload: subscription};
        cy.task('wsHandshake', {url: wsUrl, origin: jahiaUrl.origin, cookie, send: [start, start], wait: 2000})
            .then((res: any) => {
                expect(res.status).to.equal(101);
                expect(res.closeCode, res.error).to.be.null;
                const errors = res.received.filter((m: any) => m.type === 'error');
                expect(errors, JSON.stringify(res.received)).to.have.length(1);
                expect(errors[0].id).to.equal('op');
            });
    });
});
