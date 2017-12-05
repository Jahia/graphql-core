package org.jahia.modules.graphql.provider.dxm.nodetype;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.connection.GraphQLConnection;
import graphql.annotations.connection.PaginatedData;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.modules.graphql.provider.dxm.relay.PaginationHelper;
import org.jahia.services.content.nodetypes.ExtendedNodeDefinition;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.utils.LanguageCodeConverters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO Comment me
 *
 * @author toto
 */
@GraphQLName("JCRNodeType")
public class GqlJcrNodeType {
    public static final Logger logger = LoggerFactory.getLogger(GqlJcrNodeType.class);

    private ExtendedNodeType nodeType;
    private String name;
    private String systemId;
    private boolean isMixin;
    private boolean isAbstract;
    private boolean hasOrderableChildNodes;
    private boolean isQueryable;

    public GqlJcrNodeType(ExtendedNodeType nodeType) {
        this.nodeType = nodeType;
        this.name = nodeType.getName();
        this.systemId = nodeType.getSystemId();
        this.isMixin = nodeType.isMixin();
        this.isAbstract = nodeType.isAbstract();
        this.hasOrderableChildNodes = nodeType.hasOrderableChildNodes();
        this.isQueryable = nodeType.isQueryable();
    }

    public ExtendedNodeType getNodeType() {
        return nodeType;
    }

    @GraphQLField()
    public String getName() {
        return name;
    }

    @GraphQLField()
    public String getDisplayName(@GraphQLName("language") String language) {
        return nodeType.getLabel(LanguageCodeConverters.languageCodeToLocale(language));

    }

    @GraphQLField
    public String getSystemId() {
        return systemId;
    }

    @GraphQLField
    public boolean isMixin() {
        return isMixin;
    }

    @GraphQLField
    public boolean isAbstract() {
        return isAbstract;
    }

    @GraphQLField
    public boolean isHasOrderableChildNodes() {
        return hasOrderableChildNodes;
    }

    @GraphQLField
    public boolean isQueryable() {
        return isQueryable;
    }


    @GraphQLField
    public List<GqlJcrPropertyDefinition> getProperties() {
        List<GqlJcrPropertyDefinition> propertyList = null;
        try {
            ExtendedNodeType ent = NodeTypeRegistry.getInstance().getNodeType(nodeType.getName());
            propertyList = new ArrayList<>();
            for (ExtendedPropertyDefinition definition : ent.getPropertyDefinitions()) {
                GqlJcrPropertyDefinition qlPropertyDefinition = new GqlJcrPropertyDefinition();
                qlPropertyDefinition.setName(definition.getName());
                propertyList.add(qlPropertyDefinition);
            }
        } catch (NoSuchNodeTypeException e) {
            logger.error(e.getMessage(), e);
        }
        return propertyList;

    }

    @GraphQLField
    public List<GqlJcrNodeDefinition> getNodes() {
        List<GqlJcrNodeDefinition> nodeList = null;
        try {
            ExtendedNodeType ent = NodeTypeRegistry.getInstance().getNodeType(nodeType.getName());
            nodeList = new ArrayList<>();
            for (ExtendedNodeDefinition definition : ent.getChildNodeDefinitions()) {
                GqlJcrNodeDefinition qlNodeDefinition = new GqlJcrNodeDefinition();
                qlNodeDefinition.setName(definition.getName());
                nodeList.add(qlNodeDefinition);
            }
        } catch (NoSuchNodeTypeException e) {
            logger.error(e.getMessage(), e);
        }
        return nodeList;

    }

    @GraphQLField
    @GraphQLConnection(connection = DXPaginatedDataConnectionFetcher.class)
    public PaginatedData<GqlJcrNodeType> getSubTypes(DataFetchingEnvironment environment) {
        List<GqlJcrNodeType> subTypes = null;
        try {
            ExtendedNodeType ent = NodeTypeRegistry.getInstance().getNodeType(nodeType.getName());
            subTypes = new ArrayList<>();
            for (ExtendedNodeType type : ent.getSubtypesAsList()) {
                subTypes.add(new GqlJcrNodeType(type));
            }
        } catch (NoSuchNodeTypeException e) {
            logger.error(e.getMessage(), e);
        }
        return PaginationHelper.paginate(subTypes, t -> PaginationHelper.encodeCursor(t.name), environment);


    }

    @GraphQLField
    public List<GqlJcrNodeType> getSuperTypes() {
        List<GqlJcrNodeType> superTypes = null;
        try {
            ExtendedNodeType ent = NodeTypeRegistry.getInstance().getNodeType(nodeType.getName());
            superTypes = new ArrayList<>();
            for (ExtendedNodeType type : ent.getSupertypeSet()) {
                superTypes.add(new GqlJcrNodeType(type));
            }
        } catch (NoSuchNodeTypeException e) {
            logger.error(e.getMessage(), e);
        }
        return superTypes;

    }


}
