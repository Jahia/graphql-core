/* eslint-disable @typescript-eslint/no-explicit-any */

describe('Test admin jahia cluster endpoint', () => {
    it('Gets cluster details', () => {
        const systemKey = 'karaf.base'
        cy.apollo({
            queryFile: 'admin/system.graphql',
            variables: { systemKey: systemKey},
        }).should((response: any) => {
            expect(response.data.admin.jahia.system.property.key).to.equal(systemKey)
            expect(response.data.admin.jahia.system.property.value).to.equal("/var/jahia/karaf")

            expect(response.data.admin.jahia.system.os.name).to.equal("Linux")
            expect(response.data.admin.jahia.system.os.architecture.length).to.greaterThan(3)
            expect(response.data.admin.jahia.system.os.version.length).to.greaterThan(3)

            expect(response.data.admin.jahia.system.java.runtimeName).to.equal("OpenJDK Runtime Environment")
            expect(response.data.admin.jahia.system.java.runtimeVersion.length).to.greaterThan(3)
            expect(response.data.admin.jahia.system.java.vendor.length).to.greaterThan(3)
            expect(response.data.admin.jahia.system.java.vendorVersion.length).to.greaterThan(3)
        })
    })
})
