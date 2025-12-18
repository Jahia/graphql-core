describe('GraphQL Workspace tests', () => {
    const GRAPHQL_WORKSPACE_URL = '/modules/graphql-dxm-provider/tools/graphql-workspace.jsp';
    beforeEach('reset default value', () => {
        cy.login();
    });

    it('Should the link to the GraphQL workspace be available on the /tools page', () => {
        cy.visit('/tools');
        cy.get(`a[href="${GRAPHQL_WORKSPACE_URL}"]`).should('exist');
        cy.get(`a[href="${GRAPHQL_WORKSPACE_URL}"]`).contains('Jahia GraphQL Core Provider : graphql-workspace');
    });

    it('Should be able to open the GraphQL workspace', () => {
        cy.visit(GRAPHQL_WORKSPACE_URL);
        cy.get('[aria-label="Show Documentation Explorer"]').click();
        // Validate that the JCRNode type, as an example, is present
        cy.get('a.graphiql-doc-explorer-type-name').contains('JCRNode');
    });
});
