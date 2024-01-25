import gql from 'graphql-tag';
import {addNode} from '@jahia/cypress';

let nodeUuid = 'null';
const nodeTitleFR = 'text FR';
const nodeTitleEN = 'text EN';

describe('Test GraphQL Properties', () => {
    before('setup list with some properties', () => {
        addNode({
            parentPathOrId: '/',
            name: 'testList',
            primaryNodeType: 'jnt:contentList',
            mixins: ['jmix:liveProperties'],
            properties: [
                {name: 'jcr:title', language: 'en', value: nodeTitleEN},
                {name: 'jcr:title', language: 'fr', value: nodeTitleFR},
                {name: 'j:liveProperties', values: ['liveProperty1', 'liveProperty2']}
            ]
        }).then(result => {
            nodeUuid = result.data.jcr.addNode.uuid;
        });
    });

    it('Should retrieve property with basic fields', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr(workspace: EDIT) {
                        nodeByPath(path: "/testList") {
                            property(name: "jcr:uuid") {
                                name,
                                type,
                                node {
                                    path
                                }
                            }
                        }
                    }
                }
            `
        }).should(result => {
            const property = result?.data?.jcr?.nodeByPath?.property;
            expect(property).to.have.property('name', 'jcr:uuid');
            expect(property).to.have.property('type', 'STRING');
            expect(property.node).to.have.property('path', '/testList');
        });
    });

    it('Should retrieve NON internationalized property NOT passing language', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr(workspace: EDIT) {
                        nodeByPath(path: "/testList") {
                            property(name: "jcr:uuid") {
                                internationalized,
                                language,
                                value,
                                values
                            }
                        }
                    }
                }
            `
        }).should(result => {
            const property = result?.data?.jcr?.nodeByPath?.property;
            expect(property).to.have.property('internationalized', false);
            expect(property).to.have.property('language', null);
            expect(property).to.have.property('value', nodeUuid);
            expect(property).to.have.property('values', null);
        });
    });

    it('Should retrieve NON internationalized property passing language', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr(workspace: EDIT) {
                        nodeByPath(path: "/testList") {
                            property(name: "jcr:uuid", language: "en") {
                                internationalized,
                                language,
                                value,
                                values
                            }
                        }
                    }
                }
            `
        }).should(result => {
            const property = result?.data?.jcr?.nodeByPath?.property;
            expect(property).to.have.property('internationalized', false);
            expect(property).to.have.property('language', null);
            expect(property).to.have.property('value', nodeUuid);
            expect(property).to.have.property('values', null);
        });
    });

    it('Should NOT retrieve internationalized property NOT passing language', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr(workspace: EDIT) {
                        nodeByPath(path: "/testList") {
                            property(name: "jcr:title") {
                                internationalized,
                                language,
                                value,
                                values
                            }
                        }
                    }
                }
            `
        }).should(result => {
            const property = result?.data?.jcr?.nodeByPath?.property;
            expect(property).null;
        });
    });

    it('Should retrieve internationalized property passing language', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr(workspace: EDIT) {
                        nodeByPath(path: "/testList") {
                            property(name: "jcr:title", language: "fr") {
                                internationalized,
                                language,
                                value,
                                values
                            }
                        }
                    }
                }
            `
        }).should(result => {
            const property = result?.data?.jcr?.nodeByPath?.property;
            expect(property).to.have.property('internationalized', true);
            expect(property).to.have.property('language', 'fr');
            expect(property).to.have.property('value', nodeTitleFR);
            expect(property).to.have.property('values', null);
        });
    });

    it('Should retrieve NON internationalized properties NOT passing language', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr(workspace: EDIT) {
                        nodeByPath(path: "/testList") {
                            properties(names: ["jcr:uuid", "jcr:title"]) {
                                name,
                                type,
                                internationalized,
                                language,
                                value,
                                values,
                                node {
                                    path
                                }
                            }
                        }
                    }
                }
            `
        }).should(result => {
            const properties = result?.data?.jcr?.nodeByPath?.properties;
            expect(properties).to.have.length(1);
            expect(properties[0]).to.have.property('name', 'jcr:uuid');
            expect(properties[0]).to.have.property('type', 'STRING');
            expect(properties[0].node).to.have.property('path', '/testList');
            expect(properties[0]).to.have.property('internationalized', false);
            expect(properties[0]).to.have.property('language', null);
            expect(properties[0]).to.have.property('value', nodeUuid);
            expect(properties[0]).to.have.property('values', null);
        });
    });

    it('Should retrieve internationalized and NON internationalized properties passing language', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr(workspace: EDIT) {
                        nodeByPath(path: "/testList") {
                            properties(names: ["jcr:uuid", "jcr:title"], language: "en") {
                                name,
                                type,
                                internationalized,
                                language,
                                value,
                                values,
                                node {
                                    path
                                }
                            }
                        }
                    }
                }
            `
        }).should(result => {
            const properties = result?.data?.jcr?.nodeByPath?.properties;
            expect(properties).to.have.length(2);

            const uuid = properties.find(p => p.name === 'jcr:uuid');
            expect(uuid).not.null;
            expect(uuid).to.have.property('type', 'STRING');
            expect(uuid.node).to.have.property('path', '/testList');
            expect(uuid).to.have.property('internationalized', false);
            expect(uuid).to.have.property('language', null);
            expect(uuid).to.have.property('value', nodeUuid);
            expect(uuid).to.have.property('values', null);

            const title = properties.find(p => p.name === 'jcr:title');
            expect(title).not.null;
            expect(title).to.have.property('type', 'STRING');
            expect(title.node).to.have.property('path', '/testList');
            expect(title).to.have.property('internationalized', true);
            expect(title).to.have.property('language', 'en');
            expect(title).to.have.property('value', nodeTitleEN);
            expect(title).to.have.property('values', null);
        });
    });

    it('Should retrieve all properties passing language', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr(workspace: EDIT) {
                        nodeByPath(path: "/testList") {
                            properties(language: "fr") {
                                name
                            }
                        }
                    }
                }
            `
        }).should(result => {
            const properties = result?.data?.jcr?.nodeByPath?.properties;
            expect(properties).to.have.length(15);

            expect(properties.find(p => p.name === 'j:liveProperties')).not.null;
            expect(properties.find(p => p.name === 'j:nodename')).not.null;
            expect(properties.find(p => p.name === 'j:originWS')).not.null;
            expect(properties.find(p => p.name === 'jcr:baseVersion')).not.null;
            expect(properties.find(p => p.name === 'jcr:created')).not.null;
            expect(properties.find(p => p.name === 'jcr:createdBy')).not.null;
            expect(properties.find(p => p.name === 'jcr:isCheckedOut')).not.null;
            expect(properties.find(p => p.name === 'jcr:lastModified')).not.null;
            expect(properties.find(p => p.name === 'jcr:lastModifiedBy')).not.null;
            expect(properties.find(p => p.name === 'jcr:mixinTypes')).not.null;
            expect(properties.find(p => p.name === 'jcr:predecessors')).not.null;
            expect(properties.find(p => p.name === 'jcr:primaryType')).not.null;
            expect(properties.find(p => p.name === 'jcr:uuid')).not.null;
            expect(properties.find(p => p.name === 'jcr:versionHistory')).not.null;
            expect(properties.find(p => p.name === 'jcr:title')).not.null;
        });
    });

    it('Should retrieve multivalued property', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr(workspace: EDIT) {
                        nodeByPath(path: "/testList") {
                            property(name: "j:liveProperties") {
                                value,
                                values
                            }
                        }
                    }
                }
            `
        }).should(result => {
            const property = result?.data?.jcr?.nodeByPath?.property;
            expect(property.value).null;
            expect(property.values).to.have.length(2);
            expect(property.values.find(v => v === 'liveProperty1')).not.null;
            expect(property.values.find(v => v === 'liveProperty2')).not.null;
        });
    });

    after('Delete list', function () {
        cy.apollo({
            mutationFile: 'jcr/deleteNode.graphql',
            variables: {
                pathOrId: '/testList'
            }
        });
    });
});
