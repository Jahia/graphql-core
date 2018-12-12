package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.language.*;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JCRTypeChecker {

    /**
     * Check mapping directive node types and remove from type registry definitions which refer to jcr types not
     * found in JCR.
     *
     * @param typeDefinitionRegistry
     */
    public static List<ObjectTypeDefinition> removeNonExistentJCRTypes(TypeDefinitionRegistry typeDefinitionRegistry) {
        List<ObjectTypeDefinition> definitionsWithMissingJCRTypes = new ArrayList<>();

        List<String> toremove = new ArrayList<>();
        for (Map.Entry<String, TypeDefinition> entry : typeDefinitionRegistry.types().entrySet()) {
            List<Directive> directives = entry.getValue().getDirectives();
            for (Directive directive : directives) {
                if (directive.getName().equals("mapping") && directive.getArgument("node") != null) {
                    String jcrNodeType = ((StringValue)directive.getArgument("node").getValue()).getValue();
                    if (!NodeTypeRegistry.getInstance().hasNodeType(jcrNodeType)) {
                        toremove.add(entry.getKey());
                    }
                }
            }
        }

        for (String removedType : toremove) {
            if (typeDefinitionRegistry.getType(removedType).isPresent()) {
                ObjectTypeDefinition def = (ObjectTypeDefinition) typeDefinitionRegistry.getType(removedType).get();
                typeDefinitionRegistry.remove(typeDefinitionRegistry.getType(removedType).get());
                typeDefinitionRegistry.add(new CustomTypeDefinition(def, "No JCR type was found for this type. Fix SDL and redeploy your module."));
                definitionsWithMissingJCRTypes.add(def);
                List<FieldDefinition> fieldDefinitions = typeDefinitionRegistry.objectTypeExtensions().get("Query").get(0).getFieldDefinitions();
                fieldDefinitions.removeIf(fd -> {
                    if (fd.getType() instanceof ListType) {
                        return ((TypeName) ((ListType) fd.getType()).getType()).getName().equals(removedType);
                    }
                    //TODO handle other types
                    return false;
                });
            }
        }

        return definitionsWithMissingJCRTypes;
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
