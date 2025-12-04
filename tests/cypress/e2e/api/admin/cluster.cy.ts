
describe('Test admin jahia cluster endpoint', () => {
    it('Gets cluster details', () => {
        cy.apollo({
            queryFile: 'admin/cluster.graphql'
        }).should((response: any) => {
            expect(response.data.admin.cluster.isActivated).to.equal(false);
        });
    });
});
