import gql from 'graphql-tag';
import {addNode, createSite, createUser, deleteSite, deleteUser, grantRoles} from '@jahia/cypress';

const SITE_NAME = 'decryptedValueSite';
const SITE_PATH = `/sites/${SITE_NAME}`;
const TEST_NODE = 'decryptedValueTestList';
const TEST_NODE_PATH = `${SITE_PATH}/contents/${TEST_NODE}`;
const TEST_ROLE = 'decryptedValueTestRole';
const PLAIN_VALUE = 'decrypted value test';

type TestUser = {username: string, password: string};
const user = (username: string): TestUser => ({username, password: `${username}Pwd1!`});

const EDITOR = user('decryptEditor');
const SITE_ADMIN = user('decryptSiteAdmin');
const NODE_GRANT = user('decryptNodeGrant');
const ROOT_GRANT = user('decryptRootGrant');
const SERVER_ADMIN = user('decryptServerAdmin');
const SYSTEM_ADMIN = user('decryptSystemAdmin');
const USERS = [EDITOR, SITE_ADMIN, NODE_GRANT, ROOT_GRANT, SERVER_ADMIN, SYSTEM_ADMIN];

const readTitle = gql`
    query {
        currentUser {
            name
        }
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

// Reads the title as the given user, and checks that the query ran as that user
const readTitleAs = (testUser: TestUser) => cy.apolloClient(testUser).apollo({query: readTitle}).then(result => {
    expect(result.graphQLErrors, JSON.stringify(result.graphQLErrors)).to.be.undefined;
    expect(result.data.currentUser.name).to.equal(testUser.username);
    const property = result.data.jcr.nodeByPath.property;
    expect(property.value, 'the user reads the node').to.not.be.null;
    return property.decryptedValue;
});

describe('Read the decrypted value of an encrypted property', () => {
    before('create an encrypted property, a role and the users', () => {
        deleteSite(SITE_NAME);
        removeLeftover(`/roles/${TEST_ROLE}`);
        USERS.forEach(testUser => deleteUser(testUser.username));

        createSite(SITE_NAME);
        addNode({parentPathOrId: `${SITE_PATH}/contents`, name: TEST_NODE, primaryNodeType: 'jnt:contentList'});
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

        // A role that adds the new permission alone, so that users differ by where it is granted
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

        USERS.forEach(testUser => createUser(testUser.username, testUser.password));
        grantRoles(SITE_PATH, ['editor'], EDITOR.username, 'USER');
        grantRoles(SITE_PATH, ['site-administrator'], SITE_ADMIN.username, 'USER');
        grantRoles(SITE_PATH, ['editor'], NODE_GRANT.username, 'USER');
        grantRoles(TEST_NODE_PATH, [TEST_ROLE], NODE_GRANT.username, 'USER');
        grantRoles(SITE_PATH, ['editor'], ROOT_GRANT.username, 'USER');
        grantRoles('/', [TEST_ROLE], ROOT_GRANT.username, 'USER');
        grantRoles('/', ['server-administrator'], SERVER_ADMIN.username, 'USER');
        grantRoles('/', ['system-administrator'], SYSTEM_ADMIN.username, 'USER');
    });

    after('remove the test data', () => {
        USERS.forEach(testUser => deleteUser(testUser.username));
        deleteSite(SITE_NAME);
        removeLeftover(`/roles/${TEST_ROLE}`);
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

    it('returns null to an editor of the site', () => {
        readTitleAs(EDITOR).should('be.null');
    });

    it('returns null to an administrator of the site', () => {
        readTitleAs(SITE_ADMIN).should('be.null');
    });

    it('returns null to a user granted the permission on the node only', () => {
        readTitleAs(NODE_GRANT).should('be.null');
    });

    it('returns the decrypted value to a user granted the permission on the repository root', () => {
        readTitleAs(ROOT_GRANT).should('equal', PLAIN_VALUE);
    });

    it('returns the decrypted value to a server administrator', () => {
        readTitleAs(SERVER_ADMIN).should('equal', PLAIN_VALUE);
    });

    it('returns the decrypted value to a system administrator', () => {
        readTitleAs(SYSTEM_ADMIN).should('equal', PLAIN_VALUE);
    });

    ['/roles/server-administrator', '/roles/system-administrator', '/roles/system-administrator/root-access'].forEach(rolePath => {
        it(`grants the permission to ${rolePath} at module start`, () => {
            readPermissionNames(rolePath).should(result => {
                expect(result.data.jcr.nodeByPath.property.values).to.include('viewEncryptedValue');
            });
        });
    });
});
