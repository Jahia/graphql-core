import gql from 'graphql-tag';
import {addVanityUrl, createSite, deleteSite} from '@jahia/cypress';

const sitename = 'graphql_test_node_url_params';
const vanity = '/url-params-home';

const urlQuery = gql`
    query ($path: String!) {
        jcr {
            nodeByPath(path: $path) {
                url
                sizedUrl: url(params: ["w:640", "q:80"])
                emptyParamsUrl: url(params: [])
                vanityUrls(languages: ["en"]) {
                    url
                    sizedUrl: url(params: ["w:640"])
                }
            }
        }
    }
`;

describe('Node url field with params', () => {
    before('Create a site with a vanity URL', () => {
        createSite(sitename);
        addVanityUrl(`/sites/${sitename}/home`, 'en', vanity);
    });

    after('Delete the site', () => {
        deleteSite(sitename);
    });

    it('Returns the same URL with or without params when no decorator reads them', () => {
        cy.apollo({query: urlQuery, variables: {path: `/sites/${sitename}/home`}}).should(resp => {
            const node = resp.data.jcr.nodeByPath;
            expect(node.url).to.contain(`/sites/${sitename}/home`);
            expect(node.sizedUrl).to.equal(node.url);
            expect(node.emptyParamsUrl).to.equal(node.url);
            expect(node.vanityUrls).to.have.length(1);
            expect(node.vanityUrls[0].url).to.equal(vanity);
            expect(node.vanityUrls[0].sizedUrl).to.equal(vanity);
        });
    });
});
