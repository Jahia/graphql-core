package org.jahia.modules.graphql.provider.dxm.builder;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import graphql.schema.*;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLConnection;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLEdge;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLPageInfo;

import javax.annotation.Nullable;
import java.util.*;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

public abstract class DXGraphQLBuilder<T> {

    protected GraphQLOutputType type;
    protected GraphQLOutputType edgeType;
    protected GraphQLOutputType connectionType;

    public abstract String getName();

    public GraphQLOutputType getType() {
        if (type == null) {
            type = newObject()
                    .name(getName())
                    .fields(getAllFields())
                    .build();
        }
        return type;
    }

    protected List<GraphQLFieldDefinition> getAllFields() {
        List<GraphQLFieldDefinition> l = new ArrayList<>(getFields());
        if (FieldsResolver.getInstance() != null) {
            l.addAll(FieldsResolver.getInstance().getFields(getName()));
        }
        return l;
    }

    protected abstract List<GraphQLFieldDefinition> getFields();

}
