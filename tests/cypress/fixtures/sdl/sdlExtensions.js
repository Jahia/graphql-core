import gql from 'graphql-tag';

export const getTypeFields = (typeName) => gql`
    query coreSdlExtensions {
        __type(name: "${typeName}") {
            fields {name}
        }
    }
`;
