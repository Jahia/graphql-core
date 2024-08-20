import {addNode, createSite, deleteSite, publishAndWaitJobEnding} from '@jahia/cypress';
import {grantUserRole} from '../../../fixtures/acl';

describe('Node locks tests', () => {
    const siteName = 'locks';
    const sitePath = '/sites/locks';
    const page1Path = sitePath + '/home/page1';
    const page2Path = sitePath + '/home/page2';
    const siteAdminCredentials = {username: 'bill', password: 'password'};
    const editorCredentials = {username: 'mathias', password: 'password'};

    before('Create a site', () => {
        createSite(siteName);
        addNode({
            parentPathOrId: sitePath + '/home',
            name: 'page1',
            primaryNodeType: 'jnt:page',
            properties: [
                {name: 'jcr:title', value: 'page1'},
                {name: 'j:templateName', value: 'simple'}
            ]
        });
        addNode({
            parentPathOrId: sitePath + '/home',
            name: 'page2',
            primaryNodeType: 'jnt:page',
            properties: [
                {name: 'jcr:title', value: 'page2'},
                {name: 'j:templateName', value: 'simple'}
            ]
        });
        publishAndWaitJobEnding(sitePath, ['en']);
        grantUserRole(sitePath, 'site-administrator', 'bill');
        grantUserRole(sitePath, 'editor', 'mathias');
    });

    after('Delete the site', () => {
        deleteSite(siteName);
    });

    it('Should not let user unlock a node that is locked by another user', () => {
        cy.apolloClient(siteAdminCredentials).apollo({
            queryFile: 'jcr/lockNode.graphql',
            variables: {
                pathOrId: page1Path
            }
        });

        cy.apolloClient(editorCredentials)
            .apollo({
                queryFile: 'jcr/lockInfoByPath.graphql',
                variables: {
                    pathOrId: page1Path
                }
            })
            .should(resp => {
                debugger;
                expect(resp?.data?.jcr?.nodeByPath?.lockInfo?.canUnlock, 'Cannot unlock').to.be.false;
            });

        cy.apolloClient(editorCredentials)
            .apollo({
                queryFile: 'jcr/unlockNode.graphql',
                variables: {pathOrId: page1Path}
            })
            .should(resp => {
                expect(resp?.data?.jcr?.mutateNode?.unlock, 'Unlock request KO').to.be.false;
            });
    });

    it('Should let site admins unlock content locked by another user', () => {
        cy.apolloClient(editorCredentials).apollo({
            queryFile: 'jcr/lockNode.graphql',
            variables: {
                pathOrId: page2Path
            }
        });

        cy.apolloClient(siteAdminCredentials)
            .apollo({
                queryFile: 'jcr/lockInfoByPath.graphql',
                variables: {
                    pathOrId: page2Path
                }
            })
            .should(resp => {
                expect(resp?.data?.jcr?.nodeByPath?.lockInfo?.canUnlock, 'Can unlock').to.be.true;
            });

        cy.apolloClient(siteAdminCredentials)
            .apollo({
                queryFile: 'jcr/unlockNode.graphql',
                variables: {pathOrId: page2Path}
            })
            .should(resp => {
                expect(resp?.data?.jcr?.mutateNode?.unlock, 'Unlock request OK').to.be.true;
            });
    });
});
