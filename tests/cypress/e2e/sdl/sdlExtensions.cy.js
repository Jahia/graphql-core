import {
    getCoreSdlExtensions,
    getExampleSdlExtensions
} from '../../fixtures/sdl/sdlExtensions';

describe('SDL extensions', () => {
    it('should have SDL extensions defined in dxm-provider module', () => {
        cy.apollo({query: getCoreSdlExtensions}).then((response) => {
            const {category, imageAsset} = response?.data || {};
            expect(category?.fields?.map(f => f.name)).to.deep.eq(
                ['metadata', 'description', 'title']
            );
            expect(imageAsset?.fields?.map(f => f.name)).to.deep.eq(
                ['metadata', 'type', 'size', 'height', 'width']
            );
        });
    });

    it('should have SDL extensions defined in extension-examples module', () => {
        cy.apollo({query: getExampleSdlExtensions}).then((response) => {
            const {newsSdl, images, queries} = response?.data || {};
            expect(newsSdl?.fields?.map(f => f.name)).to.deep.eq(
                ['title', 'description', 'checked', 'date']
            );
            expect(images?.fields?.map(f => f.name)).to.deep.eq(
                ['height']
            );
            ['newsByChecked', 'myNewsByDate', 'myImagesByHeight'].forEach(queryField => {
                expect(queries?.fields?.find(f => f.name === queryField)).to.exist;
            });
        });
    });
});
