import gql from 'graphql-tag';
import {addNode, createUser, deleteNode, deleteUser, grantRoles} from '@jahia/cypress';

const TEST_NODE = 'decryptedValueTestList';
const TEST_NODE_PATH = `/${TEST_NODE}`;
const TEST_ROLE = 'decryptedValueTestRole';
const PLAIN_VALUE = 'decrypted value test';
const NO_PERM_USER = {username: 'decryptNoPermUser', password: 'decryptNoPermPwd1!'};
const PERM_USER = {username: 'decryptPermUser', password: 'decryptPermPwd1!'};

const readTitle = gql`
    query {
        jcr {
            nodeByPath(path: "${TEST_NODE_PATH}") {
                property(name: "jcr:title", language: "en") {
                    value
                    decryptedValue
                }
            }
        }
    }
`;

const readPermissionNames = (rolePath: string) => cy.apolloClient().apollo({
    query: gql`
        query {
            jcr {
                nodeByPath(path: "${rolePath}") {
                    property(name: "j:permissionNames") {
                        values
                    }
                }
            }
        }
    `
});

// Removes a node that a previous interrupted run may have left, and accepts that it is absent
const removeLeftover = (path: string) => cy.apolloClient().apollo({
    mutation: gql`mutation { jcr { deleteNode(pathOrId: "${path}") } }`,
    errorPolicy: 'all'
});

describe('Read the decrypted value of an encrypted property', () => {
    before('create an encrypted property, a role and two users', () => {
        removeLeftover(TEST_NODE_PATH);
        removeLeftover(`/roles/${TEST_ROLE}`);
        deleteUser(NO_PERM_USER.username);
        deleteUser(PERM_USER.username);

        addNode({parentPathOrId: '/', name: TEST_NODE, primaryNodeType: 'jnt:contentList'});
        cy.apolloClient().apollo({
            mutation: gql`
                mutation {
                    jcr {
                        mutateNode(pathOrId: "${TEST_NODE_PATH}") {
                            mutateProperty(name: "jcr:title") {
                                setValue(language: "en", value: "${PLAIN_VALUE}", option: ENCRYPTED)
                            }
                        }
                    }
                }
            `
        });

        // A role that adds the new permission alone, so the two users differ by that permission only
        addNode({parentPathOrId: '/roles', name: TEST_ROLE, primaryNodeType: 'jnt:role'});
        cy.apolloClient().apollo({
            mutation: gql`
                mutation {
                    jcr {
                        mutateNode(pathOrId: "/roles/${TEST_ROLE}") {
                            mutateProperty(name: "j:permissionNames") {
                                setValues(values: ["viewEncryptedValue"])
                            }
                        }
                    }
                }
            `
        });

        createUser(NO_PERM_USER.username, NO_PERM_USER.password);
        createUser(PERM_USER.username, PERM_USER.password);
        grantRoles(TEST_NODE_PATH, ['editor'], NO_PERM_USER.username, 'USER');
        grantRoles(TEST_NODE_PATH, ['editor', TEST_ROLE], PERM_USER.username, 'USER');
    });

    after('remove the test data', () => {
        deleteUser(NO_PERM_USER.username);
        deleteUser(PERM_USER.username);
        deleteNode(TEST_NODE_PATH);
        deleteNode(`/roles/${TEST_ROLE}`);
    });

    it('stores the value encrypted', () => {
        cy.apolloClient().apollo({query: readTitle}).should(result => {
            const property = result.data.jcr.nodeByPath.property;
            expect(property.value).to.not.equal(PLAIN_VALUE);
        });
    });

    it('returns the decrypted value to root', () => {
        cy.apolloClient().apollo({query: readTitle}).should(result => {
            expect(result.data.jcr.nodeByPath.property.decryptedValue).to.equal(PLAIN_VALUE);
        });
    });

    it('returns null to an editor without the permission', () => {
        cy.apolloClient(NO_PERM_USER).apollo({query: readTitle}).should(result => {
            expect(result.graphQLErrors, JSON.stringify(result.graphQLErrors)).to.be.undefined;
            const property = result.data.jcr.nodeByPath.property;
            expect(property.value, 'the editor reads the node').to.not.be.null;
            expect(property.decryptedValue).to.be.null;
        });
    });

    it('returns the decrypted value to a user granted the permission on the node', () => {
        cy.apolloClient(PERM_USER).apollo({query: readTitle}).should(result => {
            expect(result.graphQLErrors, JSON.stringify(result.graphQLErrors)).to.be.undefined;
            expect(result.data.jcr.nodeByPath.property.decryptedValue).to.equal(PLAIN_VALUE);
        });
    });

    ['/roles/server-administrator', '/roles/system-administrator', '/roles/system-administrator/root-access'].forEach(rolePath => {
        it(`grants the permission to ${rolePath} at module start`, () => {
            readPermissionNames(rolePath).should(result => {
                expect(result.data.jcr.nodeByPath.property.values).to.include('viewEncryptedValue');
            });
        });
    });
});
