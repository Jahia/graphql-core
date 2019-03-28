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

    public static SDLDefinitionStatus checkType(TypeDefinition type, TypeDefinitionRegistry typeDefinitionRegistry) {
        if (type instanceof ObjectTypeDefinition) {
            ObjectTypeDefinition objectTypeDefinition = (ObjectTypeDefinition) type;
            SDLDefinitionStatus l = checkForFieldsConsistency(objectTypeDefinition, typeDefinitionRegistry);
            if (l.getStatus() != SDLDefinitionStatusType.OK) {
                return l;
            } else if (!objectTypeDefinition.getName().equals("Query")) {
                addDefaultFields(objectTypeDefinition);
            }
        }
        return checkForConsistencyWithJCR(type);
    }

    private static void addDefaultFields(ObjectTypeDefinition objectTypeDefinition) {
        // append id, path and url fields
        List<FieldDefinition> fieldDefinitions = objectTypeDefinition.getFieldDefinitions();
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

    public static void printStatuses(Map<String, SDLDefinitionStatus> statusMap) {
        statusMap.values().forEach(e -> logger.info(e.toString()));
    }

    private static SDLDefinitionStatus checkForFieldsConsistency(ObjectTypeDefinition objectTypeDefinition, TypeDefinitionRegistry typeDefinitionRegistry) {
        if (objectTypeDefinition.getFieldDefinitions().isEmpty()) {
            return new SDLDefinitionStatus(objectTypeDefinition.getName(), SDLDefinitionStatusType.MISSING_FIELDS);
        } else {
            List<String> l = new ArrayList<>();
            objectTypeDefinition.getFieldDefinitions().forEach(fieldDefinition -> {
                if (!typeDefinitionRegistry.getType(fieldDefinition.getType()).isPresent()) {
                    l.add(fieldDefinition.getType().toString());
                }
            });
            if (!l.isEmpty()) {
                return new SDLDefinitionStatus(objectTypeDefinition.getName(), SDLDefinitionStatusType.MISSING_TYPE, StringUtils.join(l, ','));
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
            Directive fieldDirective = field.getDirective(SDLConstants.MAPPING_DIRECTIVE);
            return fieldDirective != null && fieldDirective.getArgument(SDLConstants.MAPPING_DIRECTIVE_PROPERTY) != null;
        }).forEach(field -> {
            Directive fieldDirective = field.getDirective(SDLConstants.MAPPING_DIRECTIVE);
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
        });
    }
}
