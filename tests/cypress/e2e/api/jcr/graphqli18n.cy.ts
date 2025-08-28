import {addNode, getNodeByPath, createSite, deleteSite, setNodeProperty, Log} from '@jahia/cypress';
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
/**
 * Helper class to operate with Site properties fetched with ../../../fixtures/site/getSiteProperties.graphql query
 * @param {{[key: string]: string | string[]}[]} properties - list of properties
 * @example
 *      const properties = new SiteProperties(response?.data?.jcr?.nodeByPath?.properties);
 *      expect(properties.values('j:inactiveLanguages').values).to.have.length(1);
 *      expect(properties.values('j:inactiveLanguages').values).to.include('fr');
 * @todo to make it scalable, it makes sense to keep this class (maybe create a separate Type instead)
 *       along with gql query itself and have a helper method to retrieve the properties and
 *       return them using a corresponding data type
 */
class SiteProperties {
    properties: {[key: string]: string | string[]}[];
    /**
     * @constructor
     * @param {{[key: string]: string | string[]}[]} properties  - list of properties
     */
    constructor(properties: {[key: string]: string | string[]}[]) {
        this.properties = properties;
    }

    /**
     * Retrieves the property with the given name
     * @param {string} name - name of the property
     */
    get(name: string) {
        return this.properties.find((prop: { name: string, value: string, values: string[] }) => prop.name === name);
    }
}

/**
 * Retrieves and validates node's i18n properties for the language given
 * @param {string} lang - language to validate
 * @returns {void}
 * @note Function is added to avoid linting errors while using more than 4 nested levels and for better readability
 */
function validatei18nProperties(node: string, lang: string): void {
    cy.log(`Retrieve and validate i18n properties for the language: "${lang}"`);
    getNodeByPath(`/sites/${siteName}`, ['text'], 'en');
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
}

/**
 * Creates Content Node with the given path, name and type
 * @param {string} path - path where the node will be created (e.g. /sites/siteName/contents/myFolder)
 * @param {string} name - name of the node to be created
 * @param {string} type - type of the node to be created
 * @returns {void}
 */
function createContentNode(path: string, name: string, type: string):void {
    addNode({
        parentPathOrId: path,
        name: name,
        primaryNodeType: type
    });
}

/**
 * Retrieves the languages that need to be translated for the given node.
 * @note Uses gql which checks if the given locales need translation,
 *       by comparing last modifications dates with already existing translations.
 * @param {string} node - path of the node to check (e.g. /sites/siteName/contents/myFolder)
 * @param {string} langTranslated - languages that are already translated (e.g. ["fr"])
 * @param {string} langToCheck - languages to check for translation (e.g. ["en"])
 * @returns {Chainable} - returns a chainable object that resolves to an array of languages that need translation
 */
function getLanguagesToTranslate(node: string, langTranslated: string, langToCheck: string): Cypress.Chainable {
    // Call the GraphQL query to get the languages to translate
    Log.info(`Get languages to translate for node: ${node}, translated: ${langTranslated}, to check: ${langToCheck}`);
    // Use the gql query to check if the given locales need translation and return the result
    return cy.apollo({
        query: gql`
            query {
                jcr(workspace: EDIT) {
                    nodeByPath(path: "${node}") {
                        languagesToTranslate(languagesTranslated:${langTranslated}, languagesToCheck: ${langToCheck})
                    }
                }
            }
        `
    }).then(response => {
        // Get and return the languages to translate from the response
        return response?.data?.jcr?.nodeByPath?.languagesToTranslate || [];
    });
}

describe('Test graphql i18n calls', () => {
    before('Create a test site with i18n node', () => {
        // Set the log level to INFO
        Log.setVerbosity(Log.LEVEL.INFO);

        // Delete the site if it already exists
        deleteSite(siteName);
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

    it('Should validate i18n properties for the content node whith specific language passed', () => {
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
            const property = response?.data?.jcr?.nodeByPath?.translationLanguages;
            expect(property).to.have.length(2);
            expect(property).to.include('en');
            expect(property).to.include('fr');
        });
    });

    it('Should retrieve only Active translation languages', () => {
        cy.step('Retrieve only Active translation languages', () => {
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
            });
        });

        // STEP 1: Disable FR language
        // @note 3 properties are being changed, because adjusting just 'j:inactiveLanguages' doesn't have changes reflected on UI
        cy.step('Disable FR language', () => {
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
            });
        });

        // STEP 2: Check if the language is set as inactive
        cy.step('Fetch site properties and check if inactive language(s) are set', () => {
            getNodeByPath(`/sites/${siteName}`, null).then(response => {
                const properties = new SiteProperties(response?.data?.jcr?.nodeByPath?.properties);
                expect(properties.get('j:inactiveLanguages').values).to.have.length(1);
                expect(properties.get('j:inactiveLanguages').values).to.include('fr');
                expect(properties.get('j:inactiveLiveLanguages').values).to.have.length(1);
                expect(properties.get('j:inactiveLiveLanguages').values).to.include('fr');
                expect(properties.get('j:languages').values).to.have.length(2);
                expect(properties.get('j:languages').values).to.include('en');
                expect(properties.get('j:languages').values).to.include('de');
                Log.json(Log.LEVEL.DEBUG, response.data);
            });
        });

        // STEP 3: Fetch and validate ACTIVE languages ONLY
        cy.step('Fetch and validate ACTIVE languages ONLY', () => {
            Log.info('Fetch and validate ACTIVE languages ONLY').then(() => {
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
    });

    it('Should retrieve "Required Translation" languages', () => {
        // Adjust `jcr:title` property for the first content folder
        // (update `en` translation first and then `fr` one)
        Log.info('Adjust jcr:title property for the first content folder ("en" translation first and then "fr" one)');
        setNodeProperty(`${contentRootPath}/${contentFoldersList[0]}`, 'jcr:title', i18nTitle.en + (Math.random() * Date.now()), 'en');
        setNodeProperty(`${contentRootPath}/${contentFoldersList[0]}`, 'jcr:title', i18nTitle.fr + (Math.random() * Date.now()), 'fr');

        // Retrieve languages that need to be translated
        // `en` translation was done first and `fr` is the most recent one, so call should return `en`
        // meaning - `en` version requires translation
        getLanguagesToTranslate(`${contentRootPath}/${contentFoldersList[0]}`, '["fr"]', '["en"]')
            .then(result => {
                expect(result).to.have.length(1);
                expect(result).to.include('en');
            });

        // Adjust `jcr:title` property for the first content folder (`en` translation is now the most recent one)
        Log.info('Adjust jcr:title property for the first content folder (making "en" translation the most recent one)');
        setNodeProperty(`${contentRootPath}/${contentFoldersList[0]}`, 'jcr:title', i18nTitle.en + (Math.random() * Date.now()), 'en');

        // Retrieve languages that need to be translated
        // `en` is now the most recent translation, so nothing should be returned
        getLanguagesToTranslate(`${contentRootPath}/${contentFoldersList[0]}`, '["fr"]', '["en"]')
            .then(result => {
                expect(result).to.have.length(0);
            });

        // Retrieve languages that need to be translated
        // `en` is now the most recent translation, so `fr` translation is needed
        getLanguagesToTranslate(`${contentRootPath}/${contentFoldersList[0]}`, '["en"]', '["fr"]')
            .then(result => {
                expect(result).to.have.length(1);
                expect(result).to.include('fr');
            });

        // Retrieve languages that need to be translated
        // `en` is now the most recent translation, so `fr` and `de` translations are needed
        getLanguagesToTranslate(`${contentRootPath}/${contentFoldersList[0]}`, '["en"]', '["fr", "de"]')
            .then(result => {
                expect(result).to.have.length(2);
                expect(result).to.include('fr');
                expect(result).to.include('de');
            });

        // Adjust `jcr:title` property for the first content folder
        // (update `fr` translation first and then `de` one)
        cy.log('Adjust jcr:title property for the first content folder ("fr" translation first and then "de" one)');
        setNodeProperty(`${contentRootPath}/${contentFoldersList[0]}`, 'jcr:title', i18nTitle.fr + (Math.random() * Date.now()), 'fr');
        setNodeProperty(`${contentRootPath}/${contentFoldersList[0]}`, 'jcr:title', i18nTitle.de + (Math.random() * Date.now()), 'de');

        // Retrieve languages that need to be translated
        // `fr` translation was done first and `de` is the most recent one, so call should return `fr`
        getLanguagesToTranslate(`${contentRootPath}/${contentFoldersList[0]}`, '["de", "en"]', '["fr"]')
            .then(result => {
                expect(result).to.have.length(1);
                expect(result).to.include('fr');
            });

        // Retrieve languages that need to be translated
        // `de` is the most recent translation, so nothing should be returned
        getLanguagesToTranslate(`${contentRootPath}/${contentFoldersList[0]}`, '["fr", "en"]', '["de"]')
            .then(result => {
                expect(result).to.have.length(0);
            });

        // Retrieve languages that need to be translated
        // `fr` and `de` are the most recent translations, so `en` translation is needed
        getLanguagesToTranslate(`${contentRootPath}/${contentFoldersList[0]}`, '["fr", "de"]', '["en"]')
            .then(result => {
                expect(result).to.have.length(1);
                expect(result).to.include('en');
            });
    });

    it('allows modifying translation nodes directly', () => {
        cy.apollo({
            query: gql`
                mutation ModifyDescription {
                    jcr {
                        mutateNode(
                            pathOrId: "${contentNodePath}/j:translation_fr"
                        ) {
                            mutateProperty(name: "text") {
                                setValue(type: STRING, value: "new fr test value")
                            }
                        }
                        modifiedNodes {
                            path
                        }
                    }
                }
            `
        }).then(response => {
            expect(response.data.jcr.modifiedNodes[0].path).to.equal(`${contentNodePath}/j:translation_fr`);
        });

        getNodeByPath(contentNodePath, ['text'], 'fr').then(response => {
            const props = new SiteProperties(response.data.jcr.nodeByPath.properties);
            const textProperty = props.get('text');
            expect(textProperty).to.exist;
            expect(textProperty.value).to.equal('new fr test value');
        });
    });
});
