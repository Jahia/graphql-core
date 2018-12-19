package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.language.*;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.status.SDLDefinitionStatus;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.status.SDLDefinitionStatusDescription;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.status.SDLDefinitionStatusTypes;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SDLJCRTypeChecker {

    private static Logger logger = LoggerFactory.getLogger(SDLJCRTypeChecker.class);

    /**
     * Check mapping directive node types and remove from type registry definitions which refer to jcr types not
     * found in JCR.
     *
     * @param typeDefinitionRegistry
     */
    public static Map<String, SDLDefinitionStatus> checkForConsistencyWithJCR(TypeDefinitionRegistry typeDefinitionRegistry) {
        Map<String, SDLDefinitionStatus> statuses = new HashMap<>();

        List<String> toremove = new ArrayList<>();
        //Make sure each definition maps to an existing node type
        for (Map.Entry<String, TypeDefinition> entry : typeDefinitionRegistry.types().entrySet()) {
            String definitionName = entry.getValue().getName();
            SDLDefinitionStatus status = new SDLDefinitionStatus(definitionName, SDLDefinitionStatusTypes.OK);
            statuses.put(definitionName, status);

            List<Directive> directives = entry.getValue().getDirectives();
            for (Directive directive : directives) {
                if (directive.getName().equals("mapping") && directive.getArgument("node") != null) {
                    String jcrNodeType = ((StringValue)directive.getArgument("node").getValue()).getValue();
                    if (!NodeTypeRegistry.getInstance().hasNodeType(jcrNodeType)) {
                        toremove.add(entry.getKey());
                        status.setStatusType(SDLDefinitionStatusTypes.MISSING_JCR_NODE_TYPE);
                        status.setStatusDescription(new SDLDefinitionStatusDescription(jcrNodeType));
                    }
                    else {
                        try {
                            ExtendedNodeType jcrNode = NodeTypeRegistry.getInstance().getNodeType(jcrNodeType);
                            JahiaTemplatesPackage jahiaTemplatesPackage = jcrNode.getTemplatePackage();
                            if (jahiaTemplatesPackage != null) {
                                status.setMappedTypeModuleId(jahiaTemplatesPackage.getBundle().getSymbolicName());
                                status.setMappedTypeModuleName(jahiaTemplatesPackage.getName());
                            }
                            status.setMapsToType(jcrNodeType);

                            //Check field directives and make sure all properties can be found on the jcr node type
                            List<FieldDefinition> fields = ((ObjectTypeDefinition) entry.getValue()).getFieldDefinitions();
                            for (FieldDefinition def : fields) {
                                for (Directive fieldDirective : def.getDirectives()) {
                                    if (fieldDirective.getName().equals("mapping") && fieldDirective.getArgument("property") != null) {
                                        String jcrProperty = ((StringValue)fieldDirective.getArgument("property").getValue()).getValue();
                                            if (jcrNode.getPropertyDefinition(jcrProperty) == null) {
                                                toremove.add(entry.getKey());
                                                status.setStatusType(SDLDefinitionStatusTypes.MISSING_JCR_PROPERTY);
                                                status.setStatusDescription(new SDLDefinitionStatusDescription(jcrProperty));
                                                break;
                                            }
                                    }
                                }
                            }
                        } catch (NoSuchNodeTypeException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        for (String removedType : toremove) {
            if (typeDefinitionRegistry.getType(removedType).isPresent()) {
                ObjectTypeDefinition def = (ObjectTypeDefinition) typeDefinitionRegistry.getType(removedType).get();
                typeDefinitionRegistry.remove(typeDefinitionRegistry.getType(removedType).get());
                typeDefinitionRegistry.add(new CustomTypeDefinition(def, statuses.get(def.getName()).toString()));
                List<FieldDefinition> fieldDefinitions = typeDefinitionRegistry.objectTypeExtensions().get("Query").get(0).getFieldDefinitions();
                fieldDefinitions.removeIf(fd -> {
                    if (fd.getType() instanceof ListType) {
                        return ((TypeName) ((ListType) fd.getType()).getType()).getName().equals(removedType);
                    }
                    //TODO handle other types if applicable
                    return false;
                });
            }
        }

        //Verify that types which are being extended exist, remove those extensions that do not extend an existing type
        HashMap<String, ObjectTypeDefinition> types = (HashMap<String, ObjectTypeDefinition>) typeDefinitionRegistry.getTypesMap(ObjectTypeDefinition.class);
        for (Map.Entry<String, List<ObjectTypeExtensionDefinition>> entry : typeDefinitionRegistry.objectTypeExtensions().entrySet()) {
            if (!types.containsKey(entry.getKey())) {
                    typeDefinitionRegistry.remove(entry.getValue().get(0));
                logger.warn("Extension of type [" + entry.getKey() + "]" + " does not exist... removing from type registry.");
            }
        }

        printStatuses(statuses);

        return statuses;
    }

    private static void printStatuses(Map<String, SDLDefinitionStatus> statusMap) {
        for (Map.Entry<String, SDLDefinitionStatus> entry : statusMap.entrySet())
            logger.info(entry.getValue().toString());
    }

    private static class CustomTypeDefinition extends ObjectTypeDefinition {
        CustomTypeDefinition(ObjectTypeDefinition toClone, String description) {
            super(toClone.getName(),
                    toClone.getImplements(),
                    toClone.getDirectives(),
                    toClone.getFieldDefinitions(),
                    new Description(description, new SourceLocation(1, 1), true),
                    toClone.getSourceLocation(),
                    toClone.getComments());

        }
    }
}
