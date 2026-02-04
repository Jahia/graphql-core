
export function importContent(parentPath: string, fixtureFile: string, mimeType: string) {
    const filename = fixtureFile.split('/').pop();
    return cy.fixture(fixtureFile, 'binary').then(binaryFile => {
        // Convert the file base64 string to a blob
        const blob = Cypress.Blob.binaryStringToBlob(binaryFile, mimeType);
        const file = new File([blob], filename, {type: blob.type});

        return cy.apollo({
            mutationFile: 'jcr/import/importContent.graphql',
            variables: {
                path: parentPath,
                file
            }
        });
    });
}
