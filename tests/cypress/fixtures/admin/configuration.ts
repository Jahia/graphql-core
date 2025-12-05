import gql from 'graphql-tag';
import Chainable = Cypress.Chainable;
import {ApolloQueryResult, FetchResult} from '@apollo/client';

const pid = 'org.jahia.modules.graphql.provider';

export const setGqlConfig = (name: string, value: string) : Chainable<ApolloQueryResult<any> | FetchResult> => {
    if (!value) {
        return removeGqlConfig(name);
    }

    return cy.apolloClient().apollo({
        mutation: gql`mutation setConfig($pid: String!, $name: String!, $value: String!) {
            admin {
                jahia {
                    configuration(pid: $pid identifier: "default") {
                        value(name: $name value: $value)
                    }
                }
            }
        }`,
        variables: {pid, name, value}
    });
};

export const removeGqlConfig = (name: string) : Chainable<ApolloQueryResult<any> | FetchResult> => {
    return cy.apolloClient().apollo({
        mutation: gql`mutation removeConfig($pid: String!, $name: String!) {
            admin {
                jahia {
                    configuration(pid: $pid identifier: "default") {
                        remove(name: $name)
                    }
                }
            }
        }`,
        variables: {pid, name}
    });
};

export const getGqlConfig = (name: string) : Chainable<string> => {
    return cy.apolloClient().apollo({
        query: gql`query getConfig($pid: String!, $name: String!) {
            admin {
                jahia {
                    configuration(pid: $pid, identifier: "default") {
                        value(name: $name)
                    }
                }
            }
        }`,
        variables: {pid, name}
    }).then(result => result?.data.admin.jahia.configuration.value);
};
