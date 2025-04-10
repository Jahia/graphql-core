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
package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.language.*;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLConstants;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.status.SDLDefinitionStatus;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.status.SDLDefinitionStatusType;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.util.*;

import static graphql.Scalars.GraphQLString;

public class SDLTypeChecker {

    private static Logger logger = LoggerFactory.getLogger(SDLTypeChecker.class);

    private SDLTypeChecker() {
    }

    public static SDLDefinitionStatus checkType(SDLSchemaService sdlSchemaService, TypeDefinition type, TypeDefinitionRegistry typeDefinitionRegistry) {
        if (type instanceof ObjectTypeDefinition) {
            ObjectTypeDefinition objectTypeDefinition = (ObjectTypeDefinition) type;
            SDLDefinitionStatus l = checkForFieldsConsistency(sdlSchemaService, objectTypeDefinition, typeDefinitionRegistry);
            if (l.getStatus() != SDLDefinitionStatusType.OK) {
                return l;
            } else if (!objectTypeDefinition.getName().equals("Query")) {
                addDefaultFields(objectTypeDefinition);
            }
        }
        return checkForConsistencyWithJCR(type);
    }

    // @TODO revisit this functionality in the future and extract out of type checker, as it does not fit with the named implication of this class.
    private static void addDefaultFields(ObjectTypeDefinition objectTypeDefinition) {
        // append id, path and url fields
        // TODO list of fields cannot be modified anymore
        List<FieldDefinition> fieldDefinitions = new ArrayList<>(objectTypeDefinition.getFieldDefinitions());
        FieldDefinition uuid = FieldDefinition.newFieldDefinition()
                .name("uuid")
                .type(TypeName.newTypeName(GraphQLString.getName()).build())
                .directive(Directive.newDirective()
                        .name(SDLConstants.MAPPING_DIRECTIVE)
                        .arguments(Collections.singletonList(Argument.newArgument()
                                .name(SDLConstants.MAPPING_DIRECTIVE_PROPERTY)
                                .value(new StringValue(SDLConstants.IDENTIFIER))
                                .build()))
                        .build())
                .build();

        if (fieldDefinitions.stream().noneMatch(definition -> definition.getName().equals(uuid.getName()))) {
            fieldDefinitions.add(uuid);
        }

        FieldDefinition path = FieldDefinition.newFieldDefinition()
                .name("path")
                .type(TypeName.newTypeName(GraphQLString.getName()).build())
                .directive(Directive.newDirective()
                        .name(SDLConstants.MAPPING_DIRECTIVE)
                        .arguments(Collections.singletonList(Argument.newArgument()
                                .name(SDLConstants.MAPPING_DIRECTIVE_PROPERTY)
                                .value(new StringValue(SDLConstants.PATH))
                                .build()))
                        .build())
                .build();

        if (fieldDefinitions.stream().noneMatch(definition -> definition.getName().equals(path.getName()))) {
            fieldDefinitions.add(path);
        }

        FieldDefinition url = FieldDefinition.newFieldDefinition()
                .name("url")
                .type(TypeName.newTypeName(GraphQLString.getName()).build())
                .directive(Directive.newDirective()
                        .name(SDLConstants.MAPPING_DIRECTIVE)
                        .arguments(Collections.singletonList(Argument.newArgument()
                                .name(SDLConstants.MAPPING_DIRECTIVE_PROPERTY)
                                .value(new StringValue(SDLConstants.URL))
                                .build()))
                        .build())
                .build();

        List<Directive> directives = objectTypeDefinition.getDirectives();
        //Add url field definition only if the node type of subtype is nt:file
        if (fieldDefinitions.stream().noneMatch(definition -> definition.getName().equals(url.getName()))
                && directives.stream().anyMatch(directive -> directive.getName().equals(SDLConstants.MAPPING_DIRECTIVE)
                && directive.getArguments().stream().anyMatch(arg -> arg.getName().equals(SDLConstants.MAPPING_DIRECTIVE_NODE)
                && isNodeOfTypeFile(((StringValue) arg.getValue()).getValue())))) {
            fieldDefinitions.add(url);
        }
    }

    private static boolean isNodeOfTypeFile(String type) {
        //Check if the supplied string of comma delimited node(s) is a type or subtype of nt:file
        return Arrays.stream(type.split(",")).anyMatch(s -> {
            try {
                NodeTypeRegistry registry = NodeTypeRegistry.getInstance();
                return registry.hasNodeType(s.trim()) && registry.getNodeType(s.trim()).isNodeType(Constants.NT_FILE);
            } catch (RepositoryException ex) {
                return false;
            }
        });
    }

    private static SDLDefinitionStatus checkForConsistencyWithJCR(TypeDefinition typeDefinition) {
        SDLDefinitionStatus status = new SDLDefinitionStatus(typeDefinition.getName(), SDLDefinitionStatusType.OK);
        List<Directive> directives = typeDefinition.getDirectives();
        for (Directive directive : directives) {
            if (directive.getName().equals(SDLConstants.MAPPING_DIRECTIVE) && directive.getArgument(SDLConstants.MAPPING_DIRECTIVE_NODE) != null) {
                String[] jcrNodeTypes = ((StringValue) directive.getArgument(SDLConstants.MAPPING_DIRECTIVE_NODE).getValue()).getValue().split(",");
                ExtendedNodeType[] allTypes;
                try {
                    allTypes = checkNodeTypes(jcrNodeTypes, status);
                } catch (NoSuchNodeTypeException ex) {
                    return status;
                }
                //Check field directives and make sure all properties can be found on the jcr node type
                List<FieldDefinition> fields = ((ObjectTypeDefinition) typeDefinition).getFieldDefinitions();
                List<String> missingProps = new ArrayList<>();
                List<String> missingChildren = new ArrayList<>();

                checkMissingChildrenOrProps(allTypes, fields, missingProps, missingChildren);

                if (!missingProps.isEmpty()) {
                    status.setStatusType(SDLDefinitionStatusType.MISSING_JCR_PROPERTY, StringUtils.join(missingProps, ","));
                }
                if (!missingChildren.isEmpty()) {
                    status.setStatusType(SDLDefinitionStatusType.MISSING_JCR_CHILD, StringUtils.join(missingChildren, ","));
                }
            }
        }
        return status;
    }

    /**
     * Print and log SDL definition status.
     * If status is not OK, it will be logged as error. Otherwise, it logs status only if debug is enabled.
     * @param statuses list of statuses to log
     */
    public static void printStatuses(Collection<SDLDefinitionStatus> statuses) {
        statuses.forEach(e -> {
            if (e.getStatus() != SDLDefinitionStatusType.OK) {
                logger.error(e.toString());
            } else if (logger.isDebugEnabled()) {
                logger.debug(e.toString());
            }
        });
    }

    private static SDLDefinitionStatus checkForFieldsConsistency(SDLSchemaService sdlSchemaService, ObjectTypeDefinition objectTypeDefinition, TypeDefinitionRegistry typeDefinitionRegistry) {
        if (objectTypeDefinition.getFieldDefinitions().isEmpty()) {
            return new SDLDefinitionStatus(objectTypeDefinition.getName(), SDLDefinitionStatusType.MISSING_FIELDS);
        } else {
            List<String> l = new ArrayList<>();
            objectTypeDefinition.getFieldDefinitions().forEach(fieldDefinition -> {
                if (!typeDefinitionRegistry.getType(fieldDefinition.getType()).isPresent()) {
                    l.add(fieldDefinition.getType().toString());
                }
            });
            //Verify that type exists in definition registry
            if (!l.isEmpty()) {
                return new SDLDefinitionStatus(objectTypeDefinition.getName(), SDLDefinitionStatusType.MISSING_TYPE, StringUtils.join(l, ','));
            }
            //Validate that if custom fetchers are declared (through fetcher directive) they exist in the map of registered fetchers.
            if (sdlSchemaService != null) {
                return objectTypeDefinition.getFieldDefinitions()
                        .stream()
                        //Validate each fetcher we cant return null, so valid fetchers will return a status of SDLDefinitionStatusType.OK(which will be filtered out after)
                        .map(fieldDefinition -> checkForInvalidFetcherDirective(sdlSchemaService, objectTypeDefinition, fieldDefinition))
                        //Remove valid fetchers from stream in order to identify if there are any invalid fetchers present.
                        .filter(sdlDefinitionStatus -> sdlDefinitionStatus.getStatus() != SDLDefinitionStatusType.OK)
                        //If invalid fetcher is found we will return the first one we encountered
                        // SDLDefinitionStatusType.MISSING_FETCHER_ARGUMENT or SDLDefinitionStatusType.MISSING_FETCHER
                        .findFirst()
                        //If everything is good return SDLDefinitionStatusType.OK status for this type
                        .orElse(new SDLDefinitionStatus(objectTypeDefinition.getName(), SDLDefinitionStatusType.OK));
            } else {
                logger.warn("SDLSchemaService is unavailable! Cannot perform fetcher validation for schema!");
            }
        }
        return new SDLDefinitionStatus(objectTypeDefinition.getName(), SDLDefinitionStatusType.OK);
    }

    private static boolean hasProperty(ExtendedNodeType[] nodeTypes, String jcrPropertyName) {
        for (ExtendedNodeType type : nodeTypes) {
            if (type.getPropertyDefinition(jcrPropertyName) != null) return true;
        }
        return false;
    }

    private static boolean hasChildren(ExtendedNodeType[] nodeTypes, String jcrPropertyName) {
        for (ExtendedNodeType type : nodeTypes) {
            if (type.getChildNodeDefinitionsAsMap().containsKey(jcrPropertyName)) return true;
        }
        return false;
    }

    private static ExtendedNodeType[] checkNodeTypes(String[] jcrNodeTypes, SDLDefinitionStatus status) throws NoSuchNodeTypeException {
        ExtendedNodeType[] allTypes = null;
        for (String jcrNodeType : jcrNodeTypes) {
            if (!NodeTypeRegistry.getInstance().hasNodeType(jcrNodeType)) {
                status.setStatusType(SDLDefinitionStatusType.MISSING_JCR_NODE_TYPE, jcrNodeType);
                throw new NoSuchNodeTypeException();
            }
            ExtendedNodeType nodeType = NodeTypeRegistry.getInstance().getNodeType(jcrNodeType);
            ExtendedNodeType[] superTypes = nodeType.getSupertypes();
            JahiaTemplatesPackage jahiaTemplatesPackage = nodeType.getTemplatePackage();
            if (jahiaTemplatesPackage != null) {
                status.setMappedTypeModuleId(jahiaTemplatesPackage.getBundle().getSymbolicName());
                status.setMappedTypeModuleName(jahiaTemplatesPackage.getName());
            }
            status.setMapsToType(jcrNodeType);
            if (allTypes == null) {
                allTypes = (ExtendedNodeType[]) ArrayUtils.add(superTypes, nodeType);
            } else {
                allTypes = (ExtendedNodeType[]) ArrayUtils.addAll(allTypes, ArrayUtils.add(superTypes, nodeType));
            }
        }
        return allTypes;
    }

    private static void checkMissingChildrenOrProps(ExtendedNodeType[] allTypes, List<FieldDefinition> fields, List<String> missingChildren, List<String> missingProps) {
        fields.stream().filter(field -> {
            List<Directive> fieldDirectives = field.getDirectives(SDLConstants.MAPPING_DIRECTIVE);
            return fieldDirectives.stream().allMatch(fd -> fd != null && fd.getArgument(SDLConstants.MAPPING_DIRECTIVE_PROPERTY) != null);
        }).forEach(field -> {
            List<Directive> fieldDirectives = field.getDirectives(SDLConstants.MAPPING_DIRECTIVE);
            for (Directive fieldDirective: fieldDirectives) {
                String jcrPropertyName = ((StringValue) fieldDirective.getArgument(SDLConstants.MAPPING_DIRECTIVE_PROPERTY).getValue()).getValue();
                if (!SDLConstants.PATH.equalsIgnoreCase(jcrPropertyName)
                        && !SDLConstants.IDENTIFIER.equalsIgnoreCase(jcrPropertyName)
                        && !SDLConstants.URL.equalsIgnoreCase(jcrPropertyName)
                        && !SDLConstants.MAPPING_DIRECTIVE_FAKE_PROPERTY.equalsIgnoreCase(jcrPropertyName)) {
                    String propertyName = StringUtils.substringBefore(jcrPropertyName, ".");
                    if (!hasChildren(allTypes, propertyName) && !hasProperty(allTypes, propertyName)) {
                        missingChildren.add(jcrPropertyName);
                    }
                }
            }

        });
    }

    private static SDLDefinitionStatus checkForInvalidFetcherDirective(SDLSchemaService sdlSchemaService, ObjectTypeDefinition objectTypeDefinition, FieldDefinition fieldDefinition) {
        List<Directive> fetcherDirectives = fieldDefinition.getDirectives(SDLConstants.FETCHER_DIRECTIVE);
        if (fetcherDirectives != null) {
            for (Directive fetcherDirective: fetcherDirectives) {
                Argument fetcherName = fetcherDirective.getArgument(SDLConstants.FETCHER_DIRECTIVE_NAME);
                if (fetcherName == null) {
                    //Fetcher name argument does not exist, add to report status
                    return new SDLDefinitionStatus(objectTypeDefinition.getName(), SDLDefinitionStatusType.MISSING_FETCHER_ARGUMENT, SDLConstants.FETCHER_DIRECTIVE_NAME, fieldDefinition.getName());

                } else if (fetcherName.getValue() != null && !sdlSchemaService.getPropertyFetcherExtensions().containsKey(((StringValue)fetcherName.getValue()).getValue())) {
                    return new SDLDefinitionStatus(objectTypeDefinition.getName(), SDLDefinitionStatusType.MISSING_FETCHER, ((StringValue)fetcherName.getValue()).getValue());
                }
            }
        }
        return new SDLDefinitionStatus(objectTypeDefinition.getName(), SDLDefinitionStatusType.OK);
    }
}
