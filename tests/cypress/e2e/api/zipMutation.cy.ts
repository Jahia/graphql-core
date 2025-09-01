import {createSite, deleteSite, getNodeByPath, uploadFile} from '@jahia/cypress';
import gql from 'graphql-tag';

describe('Test zip mutation', () => {
    const siteKey = 'testZipMutation';

    before('Create a site', () => {
        createSite(siteKey);
    });

    after('Delete the site', () => {
        deleteSite(siteKey);
    });

    it('should have the correct MIME type when unzipping a file with a known extension', () => {
        const extension = 'pdf';
        const expectedMimeType = 'application/pdf';
        uploadAndUnzip(extension);
        expectMimeTypeToBe(extension, expectedMimeType);
    });

    it('when unzipping a file with an unknown extension but with its MIME type detectable, should determine its MIME type based on its content', () => {
        const extension = 'hol';
        const expectedMimeType = 'text/plain';
        uploadAndUnzip(extension);
        expectMimeTypeToBe(extension, expectedMimeType);
    });

    it('when unzipping a file with an jar extension, should determine its MIME type based on extension (jar-specific case)', () => {
        const extension = 'jar';
        const expectedMimeType = 'application/java-archive';
        uploadAndUnzip(extension);
        expectMimeTypeToBe(extension, expectedMimeType);
    });

    it('when unzipping a file with an unknown extension, should use the fallback MIME type', () => {
        const extension = 'unknown';
        const expectedMimeType = 'application/octet-stream';
        uploadAndUnzip(extension);
        expectMimeTypeToBe(extension, expectedMimeType);
    });

    function uploadAndUnzip(extension: string) {
        // Upload the zip file, expected to be named '<extension>.zip' and located in 'jcr/zipMutation/':
        uploadFile(
            `jcr/zipMutation/${extension}.zip`,
            `/sites/${siteKey}/files`,
            `${extension}.zip`,
            'application/zip'
        ).then(result => {
            expect(result?.data?.jcr?.addNode?.uuid).to.exist;
        });

        // Unzip the file:
        const mutation = gql`
            mutation unzipFile($pathOrId: String!, $path: String!) {
                jcr {
                    mutateNode(pathOrId: $pathOrId) {
                        zip {
                            unzip(path: $path)
                        }
                    }
                }
            }
        `;
        cy.apollo({
            mutation: mutation,
            variables: {pathOrId: `/sites/${siteKey}/files/${extension}.zip`, path: `/sites/${siteKey}/files`}
        }).then(result => {
            expect(result?.data?.jcr?.mutateNode?.zip?.unzip).to.be.true;
        });
    }

    function expectMimeTypeToBe(extension: string, expectedMimeType: string) {
        getNodeByPath(`/sites/${siteKey}/files/sample.${extension}/jcr:content/`, ['jcr:mimeType']).then(response => {
            const properties = response?.data?.jcr?.nodeByPath?.properties;
            expect(properties).to.have.length(1);
            const mimeType = properties[0];
            expect(mimeType.name).to.equal('jcr:mimeType');
            expect(mimeType.value).to.equal(expectedMimeType);
        });
    }
});
