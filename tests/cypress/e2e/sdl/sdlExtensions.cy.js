import {getTypeFields} from '../../fixtures/sdl/sdlExtensions';

describe('SDL extensions', () => {

    function verifySdlFields(typeName, expectedFields) {
        return cy.apollo({query: getTypeFields(typeName)})
            .then((response) => {
                const data = response?.data || {};
                const fieldNames = data['__type']?.fields?.map(f => f.name) || [];
                if (expectedFields) {
                    expect(fieldNames).to.deep.eq(expectedFields);
                }
                return fieldNames;
            });
    }

    it('should have SDL extensions defined in dxm-provider module', () => {
        verifySdlFields('Category', ['metadata', 'description', 'title']);
        verifySdlFields('ImageAsset', ['metadata', 'type', 'size', 'height', 'width']);
    });

    it('should have SDL extensions defined in extension-examples module', () => {
        verifySdlFields('NewsSDL', ['title', 'description', 'checked', 'date']);
        verifySdlFields('Images', ['height']);
        verifySdlFields('Query').then(returnedFields => {
            ['newsByChecked', 'myNewsByDate', 'myImagesByHeight'].forEach(queryField => {
                expect(returnedFields).to.include(queryField);
            });
        });
    });
});
