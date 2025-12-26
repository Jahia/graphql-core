describe('GraphQL Workspace tests', () => {
    const GRAPHQL_WORKSPACE_URL = '/modules/graphql-dxm-provider/tools/graphql-workspace.jsp';
    beforeEach('prerequisites', () => {
        cy.login();
    });

    it('Should the link to the GraphQL workspace be available on the /tools page', () => {
        cy.visit('/tools');
        cy.get(`a[href="${GRAPHQL_WORKSPACE_URL}"]`).should('exist');
        cy.get(`a[href="${GRAPHQL_WORKSPACE_URL}"]`).contains('Jahia GraphQL Core Provider : graphql-workspace');
    });

    it('Should be able to open the GraphQL workspace', () => {
        // Spy on console errors, warnings and messages to make sur UI is loaded without issues.
        cy.on('window:before:load', window => {
            cy.spy(window.console, 'error').as('errors');
            cy.spy(window.console, 'warn').as('warnings');
            cy.spy(window.console, 'log').as('messages');
        });

        cy.visit(GRAPHQL_WORKSPACE_URL);
        cy.get('[aria-label="Show Documentation Explorer"]').click();

        // Make sure Root Types exist
        cy.get('div.graphiql-doc-explorer-section-content>div').contains('query: Query');
        cy.get('div.graphiql-doc-explorer-section-content>div').contains('mutation: Mutation');
        cy.get('div.graphiql-doc-explorer-section-content>div').contains('subscription: Subscription');

        // Look for "All Schema Types" title,
        // and then for all links in the sibling div to make sure their list is not empty.
        // Artificial "magic" number 42 was chosen to make sure list contains meaningful amount of types.
        // See https://simple.wikipedia.org/wiki/42_(answer) for reference.
        cy.get('div.graphiql-doc-explorer-section-title')
            .contains('All Schema Types')
            .next()
            .find('a')
            .should('have.length.greaterThan', 42);

        // Verify errors or warnings are absent in console during loading.
        // Ensure expected log messages are present.
        cy.get('@warnings').should('have.callCount', 0);
        cy.get('@errors').should('have.callCount', 0);
        cy.get('@messages').then(messages => {
            const allMessages = messages.getCalls().map(call => call.args.join(' '));

            expect(allMessages.some(msg => msg.toLowerCase().includes('starting')), 'Console should contain "starting"').to.be.true;
            expect(allMessages.some(msg => msg.toLowerCase().includes('dom loaded')), 'Console should contain "dom loaded"').to.be.true;
        });
    });
});
