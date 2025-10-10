import {addNode, addVanityUrl, createSite, deleteNode, deleteSite, removeVanityUrl} from '@jahia/cypress';

const sitename = 'graphql_test_render';
describe('Test graphql rendering', () => {
    before('Create a site', () => {
        createSite(sitename);
    });

    after('Delete the site', () => {
        deleteSite(sitename);
    });

    /**
     * Helper function to add a test node including parent page and placeholder to the given parentPath
     * @param node Object containing the following properties:
     *  - parentPath: Path where the page should be created (e.g. /sites/mysite/home)
     *  - pageName: Name of the page to create (e.g. myPage)
     *  - pageTitle: Title of the page to create (e.g. My Page)
     *  - name: Name of the actual test node (e.g. myText)
     *  - type: Node type of the actual test node (e.g. jnt:text)
     *  - value: Value of the actual test node (e.g. 'This is my text')
     */
    const addTestNode = (node: {
        parentPath: string;
        pageName: string;
        pageTitle: string;
        name: string;
        type: string;
        value: string;
    }) => {
        addNode({
            parentPathOrId: node.parentPath,
            name: node.pageName,
            primaryNodeType: 'jnt:page',
            properties: [
                {name: 'jcr:title', value: node.pageTitle, language: 'en'},
                {name: 'j:templateName', value: 'simple'}
            ],
            children: [
                {
                    name: 'landing',
                    primaryNodeType: 'jnt:contentList',
                    children: [
                        {
                            name: node.name,
                            primaryNodeType: node.type,
                            properties: [{name: 'text', value: node.value, language: 'en'}]
                        }
                    ]
                }
            ]
        });
    };

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

    it('Check output of renderedContent for node with RichText', () => {
        // Test-node description, including parent page, placeholder ('landing') and destination node
        const testNode = {
            parentPath: `/sites/${sitename}/home`,
            pageName: 'richTextPage',
            pageTitle: 'Rich Text Page',
            name: 'rich-text',
            path: `/sites/${sitename}/home/richTextPage/landing/rich-text`,
            type: 'jnt:bigText',
            value: `<p>Richtext</p>\\n\\n<p>Back to <a href="/cms/{mode}/{lang}/sites/${sitename}/home.html">Home</a></p>\\n`
        };
        const expectedContent = `<p>Richtext</p>\\n\\n<p>Back to <a href="/cms/render/default/en/sites/${sitename}/home.html">Home</a></p>\\n`;
        // Create test-node itself
        addTestNode(testNode);

        // Check renderedContent in LIVE mode for the text node
        // Should return the text value of the node with the link filtered
        // (i.e. {mode} and {lang} replaced with actual values)
        cy.apollo({
            queryFile: 'jcr/nodeRenderedContent.graphql',
            variables: {
                path: testNode.path,
                view: 'html',
                isEditMode: false
            }
        }).should(result => {
            const output = result?.data?.jcr?.nodeByPath?.renderedContent.output;
            expect(output).contains(expectedContent);
            expect(output).not.contains('{workspace}');
            expect(output).not.contains('{lang}');
            expect(output).not.contains('{mode}');
        });

        // Cleanup - delete test-node including parent page
        deleteNode(`${testNode.parentPath}/${testNode.pageName}`);
    });

    it('Check output of renderedContent in EDIT workspace', () => {
        // Test-node description, including parent page, placeholder ('landing') and destination node
        const testNode = {
            parentPath: `/sites/${sitename}/home`,
            pageName: 'editModePage',
            pageTitle: 'EDIT Mode Page',
            name: 'simple-text',
            path: `/sites/${sitename}/home/editModePage/landing/simple-text`,
            type: 'jnt:text',
            value: 'test text for EDIT mode'
        };
        // Create test-node itself
        addTestNode(testNode);

        // Check renderedContent in EDIT mode for the text node
        // Should return the text value of the node
        cy.apollo({
            queryFile: 'jcr/nodeRenderedContent.graphql',
            variables: {
                path: testNode.path,
                isEditMode: true
            }
        }).should(result => {
            const output = result?.data?.jcr?.nodeByPath?.renderedContent.output;
            expect(output).contains(testNode.value);
        });

        // Check renderedContent in EDIT mode for the page node itself (without view)
        // Should return a message that no rendering is set for the node
        // (as the page has no rendering, but its child nodes have)
        cy.apollo({
            queryFile: 'jcr/nodeRenderedContent.graphql',
            variables: {
                path: `/sites/${sitename}/home/${testNode.pageName}`,
                isEditMode: true
            }
        }).should(result => {
            const output = result?.data?.jcr?.nodeByPath?.renderedContent.output;
            expect(output).contains(`<p>\r\n    No rendering set for node: ${testNode.pageName}<br/>Types: [jnt:page]</p>`);
        });

        // Cleanup - delete test-node including parent page
        deleteNode(`${testNode.parentPath}/${testNode.pageName}`);
    });

    it('Check if property renderedValue and renderedValues are populated correctly', () => {
        addNode({
            parentPathOrId: '/sites/' + sitename + '/home',
            name: 'news2',
            primaryNodeType: 'gqltest:news',
            properties: [
                {name: 'title', language: 'en', value: 'My Test News Two'},
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
        cy.apollo({
            queryFile: 'jcr/propertyRenderedValue.graphql',
            variables: {
                path: '/sites/' + sitename + '/home/news2',
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
                path: '/sites/' + sitename + '/home/news2',
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
                path: '/sites/' + sitename + '/home/news2',
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
                path: '/sites/' + sitename + '/home/news2',
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
            expect(propertyRenderedValue).null;
            expect(propertyRenderedValues).not.null;
            expect(propertyRenderedValues.length).eq(2);
        });
        deleteNode('/sites/' + sitename + '/home/news2');
    });

    it('Check if property renderedValue and renderedValues URLs are filtered correctly', () => {
        addNode({
            parentPathOrId: '/sites/' + sitename + '/home',
            name: 'news3',
            primaryNodeType: 'gqltest:news',
            properties: [
                {name: 'title', language: 'en', value: 'My Test News Three'},
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
        cy.apollo({
            queryFile: 'jcr/propertyRenderedValue.graphql',
            variables: {
                path: '/sites/' + sitename + '/home/news3',
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
            expect(propertyValues[0]).contains('/cms/{mode}/{lang}/sites/' + sitename + '/home.html');
            expect(propertyValues[1]).contains('/cms/{mode}/{lang}/sites/' + sitename + '/other.html');
            expect(propertyRenderedValue).null;
            expect(propertyRenderedValues).not.null;
            expect(propertyRenderedValues.length).eq(2);
            expect(propertyRenderedValues[0]).not.contains('{lang}').and.not.contains('{workspace}').and.not.contains('{mode}');
            expect(propertyRenderedValues[1]).not.contains('{lang}').and.not.contains('{workspace}').and.not.contains('{mode}');
        });
        deleteNode('/sites/' + sitename + '/home/news3');
    });

    it('Check if renderedValue and renderedValues vanityUrls are filtered correctly', () => {
        addNode({
            parentPathOrId: '/sites/' + sitename + '/home',
            name: 'page1',
            primaryNodeType: 'jnt:page',
            properties: [
                {name: 'jcr:title', value: 'page1'},
                {name: 'j:templateName', value: 'simple'}
            ]
        });
        addNode({
            parentPathOrId: '/sites/' + sitename + '/home',
            name: 'news4',
            primaryNodeType: 'gqltest:news',
            properties: [
                {name: 'title', language: 'en', value: 'My Test News Four'},
                {name: 'description', values: [
                    '<p>Richtext1</p>\\n\\n<p>Back to <a href=\'/cms/{mode}/{lang}/sites/' + sitename + '/home.html\'>Home</a></p>\\n',
                    '<p>Richtext2</p>\\n\\n<p>Go to <a href=\'/cms/{mode}/{lang}/sites/' + sitename + '/home/page1.html\'>Other</a></p>\\n'
                ]},
                {name: 'author', value: 'Sheldon'},
                {name: 'author_bio', language: 'en', value: 'Sheldon Lee Cooper, Ph.D., Sc.D., is a fictional character in the CBS television' +
                        ' series The Big Bang Theory and its spinoff series Young Sheldon, portrayed by actors Jim Parsons and Iain ' +
                        'Armitage respectively (with Parsons as the latter series\' narrator).' +
                        '<p>Back to <a href=\'/cms/{mode}/{lang}/sites/' + sitename + '/home/page1.html\'>Home</a></p>'}
            ]
        });
        addVanityUrl(
            '/sites/' + sitename + '/home/page1',
            'en',
            '/thepage'
        );
        cy.apollo({
            queryFile: 'jcr/propertyRenderedValue.graphql',
            variables: {
                path: '/sites/' + sitename + '/home/news4',
                property: 'author_bio',
                language: 'en'
            }
        }).should(result => {
            const propertyValue = result?.data?.jcr?.nodeByPath?.property.value;
            const propertyRenderedValue = result?.data?.jcr?.nodeByPath?.property.renderedValue;
            expect(propertyValue).not.null;
            expect(propertyValue).not.contains('/thepage');
            expect(propertyRenderedValue).not.null;
            expect(propertyRenderedValue).contains('/thepage');
        });
        cy.apollo({
            queryFile: 'jcr/propertyRenderedValue.graphql',
            variables: {
                path: '/sites/' + sitename + '/home/news4',
                property: 'description',
                language: 'en'
            }
        }).should(result => {
            const propertyValues = result?.data?.jcr?.nodeByPath?.property.values;
            const propertyRenderedValues = result?.data?.jcr?.nodeByPath?.property.renderedValues;
            expect(propertyValues).not.null;
            expect(propertyValues.length).eq(2);
            expect(propertyValues[1]).not.contains('/thepage');
            expect(propertyRenderedValues).not.null;
            expect(propertyRenderedValues.length).eq(2);
            expect(propertyRenderedValues[1]).contains('/thepage');
        });
        removeVanityUrl(
            '/sites/' + sitename + '/home/page1',
            '/thepage'
        );
        deleteNode('/sites/' + sitename + '/home/page1');
        deleteNode('/sites/' + sitename + '/home/news4');
    });

    it('Check if renderedValue and renderedValues macros are filtered correctly', () => {
        addNode({
            parentPathOrId: '/sites/' + sitename + '/home',
            name: 'news5',
            primaryNodeType: 'gqltest:news',
            properties: [
                {name: 'title', language: 'en', value: 'My Test News Five'},
                {name: 'description', values: [
                    '<p>Richtext1</p>\\n\\n<p>Created on ##creationdate##\\n',
                    '<p>Richtext2</p>\\n\\n<p>Authored by ##authorname##\\n'
                ]},
                {name: 'author', value: 'Sheldon'},
                {name: 'author_bio', language: 'en', value: 'Sheldon Lee Cooper, Ph.D., Sc.D., is a fictional character in the CBS television' +
                        ' series The Big Bang Theory and its spinoff series Young Sheldon, portrayed by actors Jim Parsons and Iain ' +
                        'Armitage respectively (with Parsons as the latter series\' narrator).'}
            ]
        });
        cy.apollo({
            queryFile: 'jcr/propertyRenderedValue.graphql',
            variables: {
                path: '/sites/' + sitename + '/home/news5',
                property: 'description',
                language: 'en'
            }
        }).should(result => {
            const propertyValues = result?.data?.jcr?.nodeByPath?.property.values;
            const propertyRenderedValues = result?.data?.jcr?.nodeByPath?.property.renderedValues;
            expect(propertyValues).not.null;
            expect(propertyValues.length).eq(2);
            expect(propertyValues[0]).contains('##creationdate##');
            expect(propertyValues[1]).contains('##authorname##');
            expect(propertyRenderedValues).not.null;
            expect(propertyRenderedValues.length).eq(2);
            expect(propertyRenderedValues[0]).not.contains('##creationdate##');
            expect(propertyRenderedValues[1]).not.contains('##authorname##');
        });
        deleteNode('/sites/' + sitename + '/home/news5');
    });

    it('gets view name from property when rendering', () => {
        addNode({
            parentPathOrId: '/sites/' + sitename + '/home',
            name: 'text1',
            primaryNodeType: 'jnt:bigText',
            properties: [
                {name: 'text', language: 'en', value: 'test'},
                {name: 'j:view', language: 'en', value: 'link'}
            ],
            mixins: ['jmix:renderable']
        });

        cy.apollo({
            queryFile: 'jcr/nodeRenderedContent.graphql',
            variables: {
                path: '/sites/' + sitename + '/home/text1',
                view: null
            }
        }).should(result => {
            const output = result?.data?.jcr?.nodeByPath?.renderedContent.output;
            expect(output).contains(
                '<a target="" href="/cms/render/default/en/sites/graphql_test_render/home/text1.html">text1</a>'
            );
        });
        deleteNode('/sites/' + sitename + '/home/text1');
    });
});
