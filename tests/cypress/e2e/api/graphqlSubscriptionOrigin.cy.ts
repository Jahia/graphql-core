describe('GraphQL subscription connection origin', () => {
    const jahiaUrl = new URL(Cypress.env('JAHIA_URL'));
    const wsUrl = `${jahiaUrl.protocol === 'https:' ? 'wss' : 'ws'}://${jahiaUrl.host}/modules/graphqlws`;
    const otherPort = `${jahiaUrl.protocol}//${jahiaUrl.hostname}:${jahiaUrl.port === '9999' ? '9998' : '9999'}`;

    let cookie: string;

    before(() => {
        cy.login();
        cy.getCookie('JSESSIONID').then(c => {
            expect(c, 'session cookie').to.not.be.null;
            cookie = `JSESSIONID=${c.value}`;
        });
    });

    const connect = (origin?: string, headers?: Record<string, string>) => cy.task('wsHandshake', {url: wsUrl, origin, cookie, headers});

    it('accepts a connection from the origin Jahia is served on', () => {
        connect(jahiaUrl.origin).then((res: any) => {
            expect(res.status).to.equal(101);
            expect(res.messages).to.include('connection_ack');
            expect(res.closeCode, res.error).to.be.null;
        });
    });

    it('closes a connection from another origin before acknowledging it', () => {
        for (const origin of ['https://www.example.com', otherPort]) {
            connect(origin).then((res: any) => {
                expect(res.status, origin).to.equal(101);
                expect(res.closeCode, `${origin} ${res.error || ''}`).to.equal(1008);
                expect(res.closeReason, origin).to.equal('Origin not allowed');
                expect(res.messages, origin).to.not.include('connection_ack');
            });
        }
    });

    it('accepts the https address a TLS-ending proxy forwards', () => {
        // The proxy keeps the public host name, ends TLS and states the scheme it received in X-Forwarded-Proto.
        const httpsOrigin = `https://${jahiaUrl.hostname}`;
        connect(httpsOrigin, {Host: jahiaUrl.hostname, 'X-Forwarded-Proto': 'https'}).then((res: any) => {
            expect(res.status).to.equal(101);
            expect(res.messages).to.include('connection_ack');
            expect(res.closeCode, res.error).to.be.null;
        });
        connect(httpsOrigin, {Host: jahiaUrl.hostname}).then((res: any) => {
            expect(res.status).to.equal(101);
            expect(res.closeCode, res.error).to.equal(1008);
            expect(res.messages).to.not.include('connection_ack');
        });
    });

    it('closes a connection that declares no origin and is granted no subscription', () => {
        // The origin is accepted. Jahia grants the scope of its interfaces to a request from one of its pages, so this
        // connection is granted no subscription, and it is closed for that reason.
        connect(undefined).then((res: any) => {
            expect(res.status).to.equal(101);
            expect(res.closeCode, res.error).to.equal(1008);
            expect(res.closeReason).to.equal('Subscriptions not allowed');
            expect(res.messages).to.not.include('connection_ack');
        });
    });

    it('closes a connection that declares the opaque origin null', () => {
        connect('null').then((res: any) => {
            expect(res.status).to.equal(101);
            expect(res.closeCode, res.error).to.equal(1008);
            expect(res.messages).to.not.include('connection_ack');
        });
    });
});
