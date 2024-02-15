import {addNode, addVanityUrl, createSite, deleteNode, deleteSite} from '@jahia/cypress';

const sitename = 'graphql_test_render';
describe('Test graphql rendering', () => {
    before('Create a site', () => {
        createSite(sitename);
    });

    after('Delete the site', () => {
        deleteSite(sitename);
    });

    it('Check if node (page) is displayable', () => {
        addNode({
            parentPathOrId: '/sites/' + sitename + '/home',
            name: 'page1',
            primaryNodeType: 'jnt:page',
            properties: [
                {name: 'jcr:title', value: 'page1'},
                {name: 'j:templateName', value: 'simple'}
            ]
        });
        cy.apollo({
            queryFile: 'jcr/displayableNode.graphql',
            variables: {
                path: '/sites/' + sitename + '/home/page1'
            }
        }).should(result => {
            const property = result?.data?.jcr?.nodeByPath?.isDisplayableNode;
            expect(property).true;
        });
        deleteNode('/sites/' + sitename + '/home/page1');
    });

    it('Check if node (list) is NOT displayable', () => {
        addNode({
            parentPathOrId: '/sites/' + sitename + '/home',
            name: 'listA',
            primaryNodeType: 'jnt:containerList'
        });
        cy.apollo({
            queryFile: 'jcr/displayableNode.graphql',
            variables: {
                path: '/sites/' + sitename + '/home/listA'
            }
        }).should(result => {
            const property = result?.data?.jcr?.nodeByPath?.isDisplayableNode;
            expect(property).false;
        });
        deleteNode('/sites/' + sitename + '/home/listA');
    });

    it('Check output of renderedContent for node with richtext', () => {
        addNode({
            parentPathOrId: '/sites/' + sitename + '/home',
            name: 'news1',
            primaryNodeType: 'gqltest:news',
            properties: [
                {name: 'title', language: 'en', value: 'My Test News One'},
                {name: 'description', values: [
                    '<p>Richtext1</p>\\n\\n<p>Back to <a href=\\"/cms/{mode}/{lang}/sites/' + sitename + '/home.html\\">Home</a></p>\\n',
                    '<p>Richtext2</p>\\n\\n<p>Go to <a href=\\"/cms/{mode}/{lang}/sites/' + sitename + '/other.html\\">Other</a></p>\\n'
                ]}
            ]
        });
        cy.apollo({
            queryFile: 'jcr/nodeRenderedContent.graphql',
            variables: {
                path: '/sites/' + sitename + '/home/news1',
                view: 'html'
            }
        }).should(result => {
            const output = result?.data?.jcr?.nodeByPath?.renderedContent.output;
            expect(output).not.contains('{lang}');
            expect(output).not.contains('{workspace}');
            expect(output).not.contains('{mode}');
        });
        deleteNode('/sites/' + sitename + '/home/news1');
    });

    it('Check if property renderedValue is populated and filtered correctly', () => {
        addNode({
            parentPathOrId: '/sites/' + sitename + '/home',
            name: 'news1',
            primaryNodeType: 'gqltest:news',
            properties: [
                {name: 'title', language: 'en', value: 'My Test News One'},
                {name: 'description', values: [
                    '<p>Richtext1</p>\\n\\n<p>Back to <a href=\\"/cms/{mode}/{lang}/sites/' + sitename + '/home.html\\">Home</a></p>\\n',
                    '<p>Richtext2</p>\\n\\n<p>Go to <a href=\\"/cms/{mode}/{lang}/sites/' + sitename + '/other.html\\">Other</a></p>\\n'
                    ]},
                {name: 'author', value: 'Sheldon'},
                {name: 'author_bio', language: 'en', value: 'Sheldon Lee Cooper, Ph.D., Sc.D., is a fictional character in the CBS television' +
                        ' series The Big Bang Theory and its spinoff series Young Sheldon, portrayed by actors Jim Parsons and Iain ' +
                        'Armitage respectively (with Parsons as the latter series\' narrator).'}
            ]
        });
        addVanityUrl(
            '/cms/{mode}/{lang}/sites/' + sitename + '/home.html',
            'en',
            '/welcome'
        );
        cy.apollo({
            queryFile: 'jcr/propertyRenderedValue.graphql',
            variables: {
                path: '/sites/' + sitename + '/home/news1',
                property: 'title',
                language: 'en'
            }
        }).should(result => {
            const propertyName = result?.data?.jcr?.nodeByPath?.property.name;
            const propertyValue = result?.data?.jcr?.nodeByPath?.property.value;
            const propertyValues = result?.data?.jcr?.nodeByPath?.property.values;
            const propertyRenderedValue = result?.data?.jcr?.nodeByPath?.property.renderedValue;
            const propertyRenderedValues = result?.data?.jcr?.nodeByPath?.property.renderedValues;
            expect(propertyName).eq('title');
            expect(propertyValue).not.null;
            expect(propertyValues).null;
            expect(propertyRenderedValue).null;
            expect(propertyRenderedValues).null;
        });
        cy.apollo({
            queryFile: 'jcr/propertyRenderedValue.graphql',
            variables: {
                path: '/sites/' + sitename + '/home/news1',
                property: 'jcr:uuid',
                language: 'en'
            }
        }).should(result => {
            const propertyName = result?.data?.jcr?.nodeByPath?.property.name;
            const propertyValue = result?.data?.jcr?.nodeByPath?.property.value;
            const propertyValues = result?.data?.jcr?.nodeByPath?.property.values;
            const propertyRenderedValue = result?.data?.jcr?.nodeByPath?.property.renderedValue;
            const propertyRenderedValues = result?.data?.jcr?.nodeByPath?.property.renderedValues;
            expect(propertyName).eq('jcr:uuid');
            expect(propertyValue).not.null;
            expect(propertyValues).null;
            expect(propertyRenderedValue).null;
            expect(propertyRenderedValues).null;
        });
        cy.apollo({
            queryFile: 'jcr/propertyRenderedValue.graphql',
            variables: {
                path: '/sites/' + sitename + '/home/news1',
                property: 'author_bio',
                language: 'en'
            }
        }).should(result => {
            const propertyName = result?.data?.jcr?.nodeByPath?.property.name;
            const propertyValue = result?.data?.jcr?.nodeByPath?.property.value;
            const propertyValues = result?.data?.jcr?.nodeByPath?.property.values;
            const propertyRenderedValue = result?.data?.jcr?.nodeByPath?.property.renderedValue;
            const propertyRenderedValues = result?.data?.jcr?.nodeByPath?.property.renderedValues;
            expect(propertyName).eq('author_bio');
            expect(propertyValue).contains('The Big Bang Theory');
            expect(propertyValues).null;
            expect(propertyRenderedValue).contains('The Big Bang Theory');
            expect(propertyRenderedValues).null;
        });
        cy.apollo({
            queryFile: 'jcr/propertyRenderedValue.graphql',
            variables: {
                path: '/sites/' + sitename + '/home/news1',
                property: 'description',
                language: 'en'
            }
        }).should(result => {
            const propertyValue = result?.data?.jcr?.nodeByPath?.property.value;
            const propertyValues = result?.data?.jcr?.nodeByPath?.property.values;
            const propertyRenderedValue = result?.data?.jcr?.nodeByPath?.property.renderedValue;
            const propertyRenderedValues = result?.data?.jcr?.nodeByPath?.property.renderedValues;
            expect(propertyValue).null;
            expect(propertyValues).not.null;
            expect(propertyValues.length).eq(2);
            expect(propertyValues[0]).contains('{lang}');
            expect(propertyValues[0]).contains('{mode}');
            expect(propertyValues[1]).contains('{lang}');
            expect(propertyValues[1]).contains('{mode}');
            expect(propertyValues[1]).not.contains('/welcome');
            expect(propertyRenderedValue).null;
            expect(propertyRenderedValues).not.null;
            expect(propertyRenderedValues.length).eq(2);
            expect(propertyRenderedValues[0]).not.contains('{lang}');
            expect(propertyRenderedValues[0]).not.contains('{workspace}');
            expect(propertyRenderedValues[0]).not.contains('{mode}');
            expect(propertyRenderedValues[1]).not.contains('{lang}');
            expect(propertyRenderedValues[1]).not.contains('{workspace}');
            expect(propertyRenderedValues[1]).not.contains('{mode}');
            expect(propertyRenderedValues[1]).contains('/welcome');
        });
        deleteNode('/sites/' + sitename + '/home/news1');
    });

    //TODO Add test for renderedOutput and renderedValue including macro AND vanity URLs interpreted also
});
