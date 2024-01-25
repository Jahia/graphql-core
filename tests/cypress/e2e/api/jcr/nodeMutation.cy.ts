import gql from 'graphql-tag';
import {addNode, copyNode, deleteNode, renameNode} from '@jahia/cypress';

describe('Node mutation', () => {
    context('Import content', () => {
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

    context('Node name checks', () => {
        const longNodeName = 'abcde'.repeat(30);

        it('Should save and rename node according to limit of maxNameSize', () => {
            addNode({
                parentPathOrId: '/sites/digitall/home/area-main',
                name: longNodeName,
                primaryNodeType: 'jnt:text',
                properties: [
                    {
                        language: 'en',
                        name: 'text',
                        value: 'My text'
                    }
                ]
            }).then(result => {
                const name = result.data.jcr.addNode.node.name;
                expect(name).to.equal(longNodeName.substring(0, 128));
                deleteNode(`/sites/digitall/home/area-main/${longNodeName.substring(0, 128)}`);
            });
        });

        it('Should limit name value on rename', () => {
            addNode({
                parentPathOrId: '/sites/digitall/home/area-main',
                name: 'test',
                primaryNodeType: 'jnt:text',
                properties: [
                    {
                        language: 'en',
                        name: 'text',
                        value: 'My text'
                    }
                ]
            }).then(() => {
                renameNode({
                    pathOrId: '/sites/digitall/home/area-main/test',
                    newName: longNodeName
                }).then(result => {
                    const name = result.data.jcr.mutateNode.rename;
                    expect(name).to.equal(`/sites/digitall/home/area-main/${longNodeName.substring(0, 128)}`);
                    deleteNode(`/sites/digitall/home/area-main/${longNodeName.substring(0, 128)}`);
                });
            });
        });

        it('Should limit name on copy', () => {
            addNode({
                parentPathOrId: '/sites/digitall/home/area-main',
                name: 'test',
                primaryNodeType: 'jnt:text',
                properties: [
                    {
                        language: 'en',
                        name: 'text',
                        value: 'My text'
                    }
                ]
            }).then(() => {
                copyNode({
                    pathOrId: '/sites/digitall/home/area-main/simple-text',
                    destParentPathOrId: '/sites/digitall/home/about/area-main',
                    destName: longNodeName
                }).then(result => {
                    const name = result.data.jcr.copyNode.node.name;
                    expect(name).to.equal(`${longNodeName.substring(0, 128)}`);
                    deleteNode('/sites/digitall/home/area-main/simple-text');
                    deleteNode(`/sites/digitall/home/area-main/about/${longNodeName.substring(0, 128)}`);
                });
            });
        });
    });
});
