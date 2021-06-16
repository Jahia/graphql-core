/* eslint-disable @typescript-eslint/no-explicit-any */
import { DocumentNode } from 'graphql'
import { isValid } from 'date-fns'
import { apollo } from '../../../support/apollo'
import gql from 'graphql-tag'

describe('admin.configuration', () => {
    let createConfig: string

    before('load graphql file and create test dataset', () => {
        createConfig = require('../../../fixtures/admin/createConfig.json')
        cy.runProvisioningScript(createConfig)
    })

    it('Get flat properties from config', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: gql`
                    query x {
                        admin {
                            jahia {
                                configuration(pid: "org.jahia.test.config") {
                                    flatKeys
                                    flatProperties {
                                        key
                                        value
                                    }
                                }
                            }
                        }
                    }
                `,
            },
        ).should((response: any) => {
            expect(response.data.admin.jahia.configuration.flatKeys).to.contain('values.listObjects[0].A')
        })
    })
})
