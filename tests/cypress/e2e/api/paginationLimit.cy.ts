import gql from 'graphql-tag';

describe('Pagination Limit test', () => {
    const sshCommands = [
        'config:list "(service.factoryPid=org.jahia.modules.graphql.provider)"'
    ];

    const waitUntilOptions = {
        interval: 250,
        timeout: 5000,
        errorMsg: 'Failed to verify configuration update'
    };

    const waitUntilTestFcnDisable = (response: string) => response.indexOf('graphql.fields.node.limit = 5000') !== -1;
    const waitUntilTestFcnEnable = (response: string) => response.indexOf('graphql.fields.node.limit = 100') !== -1;

    before('create nodes with structure', () => {
        // Setup data for testing
        console.log('run groovy script');
        cy.executeGroovy('groovy/preparePaginationNodeLimitTest.groovy', {});
    });
    after('clean up test data', () => {
        cy.apollo({
            mutation: gql`
                mutation {
                    jcr {
                        mutateNodes(pathsOrIds: ["/sites/systemsite/contents/paginationNodeTest"]) {
                            delete
                        }
                    }
                }
            `
        });
        cy.runProvisioningScript({script: { fileName: 'paginationLimit5000.json', type: 'application/json'}});
        cy.waitUntil(() => cy.task('sshCommand', sshCommands)
            .then(waitUntilTestFcnDisable), waitUntilOptions);
    });
    it('Finds all descendants nodes', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        rootNodes: nodesByPath(paths: ["/sites/systemsite/contents/paginationNodeTest"]) {
                            name
                            descendants(
                                typesFilter: { types: ["jnt:contentFolder","jmix:mainResource"] }
                            ) {
                                pageInfo {
                                    nodesCount
                                }
                            }
                        }
                    }
                }
           `
        }).then(response => {
            cy.log(JSON.stringify(response.data, null, 2));
            expect(response.data.jcr.rootNodes[0].descendants.pageInfo.nodesCount).to.equal(156);
        });
    });

    it('Finds all descendants nodes with limit', () => {
        cy.runProvisioningScript({script: { fileName: 'paginationLimit100.json', type: 'application/json'}});
        cy.waitUntil(() => cy.task('sshCommand', sshCommands)
            .then(waitUntilTestFcnEnable), waitUntilOptions);
        cy.apollo({
            query: gql`
                query {
                    jcr {
                        rootNodes: nodesByPath(paths: ["/sites/systemsite/contents/paginationNodeTest"]) {
                            name
                            descendants(
                                typesFilter: { types: ["jnt:contentFolder","jmix:mainResource"] }
                            ) {
                                pageInfo {
                                    nodesCount
                                }
                            }
                        }
                    }
                }
            `
        }).then(response => {
            cy.log(JSON.stringify(response.data, null, 2));
            expect(response.data.jcr.rootNodes[0].descendants.pageInfo.nodesCount).to.equal(100);
        });
    });
});
