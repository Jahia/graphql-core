package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.language.*;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.status.SDLDefinitionStatus;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.status.SDLDefinitionStatusType;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SDLTypeChecker {

    private static Logger logger = LoggerFactory.getLogger(SDLTypeChecker.class);

    public static SDLDefinitionStatus checkType(TypeDefinition type, TypeDefinitionRegistry typeDefinitionRegistry) {
        if (type instanceof ObjectTypeDefinition) {
            ObjectTypeDefinition objectTypeDefinition = (ObjectTypeDefinition) type;
            SDLDefinitionStatus l = checkForFieldsConsistency(objectTypeDefinition, typeDefinitionRegistry);
            if (l.getStatus() != SDLDefinitionStatusType.OK) {
                return l;
            }
        }
        return checkForConsistencyWithJCR(type);
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

    public static SDLDefinitionStatus checkForConsistencyWithJCR(TypeDefinition typeDefinition) {
        SDLDefinitionStatus status = new SDLDefinitionStatus(typeDefinition.getName(), SDLDefinitionStatusType.OK);
        List<Directive> directives = typeDefinition.getDirectives();
        for (Directive directive : directives) {
            if (directive.getName().equals("mapping") && directive.getArgument("node") != null) {
                String jcrNodeType = ((StringValue) directive.getArgument("node").getValue()).getValue();
                try {
                    ExtendedNodeType nodeType = NodeTypeRegistry.getInstance().getNodeType(jcrNodeType);
                    ExtendedNodeType[] superTypes = nodeType.getSupertypes();
                    JahiaTemplatesPackage jahiaTemplatesPackage = nodeType.getTemplatePackage();
                    if (jahiaTemplatesPackage != null) {
                        status.setMappedTypeModuleId(jahiaTemplatesPackage.getBundle().getSymbolicName());
                        status.setMappedTypeModuleName(jahiaTemplatesPackage.getName());
                    }
                    status.setMapsToType(jcrNodeType);

                    //Check field directives and make sure all properties can be found on the jcr node type
                    List<FieldDefinition> fields = ((ObjectTypeDefinition) typeDefinition).getFieldDefinitions();
                    List<String> missing = new ArrayList<>();
                    for (FieldDefinition def : fields) {
                        Directive fieldDirective = def.getDirective("mapping");
                        if (fieldDirective != null && fieldDirective.getArgument("property") != null) {
                            String jcrPropertyName = ((StringValue) fieldDirective.getArgument("property").getValue()).getValue();
                            if (!hasProperty((ExtendedNodeType[]) ArrayUtils.add(superTypes, nodeType), jcrPropertyName)) {
                                missing.add(jcrPropertyName);
                            }
                        }
                    }
                    if (!missing.isEmpty()) {
                        status.setStatusType(SDLDefinitionStatusType.MISSING_JCR_PROPERTY, StringUtils.join(missing, ","));
                    }
                } catch (NoSuchNodeTypeException e) {
                    status.setStatusType(SDLDefinitionStatusType.MISSING_JCR_NODE_TYPE, jcrNodeType);
                }
            }
        }
        return status;
    }

    public static void printStatuses(Map<String, SDLDefinitionStatus> statusMap) {
        statusMap.values().forEach(e -> logger.info(e.toString()));
    }

    private static boolean hasProperty(ExtendedNodeType[] nodeTypes, String jcrPropertyName) {
        for (ExtendedNodeType type : nodeTypes) {
            if (type.getPropertyDefinition(jcrPropertyName) != null) return true;
        }
        return false;
    }
}
