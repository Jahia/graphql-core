/* eslint-disable @typescript-eslint/no-explicit-any */
import { apollo } from '../../../support/apollo'
import gql from 'graphql-tag'

describe('admin.configuration', () => {
    let createConfig: string

    before('load graphql file and create test dataset', () => {
        createConfig = require('../../../fixtures/admin/createConfig.json')
        cy.runProvisioningScript(createConfig)
    })

    function readConfig(variables) {
        return cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: gql`
                    query($configPid: String!, $configIdentifier: String, $value: String, $object: String, $objectValue: String, $objectList: String, $objectListValue:String) {
                        admin {
                            jahia {
                                configuration(pid: $configPid, identifier: $configIdentifier) {
                                    flatKeys @include(if: ${Boolean(variables.flat)})
                                    flatProperties @include(if: ${Boolean(variables.flat)}) {
                                        key value
                                    }
                                    keys
                                    value(name: $value) @include(if: ${Boolean(variables.value)})
                                    object(name: $object) @include(if: ${Boolean(variables.object)}) {
                                        keys
                                        value(name: $objectValue)
                                        list(name: $objectList) @include(if: ${Boolean(variables.objectList)}) {
                                            values
                                            objects {
                                                keys
                                                value(name: $objectListValue) @include(if: ${Boolean(
                                                    variables.objectListValue,
                                                )})
                                            }
                                        }

                                    }
                                }
                            }
                        }
                    }
                `,
                variables,
            },
        )
    }

    function editConfig(variables) {
        cy.apolloMutate(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                mutation: gql`
                    mutation($configPid: String!, $configIdentifier: String, $valueKey: String, $value: String, $object: String, $objectValueKey: String,  $objectValue: String, $objectList: String, $addListValue:String, $addListObjectValueKey: String, $addListObjectValue:String) {
                        admin {
                            jahia {
                                configuration(pid: $configPid, identifier: $configIdentifier) {
                                    value(name: $valueKey, value: $value) @include(if: ${Boolean(variables.valueKey)})
                                    mutateObject(name: $object) @include(if: ${Boolean(variables.object)}) {
                                        value(name: $objectValueKey, value: $objectValue) @include(if: ${Boolean(
                                            variables.objectValueKey,
                                        )})
                                        mutateList(name:$objectList) @include(if: ${Boolean(variables.objectList)}) {
                                            addValue(value: $addListValue) @include(if: ${Boolean(
                                                variables.addListValue,
                                            )})
                                            addObject @include(if: ${Boolean(variables.addListObjectValue)}) {
                                                value(name: $addListObjectValueKey, name: $addListObjectValue) 
                                            } 
                                        }
                                    }
                                }
                            }
                        }
                    }
                `,
                variables,
            },
        )
    }

    it('Get flat properties from config', () => {
        readConfig({ configPid: 'org.jahia.test.config', flat: true }).should((response: any) => {
            expect(response.data.admin.jahia.configuration.flatKeys).to.include.members(['object.listObjects[0].A'])
            expect(
                response.data.admin.jahia.configuration.flatProperties
                    .filter((c) => c.key === 'object.listObjects[0].A')
                    .map((c) => c.value)[0],
            ).to.eq('A0')
        })
    })

    it('Get properties from structured navigation', () => {
        readConfig({ configPid: 'org.jahia.test.config', value: 'value', object: 'object', objectValue: 'A' }).should(
            (response: any) => {
                expect(response.data.admin.jahia.configuration.keys).to.include.members(['list', 'value', 'object'])
                expect(response.data.admin.jahia.configuration.value).to.eq('test')
                expect(response.data.admin.jahia.configuration.object.keys).to.contain('A')
                expect(response.data.admin.jahia.configuration.object.value).to.eq('testA')
            },
        )
    })

    it('Get list of values', () => {
        readConfig({ configPid: 'org.jahia.test.config', object: 'object', objectList: 'list' }).should(
            (response: any) => {
                expect(response.data.admin.jahia.configuration.object.list.values).to.include.members([
                    'testObjectList0',
                    'testObjectList1',
                ])
            },
        )
    })

    it('Get list of objects', () => {
        readConfig({
            configPid: 'org.jahia.test.config',
            object: 'object',
            objectList: 'listObjects',
            objectListValue: 'A',
        }).should((response: any) => {
            response.data.admin.jahia.configuration.object.list.objects.forEach((obj) => {
                expect(obj.keys).to.include.members(['A', 'B'])
                expect(obj.value).to.be.oneOf(['A0', 'A1'])
            })
        })
    })

    it('Updates a property', () => {
        cy.apolloMutate(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                mutation: gql`
                    mutation {
                        admin {
                            jahia {
                                configuration(pid: "org.jahia.test.config") {
                                    updated: value(name: "value", value: "updatedValue")
                                    new: value(name: "newValue", value: "newValue")
                                }
                            }
                        }
                    }
                `,
            },
        )
        readConfig({ configPid: 'org.jahia.test.config', value: 'value' }).should((response: any) => {
            expect(response.data.admin.jahia.configuration.value).to.eq('updatedValue')
        })

        readConfig({ configPid: 'org.jahia.test.config', value: 'newValue' }).should((response: any) => {
            expect(response.data.admin.jahia.configuration.value).to.eq('newValue')
        })
    })

    it('creates a config', () => {
        editConfig({ configPid: 'org.jahia.test.config.new', valueKey: 'value', value: 'test-new-prop' })

        readConfig({ configPid: 'org.jahia.test.config.new', value: 'value' }).should((response: any) => {
            expect(response.data.admin.jahia.configuration.value).to.eq('test-new-prop')
        })
    })

    it('creates factory configs', () => {
        editConfig({
            configPid: 'org.jahia.test.config.new',
            configIdentifier: 'conf1',
            valueKey: 'value',
            value: 'test-new-prop-1',
        })
        editConfig({
            configPid: 'org.jahia.test.config.new',
            configIdentifier: 'conf2',
            valueKey: 'value',
            value: 'test-new-prop-2',
        })

        readConfig({ configPid: 'org.jahia.test.config.new', configIdentifier: 'conf1', value: 'value' }).should(
            (response: any) => {
                expect(response.data.admin.jahia.configuration.value).to.eq('test-new-prop-1')
            },
        )
        readConfig({ configPid: 'org.jahia.test.config.new', configIdentifier: 'conf2', value: 'value' }).should(
            (response: any) => {
                expect(response.data.admin.jahia.configuration.value).to.eq('test-new-prop-2')
            },
        )
    })

    it('creates object value', () => {
        editConfig({
            configPid: 'org.jahia.test.config.new',
            object: 'subObject',
            objectValueKey: 'value',
            objectValue: 'test-new-prop',
        })

        readConfig({
            configPid: 'org.jahia.test.config.new',
            flat: true,
            object: 'subObject',
            objectValue: 'value',
        }).should((response: any) => {
            expect(response.data.admin.jahia.configuration.flatKeys).to.include('subObject.value')
            expect(response.data.admin.jahia.configuration.object.value).to.eq('test-new-prop')
        })
    })

    it('creates list values', () => {
        editConfig({
            configPid: 'org.jahia.test.config.new',
            object: 'subObject',
            objectList: 'list',
            addListValue: 'val1',
        })
        editConfig({
            configPid: 'org.jahia.test.config.new',
            object: 'subObject',
            objectList: 'list',
            addListValue: 'val2',
        })

        readConfig({
            configPid: 'org.jahia.test.config.new',
            flat: true,
            object: 'subObject',
            objectList: 'list',
        }).should((response: any) => {
            expect(response.data.admin.jahia.configuration.flatKeys).to.include.members([
                'subObject.list[0]',
                'subObject.list[1]',
            ])
            expect(response.data.admin.jahia.configuration.object.list.values).to.include.members(['val1', 'val2'])
        })
    })
})
