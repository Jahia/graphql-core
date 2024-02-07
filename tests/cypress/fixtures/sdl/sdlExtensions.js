import gql from 'graphql-tag';

export const getCoreSdlExtensions = gql`
    query coreSdlExtensions {
        category: __type(name: "Category") {
            fields {name}
        },
        imageAsset: __type(name: "ImageAsset") {
            fields {name}
        }
    }
`;

export const getExampleSdlExtensions = gql`
    query exampleSdlExtensions {
        newsSdl: __type(name:"NewsSDL") {
            fields {name}
        }
        images: __type(name:"Images") {
            fields {name}
        }
        queries: __type(name: "Query") {
            fields {name}
        }
    }
`;
