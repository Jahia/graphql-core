/* eslint-disable @typescript-eslint/no-explicit-any */

describe('Test admin jahia database endpoint', () => {
    it('Gets database details', () => {
        cy.apollo({
            queryFile: 'admin/database.graphql'
        }).should((response: any) => {
            expect(response.data.admin.jahia.database.type).to.equal('derby')

            expect(response.data.admin.jahia.database.name).to.equal('Apache Derby')
            expect(response.data.admin.jahia.database.version.length).to.greaterThan(8)

            expect(response.data.admin.jahia.database.driverName).to.equal('Apache Derby Embedded JDBC Driver')
            expect(response.data.admin.jahia.database.driverVersion.length).to.greaterThan(8)

            expect(response.data.admin.jahia.database.url).to.equal('jdbc:derby:directory:/var/jahia/jahiadb')
        })
    })
})
