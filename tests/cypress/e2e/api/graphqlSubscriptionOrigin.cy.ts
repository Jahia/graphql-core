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

    const connect = (origin?: string) => cy.task('wsHandshake', {url: wsUrl, origin, cookie});

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
                expect(res.messages, origin).to.not.include('connection_ack');
            });
        }
    });

    it('refuses a connection that declares no origin', () => {
        for (const origin of [undefined, 'null']) {
            connect(origin).then((res: any) => {
                expect(res.status, String(origin)).to.equal(403);
                expect(res.messages, String(origin)).to.have.length(0);
            });
        }
    });
});
