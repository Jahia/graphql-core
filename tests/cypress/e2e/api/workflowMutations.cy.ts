import {addNode, addUserToGroup, createUser, deleteUser} from '@jahia/cypress';
import gql from 'graphql-tag';
import {grantUserRole} from '../../fixtures/acl';

type WorkflowMutation = {workflow: {startUser: string}; abortWorkflow?: boolean};

/**
 * `mutateWorkflows` and `abortWorkflow` require the admin mutation permission.
 *
 * Both sides are covered. The editor starts the workflow the other checks read, so its session reaches
 * the mutation root, and what it is refused afterwards is the workflow mutation itself.
 */
describe('workflow mutations', () => {
    const editor = 'workflowMutationEditor';
    const password = 'password';
    const definition = 'jBPM:2-step-publication';
    const contentPath = '/workflowMutationList';

    const startWorkflow = gql`
        mutation startWorkflow($path: String!, $definition: String!) {
            jcr {
                mutateNode(pathOrId: $path) {
                    startWorkflow(definition: $definition, language: "en")
                }
            }
        }
    `;
    const listWorkflows = gql`
        mutation listWorkflows($definition: String!) {
            mutateWorkflows(definition: $definition) {
                workflow {
                    startUser
                }
            }
        }
    `;
    const abortWorkflows = gql`
        mutation abortWorkflows($definition: String!) {
            mutateWorkflows(definition: $definition) {
                workflow {
                    startUser
                }
                abortWorkflow
            }
        }
    `;

    const asRoot = () => cy.apolloClient();
    const asEditor = () => cy.apolloClient({username: editor, password});
    const startedByEditor = (response: any): WorkflowMutation[] =>
        response.data.mutateWorkflows.filter((w: WorkflowMutation) => w.workflow.startUser.endsWith(`/${editor}`));

    before('Create an editor and a workflow started by that editor', () => {
        createUser(editor, password);
        addUserToGroup(editor, 'privileged');
        addNode({parentPathOrId: '/', name: 'workflowMutationList', primaryNodeType: 'jnt:contentList'});
        grantUserRole(contentPath, 'editor', editor);
        asEditor()
            .apollo({mutation: startWorkflow, variables: {path: contentPath, definition}})
            .should((response: any) => {
                expect(response.data.jcr.mutateNode.startWorkflow, 'the editor starts the workflow').to.be.true;
            });
    });

    after('Remove test data', () => {
        asRoot().apollo({mutation: abortWorkflows, variables: {definition}, errorPolicy: 'all'});
        asRoot().apollo({mutationFile: 'jcr/deleteNode.graphql', variables: {pathOrId: contentPath}});
        deleteUser(editor);
    });

    describe('a caller without the admin mutation permission', () => {
        it('does not list workflows', () => {
            asEditor()
                .apollo({mutation: listWorkflows, variables: {definition}, errorPolicy: 'all'})
                .should((response: any) => {
                    expect(response.errors, 'should contain a permission error').to.exist.and.not.be.empty;
                    expect(response.errors[0].message).to.contain('Permission denied');
                    expect(response.data?.mutateWorkflows).to.not.exist;
                });
        });

        it('does not abort workflows', () => {
            asEditor()
                .apollo({mutation: abortWorkflows, variables: {definition}, errorPolicy: 'all'})
                .should((response: any) => {
                    expect(response.errors, 'should contain a permission error').to.exist.and.not.be.empty;
                    expect(response.data?.mutateWorkflows).to.not.exist;
                });
            asRoot().apollo({mutation: listWorkflows, variables: {definition}}).should((response: any) => {
                expect(startedByEditor(response), 'the workflow is still running').to.have.length(1);
            });
        });
    });

    describe('a caller holding the admin mutation permission', () => {
        it('lists workflows', () => {
            asRoot().apollo({mutation: listWorkflows, variables: {definition}}).should((response: any) => {
                expect(startedByEditor(response)).to.have.length(1);
            });
        });

        it('aborts workflows', () => {
            asRoot().apollo({mutation: abortWorkflows, variables: {definition}}).should((response: any) => {
                const aborted = startedByEditor(response);
                expect(aborted).to.have.length(1);
                expect(aborted[0].abortWorkflow).to.be.true;
            });
            asRoot().apollo({mutation: listWorkflows, variables: {definition}}).should((response: any) => {
                expect(startedByEditor(response)).to.be.empty;
            });
        });
    });
});
