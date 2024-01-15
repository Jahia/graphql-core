import gql from 'graphql-tag';

describe('Import content', () => {
    const importContentGql = gql`
        mutation importContent($path: String!, $file: String!) {
            jcr {
                importContent(parentPathOrId: $path, file: $file, rootBehaviour: 1)
            }
        }
    `;

    it('should reject zip bomb', () => {
        cy.fixture('jcr/zbsm.zip', 'binary').then(zipBomb => {
            const blob = Cypress.Blob.binaryStringToBlob(zipBomb, 'application/zip');
            const file = new File([blob], 'zbsm.zip', {type: blob.type});
            cy.apollo({
                mutation: importContentGql,
                variables: {path: '/sites/digitall/home', file}
            }).then(result => {
                console.log(JSON.stringify(result));
                expect(result.graphQLErrors[0].message).to.contain('FileSizeLimitExceededException');
            });
        });
    });
});
