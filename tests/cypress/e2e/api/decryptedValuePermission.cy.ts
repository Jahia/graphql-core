import gql from 'graphql-tag';
import {addNode, createSite, createUser, deleteSite, deleteUser, grantRoles, publishAndWaitJobEnding} from '@jahia/cypress';

const SITE_NAME = 'decryptedValueSite';
const SITE_PATH = `/sites/${SITE_NAME}`;
const TEST_NODE = 'decryptedValueTestList';
const TEST_NODE_PATH = `${SITE_PATH}/contents/${TEST_NODE}`;
const TEST_ROLE = 'decryptedValueTestRole';
const PLAIN_VALUE = 'decrypted value test';
const PLAIN_VALUES = ['first decrypted value', 'second decrypted value'];

type TestUser = {username: string, password: string};
const user = (username: string): TestUser => ({username, password: `${username}Pwd1!`});

const EDITOR = user('decryptEditor');
const SITE_ADMIN = user('decryptSiteAdmin');
const NODE_GRANT = user('decryptNodeGrant');
const ROOT_GRANT = user('decryptRootGrant');
const SERVER_ADMIN = user('decryptServerAdmin');
const SYSTEM_ADMIN = user('decryptSystemAdmin');
const USERS = [EDITOR, SITE_ADMIN, NODE_GRANT, ROOT_GRANT, SERVER_ADMIN, SYSTEM_ADMIN];

type Workspace = 'EDIT' | 'LIVE';
type Decrypted = {value: string, values: string[]};

// Reads the single-valued jcr:title and the multi-valued j:keywords, both stored encrypted
const readFields = (workspace: Workspace) => gql`
    query {
        currentUser {
            name
        }
        jcr(workspace: ${workspace}) {
            nodeByPath(path: "${TEST_NODE_PATH}") {
                title: property(name: "jcr:title", language: "en") {
                    value
                    decryptedValue
                }
                keywords: property(name: "j:keywords") {
                    values
                    decryptedValues
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

// Reads both fields as the given user in the given workspace, and checks that the query ran as that user
const readAs = (testUser: TestUser, workspace: Workspace) => cy.apolloClient(testUser).apollo({query: readFields(workspace)}).then(result => {
    expect(result.graphQLErrors, JSON.stringify(result.graphQLErrors)).to.be.undefined;
    expect(result.data.currentUser.name).to.equal(testUser.username);
    const node = result.data.jcr.nodeByPath;
    expect(node.title.value, 'the user reads the node').to.not.be.null;
    return {value: node.title.decryptedValue, values: node.keywords.decryptedValues} as Decrypted;
});

const expectNothing = (decrypted: Decrypted) => {
    expect(decrypted.value).to.be.null;
    expect(decrypted.values).to.be.empty;
};

const expectPlain = (decrypted: Decrypted) => {
    expect(decrypted.value).to.equal(PLAIN_VALUE);
    expect(decrypted.values).to.deep.equal(PLAIN_VALUES);
};

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
                            addMixins(mixins: ["jmix:keywords"])
                            title: mutateProperty(name: "jcr:title") {
                                setValue(language: "en", value: "${PLAIN_VALUE}", option: ENCRYPTED)
                            }
                            keywords: mutateProperty(name: "j:keywords") {
                                setValues(values: ${JSON.stringify(PLAIN_VALUES)}, option: ENCRYPTED)
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
        publishAndWaitJobEnding(SITE_PATH);
    });

    after('remove the test data', () => {
        USERS.forEach(testUser => deleteUser(testUser.username));
        deleteSite(SITE_NAME);
        removeLeftover(`/roles/${TEST_ROLE}`);
    });

    it('stores the values encrypted', () => {
        cy.apolloClient().apollo({query: readFields('EDIT')}).should(result => {
            const node = result.data.jcr.nodeByPath;
            expect(node.title.value).to.not.equal(PLAIN_VALUE);
            expect(node.keywords.values).to.not.include(PLAIN_VALUES[0]);
        });
    });

    it('returns the decrypted values to root', () => {
        cy.apolloClient().apollo({query: readFields('EDIT')}).should(result => {
            const node = result.data.jcr.nodeByPath;
            expect(node.title.decryptedValue).to.equal(PLAIN_VALUE);
            expect(node.keywords.decryptedValues).to.deep.equal(PLAIN_VALUES);
        });
    });

    (['EDIT', 'LIVE'] as Workspace[]).forEach(workspace => {
        describe(`in the ${workspace} workspace`, () => {
            it('returns nothing to an editor of the site', () => {
                readAs(EDITOR, workspace).then(expectNothing);
            });

            it('returns nothing to an administrator of the site', () => {
                readAs(SITE_ADMIN, workspace).then(expectNothing);
            });

            it('returns nothing to a user granted the permission on the node only', () => {
                readAs(NODE_GRANT, workspace).then(expectNothing);
            });

            it('returns the decrypted values to a user granted the permission on the repository root', () => {
                readAs(ROOT_GRANT, workspace).then(expectPlain);
            });

            it('returns the decrypted values to a server administrator', () => {
                readAs(SERVER_ADMIN, workspace).then(expectPlain);
            });

            it('returns the decrypted values to a system administrator', () => {
                readAs(SYSTEM_ADMIN, workspace).then(expectPlain);
            });
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
