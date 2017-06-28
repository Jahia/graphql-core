package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.GraphQLFieldDefinition;
import graphql.servlet.GraphQLQueryProvider;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component(service = FieldsResolver.class)
public class FieldsResolver {
    private static Logger logger = LoggerFactory.getLogger(FieldsResolver.class);
    private static FieldsResolver instance;

    private Map<String, Collection<GraphQLFieldProvider>> fieldProviders = new HashMap<>();
    private Map<String, Collection<GraphQLFieldDefinition>> fieldsDefinition = new HashMap<>();

    public FieldsResolver() {
        logger.info("Init fields resolver");
        instance = this;
    }



    public static FieldsResolver getInstance() {
        return instance;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY, policy = ReferencePolicy.STATIC)
    public void bindFieldProvider(GraphQLFieldProvider fieldProvider) {
        logger.info("Bind field provider for " +fieldProvider.getTypeName() + ", "+fieldProvider.getClass().getName());
        if (!fieldProviders.containsKey(fieldProvider.getTypeName())) {
            fieldProviders.put(fieldProvider.getTypeName(), new ArrayList<GraphQLFieldProvider>());
        }
        fieldProviders.get(fieldProvider.getTypeName()).add(fieldProvider);
    }

    public void unbindFieldProvider(GraphQLFieldProvider fieldProvider) {
        logger.info("Unbind field provider for " +fieldProvider.getTypeName() + ", "+fieldProvider.getClass().getName());
        if (fieldProviders.containsKey(fieldProvider.getTypeName())) {
            fieldProviders.get(fieldProvider.getTypeName()).remove(fieldProvider);
        }
        fieldsDefinition.remove(fieldProvider);
    }

    @Deactivate
    public void deactivate() {
        instance = null;
    }

    public Collection<GraphQLFieldDefinition> getFields(String type) {
        if (!fieldsDefinition.containsKey(type)) {
            if (fieldProviders.containsKey(type)) {
                List<GraphQLFieldDefinition> fields = new ArrayList<>();
                for (GraphQLFieldProvider fieldProvider : fieldProviders.get(type)) {
                    fields.addAll(fieldProvider.getFields());
                }
                fieldsDefinition.put(type, fields);
            } else {
                fieldsDefinition.put(type, Collections.<GraphQLFieldDefinition>emptyList());
            }
        }
        return fieldsDefinition.get(type);
    }

}
