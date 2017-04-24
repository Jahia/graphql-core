package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.GraphQLFieldDefinition;
import org.osgi.service.component.annotations.*;

import java.util.*;

@Component(service = FieldsResolver.class, immediate = true)
public class FieldsResolver {

    private static Map<String, Collection<GraphQLFieldProvider>> fieldProviders = new HashMap<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY, policy = ReferencePolicy.STATIC)
    public void bindQueryProvider(GraphQLFieldProvider fieldProvider) {
        if (!fieldProviders.containsKey(fieldProvider.getTypeName())) {
            fieldProviders.put(fieldProvider.getTypeName(), new ArrayList<GraphQLFieldProvider>());
        }
    }
    public void unbindQueryProvider(GraphQLFieldProvider fieldProvider) {
        fieldProviders.get(fieldProvider.getTypeName()).remove(fieldProvider);
    }

    public static List<GraphQLFieldDefinition> getFields(String type) {
        if (fieldProviders.containsKey(type)) {
            List<GraphQLFieldDefinition> fields = new ArrayList<>();
            for (GraphQLFieldProvider fieldProvider : fieldProviders.get(type)) {
                fields.addAll(fieldProvider.getFields());
            }
            return fields;
        }
        return Collections.emptyList();
    }

}
