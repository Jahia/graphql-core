import { ApolloClient, NormalizedCacheObject } from '@apollo/client/core'

import { apolloClient } from '../utils/graphql/apolloClient'

interface authMethod {
    token?: string
    username?: string
    password?: string
    jsessionid?: string
}

interface params {
    baseUrl?: string
    authMethod?: authMethod
}

export const apollo = (params?: params): ApolloClient<NormalizedCacheObject> => {
    const baseUrl = params !== undefined && params.baseUrl !== undefined ? params.baseUrl : null
    const authMethod = params !== undefined && params.authMethod !== undefined ? params.authMethod : null

    console.log(authMethod)
    const updatedBaseUrl = baseUrl === null ? Cypress.config().baseUrl : baseUrl
    const updatedAuthMethod =
        authMethod === null
            ? {
                  username: 'root',
                  password: Cypress.env('SUPER_USER_PASSWORD'),
              }
            : authMethod

    return apolloClient(updatedBaseUrl, updatedAuthMethod)
}
