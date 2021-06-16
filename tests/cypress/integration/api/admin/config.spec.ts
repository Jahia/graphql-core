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
                    query {
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
            expect(response.data.admin.jahia.configuration.flatKeys).to.include.members(['values.listObjects[0].A'])
            expect(
                response.data.admin.jahia.configuration.flatProperties
                    .filter((c) => c.key === 'values.listObjects[0].A')
                    .map((c) => c.value)[0],
            ).to.eq('A0')
        })
    })

    it('Get properties from structured navigation', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: gql`
                    query {
                        admin {
                            jahia {
                                configuration(pid: "org.jahia.test.config") {
                                    keys
                                    value(name: "value")
                                    object(name: "values") {
                                        keys
                                        value(name: "A")
                                    }
                                }
                            }
                        }
                    }
                `,
            },
        ).should((response: any) => {
            expect(response.data.admin.jahia.configuration.keys).to.include.members(['value', 'values'])
            expect(response.data.admin.jahia.configuration.value).to.eq('test')
            expect(response.data.admin.jahia.configuration.object.keys).to.contain('A')
            expect(response.data.admin.jahia.configuration.object.value).to.eq('testA')
        })
    })

    it('Get list of values', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: gql`
                    query {
                        admin {
                            jahia {
                                configuration(pid: "org.jahia.test.config") {
                                    object(name: "values") {
                                        list(name: "list") {
                                            values
                                        }
                                    }
                                }
                            }
                        }
                    }
                `,
            },
        ).should((response: any) => {
            expect(response.data.admin.jahia.configuration.object.list.values).to.include.members(['test0', 'test1'])
        })
    })

    it('Get list of objects', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: gql`
                    query {
                        admin {
                            jahia {
                                configuration(pid: "org.jahia.test.config") {
                                    object(name: "values") {
                                        list(name: "listObjects") {
                                            objects {
                                                keys
                                                value(name: "A")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                `,
            },
        ).should((response: any) => {
            response.data.admin.jahia.configuration.object.list.objects.forEach((obj) => {
                expect(obj.keys).to.include.members(['A', 'B'])
                expect(obj.value).to.be.oneOf(['A0', 'A1'])
            })
        })
    })
})
