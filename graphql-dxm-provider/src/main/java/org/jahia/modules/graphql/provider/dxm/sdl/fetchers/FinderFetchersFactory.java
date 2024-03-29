/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.*;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLConstants;
import org.jahia.modules.graphql.provider.dxm.util.GqlTypeUtil;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.ExtendedPropertyType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.util.Map;
import java.util.stream.Collectors;

public class FinderFetchersFactory {

    private static Logger logger = LoggerFactory.getLogger(FinderFetchersFactory.class);

    public static FinderBaseDataFetcher getFetcher(GraphQLFieldDefinition fieldDefinition, String nodeType) {
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
        String propertyType = getMappedType(definitionPropertyName, propertyNameInJcr, fieldDefinition);
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
            case "Weakreference":
                WeakreferenceFinder weakreferenceFinder = getWeakreferenceFinder(finder, fieldDefinition, definitionPropertyName);
                return getFetcherType(weakreferenceFinder, FetcherType.WEAKREFERENCE);
            default:
                return getFetcherType(finder, FetcherType.STRING);
        }
    }

    public static FinderBaseDataFetcher getFetcherType(final Finder finder, final FetcherType type) {
        switch (type) {
            case ALL:
                return new AllFinderDataFetcher(finder);
            case ID:
                return new ByIdFinderDataFetcher(finder);
            case PATH:
                return new ByPathFinderDataFetcher(finder);
            case DATE:
                return new DateRangeDataFetcher(finder);
            case BOOLEAN:
                return new BooleanFinderDataFetcher(finder);
            case NUMBER:
                return new NumberFinderDataFetcher((NumberFinder) finder);
            case WEAKREFERENCE:
                return new WeakreferenceFinderDataFetcher((WeakreferenceFinder) finder);
            case STRING:
            default:
                return new StringFinderDataFetcher(finder);
        }
    }

    private static String getDefinitionProperty(String queryName) {
        return StringUtils.uncapitalize(StringUtils.substringAfterLast(queryName, "By"));
    }

    private static String getMappedProperty(String definitionPropertyName, GraphQLFieldDefinition fieldDefinition) {
        GraphQLObjectType type = (GraphQLObjectType) ((GraphQLList) fieldDefinition.getType()).getWrappedType();
        GraphQLFieldDefinition fd = type.getFieldDefinition(definitionPropertyName);
        if (fd == null) return null;
        GraphQLDirective directive = fd.getDirective(SDLConstants.MAPPING_DIRECTIVE);
        if (directive == null) return null;
        return fd.getAppliedDirective(SDLConstants.MAPPING_DIRECTIVE).getArgument(SDLConstants.MAPPING_DIRECTIVE_PROPERTY).getValue().toString();
    }

    private static String getMappedType(String definitionPropertyName, String propertyNameInJcr, GraphQLFieldDefinition fieldDefinition) {
        GraphQLObjectType graphQLType = (GraphQLObjectType) ((GraphQLList) fieldDefinition.getType()).getWrappedType();
        GraphQLAppliedDirective mappingDirective = graphQLType.getAppliedDirective(SDLConstants.MAPPING_DIRECTIVE);
        if (mappingDirective != null) {
            String nodeType = mappingDirective.getArgument(SDLConstants.MAPPING_DIRECTIVE_NODE).getValue().toString();
            ExtendedNodeType type = null;
            try {
                type = NodeTypeRegistry.getInstance().getNodeType(nodeType);
                ExtendedPropertyDefinition propDef = type.getPropertyDefinition(propertyNameInJcr);
                if (propDef != null && ExtendedPropertyType.WEAKREFERENCE == propDef.getRequiredType()) {
                    return "Weakreference";
                }
            } catch (NoSuchNodeTypeException e) {
                logger.error("Node type is not found due to", e);
            }
        }

        GraphQLType type = graphQLType.getFieldDefinition(definitionPropertyName).getType();
        return GqlTypeUtil.getTypeName(type);
    }

    private static WeakreferenceFinder getWeakreferenceFinder(Finder finder, GraphQLFieldDefinition fieldDefinition, String definitionPropertyName) {
        //Here I grab jcr type from directive of the weakreference type and set a few properties on finer
        WeakreferenceFinder weakrefFinder = WeakreferenceFinder.fromFinder(finder);
        GraphQLObjectType graphQLType = (GraphQLObjectType) ((GraphQLList) fieldDefinition.getType()).getWrappedType();
        GraphQLFieldDefinition field = graphQLType.getFieldDefinition(definitionPropertyName);
        GraphQLOutputType fieldType = field.getType();
// TODO
//        GraphQLDirective directive = (GraphQLDirective) fieldType.getChildren()
//                .stream()
//                .filter(type -> type instanceof GraphQLDirective && ((GraphQLDirective) type).getName().equals(SDLConstants.MAPPING_DIRECTIVE) && ((GraphQLDirective) type).getArgument(SDLConstants.MAPPING_DIRECTIVE_NODE) != null)
//                .findFirst()
//                .orElse(null);
//
//        if (directive != null) {
//            String nodeTypeOfWeakreference = directive.getArgument(SDLConstants.MAPPING_DIRECTIVE_NODE).getValue().toString();
//            weakrefFinder.setReferencedType(nodeTypeOfWeakreference);
//            Map<String, String> referenceProps = fieldType.getChildren()
//                    .stream()
//                    .filter(type -> type instanceof GraphQLFieldDefinition
//                            && ((GraphQLFieldDefinition) type).getType() instanceof GraphQLScalarType
//                            && ((GraphQLFieldDefinition) type).getAppliedDirective(SDLConstants.MAPPING_DIRECTIVE) != null)
//                    .collect(Collectors.toMap(type -> ((GraphQLFieldDefinition) type).getName(), type -> ((GraphQLFieldDefinition) type).getAppliedDirective(SDLConstants.MAPPING_DIRECTIVE).getArgument(SDLConstants.MAPPING_DIRECTIVE_PROPERTY).getValue().toString()));
//            weakrefFinder.setReferenceTypeProps(referenceProps);
//            weakrefFinder.setReferencedTypeSDLName(GqlTypeUtil.getTypeName(fieldType));
//        }

        return weakrefFinder;
    }

    public enum FetcherType {
        ALL("all", null),
        ID(null, "ById"),
        PATH(null, "ByPath"),
        DATE(),
        NUMBER(),
        BOOLEAN(),
        STRING(),
        WEAKREFERENCE();

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
}
