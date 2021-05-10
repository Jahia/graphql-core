import { DocumentNode } from 'graphql'

import { apolloClient } from '../utils/graphql/apolloClient'

interface authMethod {
    token?: string
    username?: string
    password?: string
    jsessionid?: string
}

interface params {
    authMethod?: authMethod
    baseUrl: string
    query: DocumentNode
    variables: Record<string, unknown>
    mode?: 'mutate' | 'query'
}

// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export const apolloNode = async (params: params) => {
    const { baseUrl, authMethod, query, variables, mode } = params

    const aClient = apolloClient(baseUrl, authMethod)

    if (mode === 'mutate') {
        let response
        try {
            response = await aClient.mutate({
                mutation: query,
                variables: variables,
            })
        } catch (e) {
            return e
        }
        return response
    }

    return await aClient.query({
        query: query,
        variables: variables,
    })
}

module.exports = apolloNode
