import gql from 'graphql-tag';

describe('WIP test', () => {
    it('should be able to delete wip content', () => {
        cy.apollo({
            mutation: gql`
                mutation m1 {
                    jcr1: jcr {
                        addNode(
                            parentPathOrId: "/"
                            primaryNodeType: "jnt:contentList"
                            name: "test-list"
                        ) {
                            addChild(name: "test-node", primaryNodeType: "jnt:bigText") {
                                uuid
                            }
                            publish(publishSubNodes: true)
                            uuid
                        }
                    }
                }
            `
        });

        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(1000);

        cy.apollo({
            query: gql`
                query q {
                    jcr {
                        nodeByPath(path: "/test-list") {
                            uuid
                        }
                    }
                }
            `
        }).should(result => {
            expect(result?.data?.jcr?.nodeByPath).not.to.be.null;
        });

        cy.apollo({
            mutation: gql`
                mutation m1 {
                    jcr2: jcr {
                        mutateNode(pathOrId: "/test-list/test-node") {
                            mutateProperty(name: "text") {
                                setValue(language: "en", value: "test")
                            }
                            createWipInfo(wipInfo: { status: ALL_CONTENT, languages: ["en"] })
                        }
                    }

                    jcr3: jcr {
                        mutateNode(pathOrId: "/test-list") {
                            markForDeletion
                            publish
                        }
                    }
                }
            `
        });

        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.wait(1000);

        cy.apollo({
            errorPolicy: 'all',
            query: gql`
                query q {
                    jcr {
                        nodeByPath(path: "/test-list") {
                            uuid
                        }
                    }
                }
            `
        }).should(result => {
            expect(Boolean(result?.data?.jcr?.nodeByPath)).to.be.false;
        });
    });
});
