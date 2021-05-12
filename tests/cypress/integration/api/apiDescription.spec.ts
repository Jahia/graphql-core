import { apollo } from '../../support/apollo'
import gql from 'graphql-tag'

describe('Test if every type in graphQL API has description', () => {
    it('Check every input for the User Type', async function () {
        const types = new Set()
        const noDesc = new Set()
        const invalidNames = new Set()
        await executeTest('Query', types, noDesc, invalidNames)

        const noDescBlacklist = [
            'type=JCRSite/field=findAvailableNodeName/arg=nodeType',
            'type=JCRSite/field=findAvailableNodeName/arg=language',
            'type=VanityUrl/field=findAvailableNodeName/arg=nodeType',
            'type=VanityUrl/field=findAvailableNodeName/arg=language',
            'type=GqlDashboard',
            'type=GqlModule',
            'type=GqlEditorForms',
            'type=EditorForm',
            'type=EditorFormSection',
            'type=EditorFormFieldSet',
            'type=EditorFormField',
            'type=EditorFormFieldValue',
            'type=EditorFormProperty',
            'type=EditorFormFieldValueConstraint',
            'type=InputContextEntryInput',
            'inputType=InputContextEntryInput/field=key',
            'inputType=InputContextEntryInput/field=value',
            'type=Query/field=categoryById/arg=id',
            'type=Metadata/field=uuid',
            'type=Metadata/field=path',
            'type=Category/field=uuid',
            'type=Category/field=path',
            'type=Query/field=categoryByPath/arg=path',
        ]
        noDescBlacklist.forEach((n) => noDesc.delete(n))

        const invalidNameBlacklist = ['wipInfo']
        invalidNameBlacklist.forEach((n) => invalidNames.delete(n))

        expect(JSON.stringify(Array.from(noDesc))).to.equals('[]')
        expect(JSON.stringify(Array.from(invalidNames))).to.equals('[]')
    })
})

// Test to go down the AST of GraphQL to check for descriptions
const executeTest = async (typeName, types, noDesc, invalidNames) => {
    if (types.has(typeName)) {
        return
    }

    if (typeName[0] !== typeName[0].toUpperCase()) {
        invalidNames.add(typeName)
    }

    types.add(typeName)

    const query = constructQuery(typeName)
    const client = apollo()
    const response = await client.query({ query })
    const responseDataType = response.data.__type
    if (responseDataType === null || responseDataType === undefined || responseDataType.kind === 'UNION') {
        return
    }

    if (!responseDataType.description) {
        noDesc.add('type=' + responseDataType.name)
    }

    if (responseDataType.fields) {
        await asyncForEach(responseDataType.fields, async (field) => {
            if (field.args) {
                await asyncForEach(field.args, async (arg) => {
                    await fieldCheck(
                        'type=' + responseDataType.name + '/field=' + field.name + '/arg=' + arg.name,
                        arg,
                        types,
                        noDesc,
                        invalidNames,
                    )
                })
            }

            await fieldCheck(
                'type=' + responseDataType.name + '/field=' + field.name,
                field,
                types,
                noDesc,
                invalidNames,
            )
        })
    }

    if (responseDataType.inputFields) {
        await asyncForEach(responseDataType.inputFields, async (field) => {
            if (field.args) {
                await asyncForEach(field.args, async (arg) => {
                    await fieldCheck(
                        'inputType=' + responseDataType.name + '/arg=' + arg.name,
                        arg,
                        types,
                        noDesc,
                        invalidNames,
                    )
                })
            }

            await fieldCheck(
                'inputType=' + responseDataType.name + '/field=' + field.name,
                field,
                types,
                noDesc,
                invalidNames,
            )
        })
    }
}

const fieldCheck = async (message, field, types, noDesc, invalidNames) => {
    if (field.description === null) {
        noDesc.add(message)
    }

    let type = field.type

    while (type.ofType) {
        type = type.ofType
    }

    if (type.kind === 'OBJECT' || type.kind === 'INPUT_OBJECT') {
        await executeTest(type.name, types, noDesc, invalidNames)
    }
}

const asyncForEach = async (array, callback) => {
    for (let index = 0; index < array.length; index++) {
        // eslint-disable-next-line no-await-in-loop
        await callback(array[index], index, array)
    }
}

const constructQuery = (typeName) => {
    return gql`query IntrospectionQuery {
        __type(name:"${typeName}") {
            kind
            name
            description
            fields {
                name
                description
                args {
                    name
                    description
                    type {
                        kind
                        name
                        description
                        ofType {
                            kind
                            name
                            description
                        }
                    }
                }
                type {
                    kind
                    name
                    description
                    ofType {
                        kind
                        name
                        description
                    }
                }
            }
            inputFields {
                name
                description
                type {
                    kind
                    name
                    description
                    ofType {
                        kind
                        name
                        description
                        ofType {
                            kind
                            name
                        }
                    }
                }
            }
        }
    }`
}
