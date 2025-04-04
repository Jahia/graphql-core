/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint max-nested-callbacks: ["error", 5] */
import {getJahiaVersion} from '@jahia/cypress';
import {compare} from 'compare-versions';

describe('Test admin jahia system endpoint', () => {
    it('Gets system details', () => {
        cy.apollo({
            queryFile: 'admin/system.graphql'
        }).then((response: any) => {
            expect(response.data.admin.jahia.system.os.name).to.equal('Linux');
            expect(response.data.admin.jahia.system.os.architecture.length).to.greaterThan(3);
            expect(response.data.admin.jahia.system.os.version.length).to.greaterThan(3);

            getJahiaVersion().then(jahiaVersion => {
                cy.log(JSON.stringify(jahiaVersion)).then(() => {
                    if (compare(jahiaVersion.release.replace('-SNAPSHOT', ''), '8.2.1', '<')) {
                        expect(response.data.admin.jahia.system.java.runtimeName).to.equal('OpenJDK Runtime Environment');
                    } else {
                        // With TECH-2029 the GraalVM vendor was changed, resulting in a new runtime name
                        expect(response.data.admin.jahia.system.java.runtimeName).to.equal('Java(TM) SE Runtime Environment');
                    }
                });
            });
            expect(response.data.admin.jahia.system.java.runtimeVersion.length).to.greaterThan(3);
            expect(response.data.admin.jahia.system.java.vendor.length).to.greaterThan(3);
            expect(response.data.admin.jahia.system.java.vendorVersion.length).to.greaterThan(3);
        });
    });
});
