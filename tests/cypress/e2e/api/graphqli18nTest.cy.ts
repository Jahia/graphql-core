import {addNode, createSite, deleteSite, setNodeProperty} from '@jahia/cypress';
import gql from 'graphql-tag';

const siteName = 'i18n-site';
const siteTemplateSet = 'dx-base-demo-templates';
const siteServerName = 'localhost';
const siteLanguages = ['en', 'fr', 'de'];
const siteLocale = 'en';

const contentRootPath = `/sites/${siteName}/contents`;
const contentFoldersList = ['ContentFolder-Level1', 'ContentFolder-Level2', 'ContentFolder-Level3'];
const contentNodeName = 'i18n-content';
const contentNodePath = `${contentRootPath}/${contentNodeName}`;

const i18nText = {en: 'Simple Text EN', fr: 'Simple Text FR'};
const i18nTitle = {en: 'Simple Title EN', fr: 'Simple Title FR', de: 'Simple Title DE'};

// Helper class to operate with Site properties fetched with getSiteProperties.graphql
// @param properties: {[key: string]: string | string[]}[] - list of properties
// @method get(name: string): string | string[] - retrieves the value of the property with the given name
// @example
//      const properties = new SiteProperties(response?.data?.jcr?.nodeByPath?.properties);
//      expect(properties.values('j:inactiveLanguages')).to.have.length(1);
//      expect(properties.values('j:inactiveLanguages')).to.include('fr');
class SiteProperties {
    properties: {[key: string]: string | string[]}[];
    // Default constructor
    // @param properties: {[key: string]: string | string[]}[] - list of properties
    constructor(properties: {[key: string]: string | string[]}[]) {
        this.properties = properties;
    }

    // Retrieves the `value` attribute of the property with the given name
    // @param name: string - name of the property
    value(name: string) {
        const myvar = this.properties.find((prop: { name: string, value: string, values: string[] }) => prop.name === name);
        return myvar.value;
    }

    // Retrieves the `values` attribute of the property with the given name
    // @param name: string - name of the property
    values(name: string) {
        const myvar = this.properties.find((prop: { name: string, value: string, values: string[] }) => prop.name === name);
        return myvar.values;
    }
}

// Retrieves and validates i18n properties for the language given
// @param lang: string - language to validate
// @returns {void}
// @note Function is added to avoid linting errors while using more than 4 nested levels and for better readability
function validatei18nProperties(node: string, lang: string) {
    it(`Should retrieve i18n properties for the language: "${lang}"`, () => {
        cy.apollo({
            query: gql`
                query {
                    jcr{
                        nodeByPath(path: "${node}") {
                        property(name: "text", language: "${lang}") {
                            internationalized,
                            language,
                            value,
                            values
                        }
                    }
                }
            }`
        }).then(response => {
            const property = response?.data?.jcr?.nodeByPath?.property;
            if (i18nText[lang] === undefined) {
                // If the translation does not exist, the property should be null
                expect(property).to.be.null;
            } else {
                // If the translation exists, the property should be an object with the expected values
                expect(property).to.have.property('internationalized', true);
                expect(property).to.have.property('language', lang);
                expect(property).to.have.property('value', i18nText[lang]);
                expect(property).to.have.property('values', null);
            }
        });
    });
}

// Creates Content Node with the given path, name and type
// @param path: string - path where the node will be created (e.g. /sites/siteName/contents/myFolder)
// @param name: string - name of the node to be created
// @param type: string - type of the node to be created
// @returns {void}
function createContentNode(path: string, name: string, type: string) {
    addNode({
        parentPathOrId: path,
        name: name,
        primaryNodeType: type
    });
}

describe('Test graphql i18n calls', () => {
    before('Create a test site with i18n node', () => {
        // Create a test site with the languages defined in the siteLanguages array
        createSite(siteName, {languages: siteLanguages.join(','), templateSet: siteTemplateSet, serverName: siteServerName, locale: siteLocale});
        // Create a node within content folders with type 'Simple text' to be used with translation
        createContentNode(contentRootPath, contentNodeName, 'jnt:text');

        // Add EN and FR translations to the node, skip DE one
        setNodeProperty(contentNodePath, 'text', i18nText.en, 'en');
        setNodeProperty(contentNodePath, 'text', i18nText.fr, 'fr');

        // Create 3 nested content folders
        let currentPath = contentRootPath;
        contentFoldersList.forEach(folderName => {
            createContentNode(currentPath, folderName, 'jnt:contentList');
            currentPath += `/${folderName}`;
        });
    });

    after('Delete test site', () => {
        deleteSite(siteName);
    });

    describe('Validate i18n properties while specific language is passed', () => {
        siteLanguages.forEach(language => validatei18nProperties(contentNodePath, language));
    });

    it('Should retrieve only those languages, i18n node is translated to', () => {
        cy.apollo({
            query: gql`
                query {
                    jcr(workspace: EDIT) {
                        nodeByPath(path: "${contentNodePath}") {
                            translationLanguages
                        }
                    }
                }
            `
        }).then(response => {
            // !cy.log(JSON.stringify(response.data, null, 2));
            const property = response?.data?.jcr?.nodeByPath?.translationLanguages;
            expect(property).to.have.length(2);
            expect(property).to.include('en');
            expect(property).to.include('fr');
        });
    });

    it('Should retrieve only Active translation languages', () => {
        // STEP 1: Disable FR language
        // @note 3 properties are being changed, because adjusting just 'j:inactiveLanguages' doesn't have changes reflected on UI
        cy.log('Disable FR language');
        cy.apollo({
            mutationFile: 'jcr/mutateNode.graphql',
            variables: {
                pathOrId: `/sites/${siteName}`,
                properties: [
                    {name: 'j:inactiveLanguages', values: ['fr']},
                    {name: 'j:inactiveLiveLanguages', values: ['fr']},
                    {name: 'j:languages', values: ['en', 'de']}
                ]
            }
        }).then(() => {
            // STEP 2: Check if the language is set as inactive
            cy.log('Fetch site properties and check if inactive language(s) are set');
            cy.apollo({
                mutationFile: 'site/getSiteProperties.graphql',
                variables: {siteName: `/sites/${siteName}`}
            }).then(response => {
                const properties = new SiteProperties(response?.data?.jcr?.nodeByPath?.properties);
                expect(properties.values('j:inactiveLanguages')).to.have.length(1);
                expect(properties.values('j:inactiveLanguages')).to.include('fr');
                expect(properties.values('j:inactiveLiveLanguages')).to.have.length(1);
                expect(properties.values('j:inactiveLiveLanguages')).to.include('fr');
                expect(properties.values('j:languages')).to.have.length(2);
                expect(properties.values('j:languages')).to.include('en');
                expect(properties.values('j:languages')).to.include('de');
            });
        }).then(() => {
            // STEP 3: Fetch and validate ACTIVE languages ONLY
            cy.log('Fetch and validate ACTIVE languages ONLY');
            cy.apollo({
                query: gql`
                    query {
                        jcr(workspace: EDIT) {
                            nodeByPath(path: "${contentNodePath}") {
                                translationLanguages(isActiveOnly:true)
                            }
                        }
                    }
                `
            }).then(response => {
                const property = response?.data?.jcr?.nodeByPath?.translationLanguages;
                expect(property).to.have.length(1);
                expect(property).to.include('en');
            });
        });
    });

    it('Should retrieve "Required Translation" languages', () => {
        // Adjust `jcr:title` property for the first content folder (`en` first and then `fr`)
        setNodeProperty(`${contentRootPath}/${contentFoldersList[0]}`, 'jcr:title', i18nTitle.en + (Math.random() * Date.now()), 'en');
        setNodeProperty(`${contentRootPath}/${contentFoldersList[0]}`, 'jcr:title', i18nTitle.fr + (Math.random() * Date.now()), 'fr');

        // `en` translation was done first and `fr` is the most recent one, so call should return `en`
        // meaning - `en` version requires translation
        cy.apollo({
            query: gql`
                query {
	                jcr(workspace: EDIT) {
  	                    nodeByPath(path: "${contentRootPath}/${contentFoldersList[0]}") {
			                languagesToTranslate(languagesTranslated:["fr"], languagesToCheck: ["en"])
                        }
                    }
                }
            `
        }).then(response => {
            const property = response?.data?.jcr?.nodeByPath?.languagesToTranslate;
            expect(property).to.have.length(1);
            expect(property).to.include('en');
        });

        // Adjust `jcr:title` property for the first content folder (`en` now is the most recent one)
        setNodeProperty(`${contentRootPath}/${contentFoldersList[0]}`, 'jcr:title', i18nTitle.en + (Math.random() * Date.now()), 'en');

        // `en` is now the most recent translation, so nothing should be returned
        cy.apollo({
            query: gql`
                query {
	                jcr(workspace: EDIT) {
  	                    nodeByPath(path: "${contentRootPath}/${contentFoldersList[0]}") {
			                languagesToTranslate(languagesTranslated:["fr"], languagesToCheck: ["en"])
                        }
                    }
                }
            `
        }).then(response => {
            const property = response?.data?.jcr?.nodeByPath?.languagesToTranslate;
            expect(property).to.have.length(0);
        });

        // `en` is now the most recent translation, so `fr` translation is needed
        cy.apollo({
            query: gql`
                query {
	                jcr(workspace: EDIT) {
  	                    nodeByPath(path: "${contentRootPath}/${contentFoldersList[0]}") {
			                languagesToTranslate(languagesTranslated:["en"], languagesToCheck: ["fr"])
                        }
                    }
                }
            `
        }).then(response => {
            const property = response?.data?.jcr?.nodeByPath?.languagesToTranslate;
            expect(property).to.have.length(1);
            expect(property).to.include('fr');
        });

        // `en` is now the most recent translation, so `fr` and `de` translations are needed
        cy.apollo({
            query: gql`
                query {
	                jcr(workspace: EDIT) {
  	                    nodeByPath(path: "${contentRootPath}/${contentFoldersList[0]}") {
			                languagesToTranslate(languagesTranslated:["en"], languagesToCheck: ["fr", "de"])
                        }
                    }
                }
            `
        }).then(response => {
            const property = response?.data?.jcr?.nodeByPath?.languagesToTranslate;
            expect(property).to.have.length(2);
            expect(property).to.include('fr');
            expect(property).to.include('de');
        });

        // Adjust `jcr:title` property for the first content folder (`fr` first and then `de`)
        setNodeProperty(`${contentRootPath}/${contentFoldersList[0]}`, 'jcr:title', i18nTitle.fr + (Math.random() * Date.now()), 'fr');
        setNodeProperty(`${contentRootPath}/${contentFoldersList[0]}`, 'jcr:title', i18nTitle.de + (Math.random() * Date.now()), 'de');

        // `fr` translation was done first and `de` is the most recent one, so call should return `fr`
        cy.apollo({
            query: gql`
                query {
	                jcr(workspace: EDIT) {
  	                    nodeByPath(path: "${contentRootPath}/${contentFoldersList[0]}") {
			                languagesToTranslate(languagesTranslated:["de", "en"], languagesToCheck: ["fr"])
                        }
                    }
                }
            `
        }).then(response => {
            const property = response?.data?.jcr?.nodeByPath?.languagesToTranslate;
            expect(property).to.have.length(1);
            expect(property).to.include('fr');
        });

        // `de` is the most recent translation, so nothing should be returned
        cy.apollo({
            query: gql`
                query {
	                jcr(workspace: EDIT) {
  	                    nodeByPath(path: "${contentRootPath}/${contentFoldersList[0]}") {
			                languagesToTranslate(languagesTranslated:["fr", "en"], languagesToCheck: ["de"])
                        }
                    }
                }
            `
        }).then(response => {
            const property = response?.data?.jcr?.nodeByPath?.languagesToTranslate;
            expect(property).to.have.length(0);
        });

        // `fr` and `de` are the most recent translations, so `en` translation is needed
        cy.apollo({
            query: gql`
                query {
	                jcr(workspace: EDIT) {
  	                    nodeByPath(path: "${contentRootPath}/${contentFoldersList[0]}") {
			                languagesToTranslate(languagesTranslated:["fr", "de"], languagesToCheck: ["en"])
                        }
                    }
                }
            `
        }).then(response => {
            const property = response?.data?.jcr?.nodeByPath?.languagesToTranslate;
            expect(property).to.have.length(1);
            expect(property).to.include('en');
        });
    });
});
