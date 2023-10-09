
describe('Test admin roles query endpoint', () => {
    it('gets ACL roles', () => {
        cy.apollo({
            queryFile: 'acl/getRoles.graphql',
            variables: {lang: 'en'}
        }).should(resp => {
            expect(resp.data.admin.roles).to.exist;
            expect(resp.data.admin.roles.length).to.be.greaterThan(0);
            const editorRole = resp.data.admin.roles.find(r => r.name === 'editor');
            expect(editorRole).to.not.be.empty;
            expect(editorRole.roleGroup).equals('edit-role');
        });
    });

    it('gets translated labels', () => {
        const lang = 'de';
        const title = 'editor[DE]';
        const desc = 'editor-desc[DE]';
        setLabels(lang, title, desc);

        cy.apollo({
            queryFile: 'acl/getRoles.graphql',
            variables: {lang: 'de'}
        }).should(resp => {
            expect(resp.data.admin.roles).to.exist;
            expect(resp.data.admin.roles.length).to.be.greaterThan(0);
            const editorRole = resp.data.admin.roles.find(r => r.name === 'editor');
            expect(editorRole).to.not.be.empty;
            expect(editorRole.roleGroup).equals('edit-role');
            expect(editorRole.label).equals(title);
            expect(editorRole.description).equals(desc);
        });

        removeLabels(lang);
    });

    function setLabels(lang, title, description) {
        cy.apollo({
            mutationFile: 'acl/setLabels.graphql',
            variables: {pathOrId: '/roles/editor', lang, title, description}
        }).should(resp => {
            expect(resp?.data?.jcr?.mutateNode?.setPropertiesBatch?.length).equals(2);
        });
    }

    function removeLabels(lang) {
        cy.apollo({
            mutationFile: 'acl/removeLabels.graphql',
            variables: {pathOrId: '/roles/editor', lang}
        }).should(resp => {
            const propResp = resp?.data?.jcr?.mutateNode?.mutateProperties;
            expect(propResp.length).equals(2);
            expect(propResp.every(n => n.delete)).to.be.true;
        });
    }
});
