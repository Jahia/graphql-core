import gql from 'graphql-tag';
import {uploadFile} from '@jahia/cypress';

describe('Import content', () => {
    const importContentGql = gql`
        mutation importContent($path: String!, $file: String!) {
            jcr {
                importContent(parentPathOrId: $path, file: $file, rootBehaviour: 1)
            }
        }
    `;

    const vfsMount = gql`
        mutation vfsMutation {
            admin {
                mountPoint {
                    addVfs(
                    name: "testVfs"
                    rootPath: "/home/tomcat"
                    mountPointRefPath: "/sites/systemsite/files"
                )
            }
          }
        }
    `;

    const moveNode = gql`mutation moveNode($pathsOrIds: [String]!, $destParentPathOrId: String!) {
        jcr {
            mutateNodes(pathsOrIds: $pathsOrIds) {
                move(parentPathOrId: $destParentPathOrId, renameOnConflict: true)
                node {
                    path
                }
            }
        }
    }
    `;

    before(() => {
        uploadFile('jcr/snowbearHome.jpeg', '/sites/systemsite/files', 'snowbearHome.jpeg', 'image/jpeg').then(() => {
            cy.apollo({
                mutation: vfsMount
            });
        });
    });

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

    it('can move node into a vfs without exception', () => {
        cy.apollo({
            mutation: moveNode,
            variables: {
                pathsOrIds: ['/sites/systemsite/files/snowbearHome.jpeg'],
                destParentPathOrId: '/sites/systemsite/files/testVfs'
            }
        }).then(result => {
            expect(JSON.stringify(result)).to.equal('{"data":{"jcr":{"mutateNodes":[{"move":"/sites/systemsite/files/testVfs/snowbearHome.jpeg","node":{"path":"/sites/systemsite/files/testVfs/snowbearHome.jpeg","__typename":"GenericJCRNode"},"__typename":"JCRNodeMutation"}],"__typename":"JCRMutation"}}}');
        });
    });
});
