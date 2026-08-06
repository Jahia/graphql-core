import {deleteNode, getNodeByPath} from '@jahia/cypress';
import gql from 'graphql-tag';

describe('Deprecated createVersion field', () => {
    const nodePath = '/testCreateVersionList';

    afterEach(() => {
        deleteNode(nodePath);
    });

    it('returns false and still persists the rest of the mutation', () => {
        cy.apollo({
            mutation: gql`
                mutation createVersionOnAddNode {
                    jcr {
                        addNode(
                            parentPathOrId: "/"
                            primaryNodeType: "jnt:contentList"
                            name: "testCreateVersionList"
                        ) {
                            createVersion
                            uuid
                        }
                    }
                }
            `
        }).then(result => {
            // Inert no-op: it must not raise, because an error would make the execution strategy
            // discard every other write in the same mutation (addNode included).
            expect(result?.graphQLErrors, 'createVersion must not raise').to.be.undefined;
            expect(result?.data?.jcr?.addNode?.createVersion).to.be.false;
            expect(result?.data?.jcr?.addNode?.uuid).to.exist;
        });

        getNodeByPath(nodePath).then(response => {
            expect(response?.data?.jcr?.nodeByPath?.uuid).to.exist;
        });
    });
});
