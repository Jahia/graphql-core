describe('Test admin jahia system endpoint', () => {
    it('Gets system details', () => {
        cy.apollo({
            queryFile: 'admin/system.graphql'
        }).then((response: any) => {
            expect(response.data.admin.jahia.system.os.name).to.equal('Linux');
            expect(response.data.admin.jahia.system.os.architecture.length).to.greaterThan(3);
            expect(response.data.admin.jahia.system.os.version.length).to.greaterThan(3);

            const runtimeName = response.data.admin.jahia.system.java.runtimeName;
            expect(runtimeName).to.satisfy(
                (name: string) => name?.includes('Java') || name?.includes('OpenJDK'),
                'Runtime name should contain either Java or OpenJDK'
            );
            expect(response.data.admin.jahia.system.java.runtimeVersion.length).to.greaterThan(3);
            expect(response.data.admin.jahia.system.java.vendor.length).to.greaterThan(3);
            expect(response.data.admin.jahia.system.java.vendorVersion.length).to.greaterThan(3);
        });
    });
});
