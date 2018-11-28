package org.jahia.modules.graphql.provider.dxm.customApi;

import graphql.schema.*;
import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.util.*;
import java.util.stream.Collectors;

import static graphql.Scalars.*;

public class CustomApi {

    private static final Logger logger = LoggerFactory.getLogger(CustomApi.class);

    private String name;

    private String nodeType;

    private Map<String, Field> fields = new HashMap<>();
    private Map<String, Finder> finders = new HashMap<>();


    private GraphQLObjectType graphQLObjectType;

    public CustomApi(String name) {
        this.name = name;

        finders.put("byId", new Finder("byId"));
        finders.put("byPath", new Finder("byPath"));
        finders.put("all", new Finder("all"));
    }

    public String getName() {
        return name;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public Field getField(String name) {
        return fields.get(name);
    }

    public Collection<Field> getFields() {
        return fields.values();
    }

    public void addField(String name, Field field) {
        fields.put(name, field);
    }

    public Finder getFinder(String name) {
        return finders.get(name);
    }

    public void addFinder(String name, Finder finder) {
        finders.put(name, finder);
    }

    public Collection<Finder> getFinders() {
        return finders.values();
    }

    public List<GraphQLFieldDefinition> getQueryFields() {
        return finders.values().stream().map(this::getFinderDataFetcher).map(finderDataFetcher ->
                GraphQLFieldDefinition.newFieldDefinition()
                        .name(finderDataFetcher.getName())
                        .dataFetcher(finderDataFetcher)
                        .argument(finderDataFetcher.getArguments())
                        .type(finderDataFetcher.getObjectType()) // todo return a connection to type if finder is multiple
                        .build()
        ).collect(Collectors.toList());
    }

    public FinderDataFetcher getFinderDataFetcher(Finder finder) {
        if (finder.getName().equals("byId")) {
            return new ByIdFinderDataFetcher(this,finder);
        } else if (finder.getName().equals("byPath")) {
            return new ByPathFinderDataFetcher(this,finder);
        } else if (finder.getName().equals("all")) {
            return new AllFinderDataFetcher(this,finder);
        } else if (finder.getName().startsWith("by")) {
            if (finder.isMultiple()) {
                return new ByPropertyMultipleFinderDataFetcher(this,finder);
            } else {
                return new ByPropertySingleFinderDataFetcher(this,finder);
            }
        }
        return null;
    }

    public GraphQLObjectType getObjectType() {
        if (graphQLObjectType == null) {
            try {
                ExtendedNodeType type = NodeTypeRegistry.getInstance().getNodeType(getNodeType());
                Map<String, ExtendedPropertyDefinition> extendedPropertyDefinitionMap = type.getPropertyDefinitionsAsMap();
                graphQLObjectType = GraphQLObjectType.newObject()
                        .name(name)
                        .fields(fields.values().stream().map(value->
                                GraphQLFieldDefinition.newFieldDefinition()
                                        .name(value.getName())
                                        .dataFetcher(new PropertiesDataFetcher(this, value))
                                        .argument(extendedPropertyDefinitionMap.get(value.getProperty()).isInternationalized() ?
                                                Collections.singletonList(GraphQLArgument.newArgument().name("language").type(new GraphQLNonNull(GraphQLString)).build()) :
                                                Collections.emptyList())
                                        .type(getGraphQLType(extendedPropertyDefinitionMap.get(value.getProperty())))
                                        .build()
            ).collect(Collectors.toList()))
                        .build();
            } catch (NoSuchNodeTypeException e) {
                throw new RuntimeException(e);
            }
        }
        return graphQLObjectType;
    }

    private static GraphQLOutputType getGraphQLType(ExtendedPropertyDefinition propertyDefinition) {
        GraphQLOutputType type;
        switch (propertyDefinition.getRequiredType()) {
            case PropertyType.BOOLEAN:
                type = GraphQLBoolean;
                break;
            case PropertyType.DATE:
            case PropertyType.DECIMAL:
            case PropertyType.LONG:
                type = GraphQLLong;
                break;
            case PropertyType.DOUBLE:
                type = GraphQLFloat;
                break;
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                // TODO
                type = GraphQLString;
                break;
            case PropertyType.BINARY:
            case PropertyType.NAME:
            case PropertyType.PATH:
            case PropertyType.STRING:
            case PropertyType.UNDEFINED:
            case PropertyType.URI:
                type = GraphQLString;
                break;
            default:
                logger.warn("Couldn't find equivalent GraphQL type for "
                        + propertyDefinition.getName()
                        + " property type will use string type instead!");
                type = GraphQLString;
        }

        return propertyDefinition.isMultiple() ? new GraphQLList(type) : type;
    }

}
