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
                    // Jahia 8.2.1 and 8.2.2 are using GraalVM, the rest of the versions are using OpenJDK
                    const jahiaVersionTrimmed = jahiaVersion.release.replace('-SNAPSHOT', '');
                    if (compare(jahiaVersionTrimmed, '8.2.1', '=') || compare(jahiaVersionTrimmed, '8.2.2', '=')) {
                        expect(response.data.admin.jahia.system.java.runtimeName).to.equal('Java(TM) SE Runtime Environment');
                    } else {
                        expect(response.data.admin.jahia.system.java.runtimeName).to.equal('OpenJDK Runtime Environment');
                    }
                });
            });
            expect(response.data.admin.jahia.system.java.runtimeVersion.length).to.greaterThan(3);
            expect(response.data.admin.jahia.system.java.vendor.length).to.greaterThan(3);
            expect(response.data.admin.jahia.system.java.vendorVersion.length).to.greaterThan(3);
        });
    });
});
