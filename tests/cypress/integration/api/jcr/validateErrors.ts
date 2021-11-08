import { ApolloQueryResult, FetchResult } from '@apollo/client'

export function validateError(
    result: ApolloQueryResult<any> | FetchResult<{ [p: string]: any }, Record<string, any>, Record<string, any>>,
    expectedError: string,
) {
    validateErrors(result, [expectedError])
}

export function validateErrors(
    result: ApolloQueryResult<any> | FetchResult<{ [p: string]: any }, Record<string, any>, Record<string, any>>,
    expectedErrors: string[],
) {
    const errors = result?.errors
    expect(errors).to.exist
    expect(errors).to.have.length(expectedErrors.length)

    for (let i = 0, l = errors.length; i < l; i++) {
        expect(errors[i]?.message).to.contain(expectedErrors[i])
    }
}
