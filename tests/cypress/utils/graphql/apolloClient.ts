// cross-fetch is needed to support request executed by the browser or by node
import fetch from 'cross-fetch'
import { ApolloClient, InMemoryCache, ApolloLink, from, NormalizedCacheObject } from '@apollo/client/core'
import { setContext } from '@apollo/client/link/context'
import { onError } from '@apollo/client/link/error'
import { HttpLink } from '@apollo/client/link/http'

interface authMethod {
    token?: string
    username?: string
    password?: string
    jsessionid?: string
}

interface httpHeaders {
    authorization?: string
    Cookie?: string
}

export const apolloClient = (baseUrl: string, authMethod?: authMethod): ApolloClient<NormalizedCacheObject> => {
    const httpLink = new HttpLink({
        uri: `${baseUrl}/modules/graphql`,
        fetch,
    })

    const authHeaders: httpHeaders = {}
    if (authMethod === undefined) {
        console.log('Performing GraphQL query as guest')
    } else if (authMethod.token !== undefined) {
        console.log(`Performing GraphQL query using API Token`)
        authHeaders.authorization = `APIToken ${authMethod.token}`
    } else if (authMethod.username !== undefined && authMethod.password !== undefined) {
        console.log(`Performing GraphQL query as ${authMethod.username}`)
        authHeaders.authorization = `Basic ${Buffer.from(
            authMethod.username + ':' + authMethod.password,
            'binary',
        ).toString('base64')}`
    } else if (authMethod.jsessionid !== undefined) {
        console.log(`Performing GraphQL query using JSESSIONID`)
        authHeaders.Cookie = 'JSESSIONID=' + authMethod.jsessionid
    }

    const authLink = setContext((_, { headers }) => {
        return {
            headers: {
                ...headers,
                ...authHeaders,
            },
        }
    })

    const errorLink = onError(({ graphQLErrors, networkError }) => {
        if (graphQLErrors)
            graphQLErrors.map(({ message, locations, path }) =>
                console.log(`[GraphQL error]: Message: ${message}, Location: ${locations}, Path: ${path}`),
            )
        if (networkError) console.log(`[Network error]: ${networkError}`)
    })

    return new ApolloClient({
        cache: new InMemoryCache(),
        link: from([(authLink as unknown) as ApolloLink, errorLink, httpLink]),
        defaultOptions: {
            query: {
                fetchPolicy: 'network-only',
            },
            watchQuery: {
                fetchPolicy: 'network-only',
            },
        },
    })
}
