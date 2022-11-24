/* eslint-disable @typescript-eslint/no-explicit-any */

describe('Get properties graphql test', () => {
    const textValueEnglish = 'text EN'
beforeEach("reset default value", ()=> {
    cy.apollo({
        mutationFile: 'jcr/mutateNode.graphql',
        variables: {
            pathOrId: '/sites/digitall',
            properties: [{name: 'j:mixLanguage', value: false}]
        },
    })
})
    before('load graphql file and create nodes', () => {
        cy.apollo({
            mutationFile: 'jcr/addNode.graphql',
            variables: {
                parentPathOrId: '/sites/digitall/home/area-main',
                nodeName: 'simple-text',
                nodeType: 'jnt:text',
                properties: [
                    { name: 'text', value: textValueEnglish, language: 'en' },
                ]
            },
        })
    })

    it('Get translated node with default language with j:mixLanguage set to true on site', () => {
        cy.apollo({
            mutationFile: 'jcr/mutateNode.graphql',
            variables: {
                pathOrId: '/sites/digitall',
                properties: [{name: 'j:mixLanguage', value: true}]
            },
        })

        cy.apollo({
            queryFile: 'jcr/nodeByPath.graphql',
            variables: {
                path: '/sites/digitall/home/area-main/simple-text',
                language: 'fr',
                useFallbackLanguage: true
            },
        }).should((response: any) => {
            expect(response.data.jcr.nodeByPath).to.exist
            const textProperty = response.data.jcr.nodeByPath.properties.find(property => property.name === 'text')
            expect(textProperty.value).to.equal(textValueEnglish)
        })

        cy.apollo({
            queryFile: 'jcr/nodeByPath.graphql',
            variables: {
                path: '/sites/digitall/home/area-main/simple-text',
                language: 'fr',
                useFallbackLanguage: false
            },
        }).should((response: any) => {
            expect(response.data.jcr.nodeByPath).to.exist
            const textProperty = response.data.jcr.nodeByPath.properties.find(property => property.name === 'text')
            expect(textProperty).to.be.undefined
        })
    })

    it('Get node should not have translated value', () => {

        cy.apollo({
            queryFile: 'jcr/nodeByPath.graphql',
            variables: {
                path: '/sites/digitall/home/area-main/simple-text',
                language: 'fr',
                useFallbackLanguage: false
            },
        }).should((response: any) => {
            expect(response.data.jcr.nodeByPath).to.exist
            const textProperty = response.data.jcr.nodeByPath.properties.find(property => property.name === 'text')
            expect(textProperty).to.be.undefined
        })

        cy.apollo({
            queryFile: 'jcr/nodeByPath.graphql',
            variables: {
                path: '/sites/digitall/home/area-main/simple-text',
                language: 'fr',
                useFallbackLanguage: true
            },
        }).should((response: any) => {
            expect(response.data.jcr.nodeByPath).to.exist
            const textProperty = response.data.jcr.nodeByPath.properties.find(property => property.name === 'text')
            expect(textProperty).to.be.undefined
        })
    })

    it('Get node should have translated value with requested language', () => {
        cy.apollo({
            queryFile: 'jcr/nodeByPath.graphql',
            variables: {
                path: '/sites/digitall/home/area-main/simple-text',
                language: 'en',
                useFallbackLanguage: false
            },
        }).should((response: any) => {
            expect(response.data.jcr.nodeByPath).to.exist
            const textProperty = response.data.jcr.nodeByPath.properties.find(property => property.name === 'text')
            expect(textProperty.value).to.equal(textValueEnglish)
        })

        cy.apollo({
            queryFile: 'jcr/nodeByPath.graphql',
            variables: {
                path: '/sites/digitall/home/area-main/simple-text',
                language: 'en',
                useFallbackLanguage: true
            },
        }).should((response: any) => {
            expect(response.data.jcr.nodeByPath).to.exist
            const textProperty = response.data.jcr.nodeByPath.properties.find(property => property.name === 'text')
            expect(textProperty.value).to.equal(textValueEnglish)
        })
    })
    after('Delete created node', function () {
        cy.apollo({
            mutationFile: 'jcr/deleteNode.graphql',
            variables: {
                pathOrId: '/sites/digitall/home/area-main/simple-text',
            },
        })
    })
})
