import { apollo } from '../../support/apollo'
import { DocumentNode } from 'graphql'

describe('Validate ability get current User', () => {
    let GQL_APIUSER: DocumentNode

    before('load graphql file', function () {
        GQL_APIUSER = require(`graphql-tag/loader!../../fixtures/getApiUser.graphql`)
    })

    it('Get Current user for Authenticated user (jay)', async function () {
        const response = await apollo({ authMethod: { username: 'jay', password: 'password' } }).query({
            query: GQL_APIUSER,
        })
        expect(response.errors).to.be.undefined
        expect(response.data.currentUser.name).to.equal('jay')
    })

    it('Get Current user for Authenticated user (root)', async function () {
        const response = await apollo({
            authMethod: {
                username: 'root',
                password: Cypress.env('SUPER_USER_PASSWORD'),
            },
        }).query({
            query: GQL_APIUSER,
        })
        expect(response.errors).to.be.undefined
        expect(response.data.currentUser.name).to.equal('root')
    })
})
