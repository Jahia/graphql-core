package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLConstants;

public class FinderFetchersFactory {

    public enum FetcherType {
        ALL("all", null),
        ID(null, "ById"),
        PATH(null, "ByPath"),
        DATE(),
        NUMBER(),
        BOOLEAN(),
        STRING();

        String prefix;
        String suffix;

        FetcherType() {
        }

        FetcherType(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        public String getName(String base) {
            return (prefix != null ? (prefix + base) : StringUtils.uncapitalize(base)) + (suffix != null ? suffix : "");
        }

        public String getPrefix() {
            return prefix;
        }

        public String getSuffix() {
            return suffix;
        }
    }

    public static FinderDataFetcher getFetcher(GraphQLFieldDefinition fieldDefinition, String nodeType) {
        String queryName = fieldDefinition.getName().replace(SDLConstants.CONNECTION_QUERY_SUFFIX, "");

        Finder finder = new Finder(queryName);
        finder.setType(nodeType);

        //Handle all, byId and byPath cases
        for (FetcherType fetcherType : FetcherType.values()) {
            if ((fetcherType.prefix != null && queryName.startsWith(fetcherType.prefix)) || (fetcherType.suffix != null && queryName.endsWith(fetcherType.suffix))) {
                return getFetcherType(finder, fetcherType);
            }
        }

        //Handle specialized types
        String definitionPropertyName = getDefinitionProperty(queryName);
        String propertyNameInJcr = getMappedProperty(definitionPropertyName, fieldDefinition);
        String propertyType = getMappedType(definitionPropertyName, fieldDefinition);
        finder.setProperty(propertyNameInJcr != null ? propertyNameInJcr : definitionPropertyName);

        switch (propertyType) {
            case "Date":
                return getFetcherType(finder, FetcherType.DATE);
            case "Boolean":
                return getFetcherType(finder, FetcherType.BOOLEAN);
            case "BigDecimal":
            case "BigInteger":
            case "Long":
            case "Short":
            case "Float":
            case "Int":
                NumberFinder f = NumberFinder.fromFinder(finder);
                f.setNumberType(propertyType);
                return getFetcherType(f, FetcherType.NUMBER);
            default:
                return getFetcherType(finder, FetcherType.STRING);
        }
    }

    public static FinderDataFetcher getFetcherType(final Finder finder, final FetcherType type) {
        switch (type) {
            case ALL:
                return new AllFinderDataFetcher(finder);
            case ID:
                return new ByIdFinderDataFetcher(finder);
            case PATH:
                return new ByPathFinderDataFetcher(finder);
            case STRING:
                return new StringFinderDataFetcher(finder);
            case DATE:
                return new DateRangeDataFetcher(finder);
            case BOOLEAN:
                return new BooleanFinderDataFetcher(finder);
            case NUMBER:
                return new NumberFinderDataFetcher((NumberFinder) finder);
            default:
                return null;
        }
    }

    public static String getDefinitionProperty(String queryName) {
        return StringUtils.uncapitalize(StringUtils.substringAfterLast(queryName, "By"));
    }

    public static String getMappedProperty(String definitionPropertyName, GraphQLFieldDefinition fieldDefinition) {
        GraphQLObjectType type = (GraphQLObjectType) ((GraphQLList) fieldDefinition.getType()).getWrappedType();
        GraphQLFieldDefinition fd = type.getFieldDefinition(definitionPropertyName);
        if (fd == null) return null;
        GraphQLDirective directive = fd.getDirective(SDLConstants.MAPPING_DIRECTIVE);
        if (directive == null) return null;
        return fd.getDirective(SDLConstants.MAPPING_DIRECTIVE).getArgument(SDLConstants.MAPPING_DIRECTIVE_PROPERTY).getValue().toString();
    }

    public static String getMappedType(String definitionPropertyName, GraphQLFieldDefinition fieldDefinition) {
        GraphQLObjectType graphQLType = (GraphQLObjectType) ((GraphQLList) fieldDefinition.getType()).getWrappedType();
        return graphQLType.getFieldDefinition(definitionPropertyName).getType().getName();
    }
}
