import {createSite, deleteNode, deleteSite} from '@jahia/cypress';
import {importContent} from '../../../fixtures/jcr/import';
import gql from 'graphql-tag';

describe('Test JCR import API', () => {
    const siteKey = 'importTestSite';

    before('setup', () => {
        createSite(siteKey);
    });

    after('cleanup', () => {
        deleteSite(siteKey);
    });

    const importXml = (mimeType: string) => {
        importContent(`/sites/${siteKey}/contents`, 'jcr/import/rich-text.xml', mimeType)
            .then(response => {
                expect(response.data?.jcr.importContent, 'import should have returned success boolean').to.be.true;
            });
        cy.waitUntil(
            () => {
                return cy.apollo({
                    query: gql`query findImportContent {
                        jcr { nodeByPath(path: "/sites/${siteKey}/contents/rich-text") {uuid}}
                    }`
                }).then(response => Boolean(response.data.jcr.nodeByPath.uuid));
            },
            {timeout: 30000, interval: 1000}
        );
        deleteNode(`/sites/${siteKey}/contents/rich-text`);
    };

    it('imports content with "application/xml" mimetype', () => {
        importXml('application/xml');
    });

    it('imports content with "text/xml" mimetype', () => {
        importXml('text/xml');
    });
});
