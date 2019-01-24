/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.*;
import graphql.annotations.connection.GraphQLConnection;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.time.DateUtils;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldFiltersInput;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedData;
import org.jahia.modules.graphql.provider.dxm.relay.DXPaginatedDataConnectionFetcher;
import org.jahia.modules.graphql.provider.dxm.util.DefaultConstraintHelper;
import org.jahia.services.content.*;
import org.jahia.services.content.nodetypes.ValueImpl;
import org.jahia.services.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.qom.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;

import static org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeConstraintInput.QueryFunction.*;
import static org.jahia.modules.graphql.provider.dxm.node.GqlJcrQuery.QueryLanguage.SQL2;

/**
 * GraphQL root object for JCR related queries
 */
@GraphQLName("JCRQuery")
@GraphQLDescription("JCR Queries")
public class GqlJcrQuery {

    private static final Logger logger = LoggerFactory.getLogger(GqlJcrQuery.class);

    private static final NodeConstraintConvertor[] NODE_CONSTRAINT_CONVERTORS = {
        new NodeConstraintConvertorLike(),
        new NodeConstraintConvertorContains(),
        new NodeConstraintConvertorEquals(),
        new NodeConstraintConvertorNotEquals(),
        new NodeConstraintConvertorGreaterThan(),
        new NodeConstraintConvertorGreaterThanOrEqualsTo(),
        new NodeConstraintConvertorLessThan(),
        new NodeConstraintConvertorLessThanOrEqualsTo(),
        new NodeConstraintConvertorExists(),
        new NodeConstraintConvertorLastDays()
    };

    private NodeQueryExtensions.Workspace workspace;

    public GqlJcrQuery(NodeQueryExtensions.Workspace workspace) {
        this.workspace = workspace;
    }

    /**
     * JCR query languages available to use for nodes querying.
     */
    @GraphQLDescription("JCR query languages available to use for nodes querying")
    public enum QueryLanguage {

        /**
         * SQL2 query language.
         */
        @GraphQLDescription("SQL2 query language")
        SQL2(Query.JCR_SQL2),

        /**
         * XPath query language.
         */
        @GraphQLDescription("XPath query language")
        XPATH(Query.XPATH);

        private String jcrQueryLanguage;

        private QueryLanguage(String jcrQueryLanguage) {
            this.jcrQueryLanguage = jcrQueryLanguage;
        }

        /**
         * @return Corresponding language value defined by the JCR API
         */
        public String getJcrQueryLanguage() {
            return jcrQueryLanguage;
        }
    }

    /**
     * @return Get the workspace of the query
     */
    @GraphQLField
    @GraphQLName("workspace")
    @GraphQLNonNull
    @GraphQLDescription("Get the workspace of the query")
    public NodeQueryExtensions.Workspace getWorkspace() {
        return workspace;
    }

    /**
     * Get GraphQL representation of a node by its UUID.
     *
     * @param uuid The UUID of the node
     * @return GraphQL representation of the node
     * @throws BaseGqlClientException In case of issues fetching the node
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLName("nodeById")
    @GraphQLDescription("Get GraphQL representation of a node by its UUID")
    public GqlJcrNode getNodeById(@GraphQLName("uuid") @GraphQLNonNull @GraphQLDescription("The UUID of the node") String uuid){
        try {
            return getGqlNodeById(uuid);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Get GraphQL representation of a node by its path.
     *
     * @param path The path of the node
     * @return GraphQL representation of the node
     * @throws BaseGqlClientException In case of issues fetching the node
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLName("nodeByPath")
    @GraphQLDescription("Get GraphQL representation of a node by its path")
    public GqlJcrNode getNodeByPath(@GraphQLName("path") @GraphQLNonNull @GraphQLDescription("The path of the node") String path) {
        try {
            return getGqlNodeByPath(path);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Get GraphQL representations of multiple nodes by their UUIDs.
     *
     * @param uuids The UUIDs of the nodes
     * @return GraphQL representations of the nodes
     * @throws BaseGqlClientException In case of issues fetching the nodes
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLName("nodesById")
    @GraphQLDescription("Get GraphQL representations of multiple nodes by their UUIDs")
    public Collection<GqlJcrNode> getNodesById(@GraphQLName("uuids") @GraphQLNonNull @GraphQLDescription("The UUIDs of the nodes") Collection<@GraphQLNonNull String> uuids){
        try {
            List<GqlJcrNode> nodes = new ArrayList<>(uuids.size());
            for (String uuid : uuids) {
                nodes.add(getGqlNodeById(uuid));
            }
            return nodes;
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Get GraphQL representations of multiple nodes by their paths.
     *
     * @param paths The paths of the nodes
     * @return GraphQL representations of the nodes
     * @throws BaseGqlClientException In case of issues fetching the nodes
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLName("nodesByPath")
    @GraphQLDescription("Get GraphQL representations of multiple nodes by their paths")
    public Collection<GqlJcrNode> getNodesByPath(@GraphQLName("paths") @GraphQLNonNull @GraphQLDescription("The paths of the nodes") Collection<@GraphQLNonNull String> paths){
        try {
            List<GqlJcrNode> nodes = new ArrayList<>(paths.size());
            for (String path : paths) {
                nodes.add(getGqlNodeByPath(path));
            }
            return nodes;
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Get GraphQL representations of nodes using a query language supported by JCR.
     *
     * @param query The query string
     * @param queryLanguage The query language
     * @param language language to access node properties in
     * @param fieldFilter Filter by GraphQL field values
     * @param environment the execution content instance
     * @return GraphQL representations of nodes selected according to the query supplied
     * @throws BaseGqlClientException In case of issues executing the query
     */
    @GraphQLField
    @GraphQLName("nodesByQuery")
    @GraphQLDescription("Get GraphQL representations of nodes using a query language supported by JCR")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    public DXPaginatedData<GqlJcrNode> getNodesByQuery(
            @GraphQLName("query") @GraphQLNonNull @GraphQLDescription("The query string") String query,
            @GraphQLName("queryLanguage") @GraphQLDefaultValue(QueryLanguageDefaultValue.class) @GraphQLDescription("The query language") QueryLanguage queryLanguage,
            @GraphQLName("language") @GraphQLDescription("Language to access node properties in") String language,
            @GraphQLName("fieldFilter") @GraphQLDescription("Filter by graphQL fields values") FieldFiltersInput fieldFilter,
            @GraphQLName("fieldSorter") @GraphQLDescription("sort by GraphQL field values") FieldSorterInput fieldSorter,
            @GraphQLName("fieldGrouping") @GraphQLDescription("Group fields by criteria") FieldGroupingInput fieldGrouping,
            DataFetchingEnvironment environment
    ){
        try {
            QueryManagerWrapper queryManager = getSession(language).getWorkspace().getQueryManager();
            QueryWrapper q = queryManager.createQuery(query, queryLanguage.getJcrQueryLanguage());
            JCRNodeIteratorWrapper nodes = q.execute().getNodes();
            // todo: naive implementation of the pagination, could be improved in some cases by setting limit/offset in query
            return NodeHelper.getPaginatedNodesList(nodes, null, null, null, fieldFilter, environment, fieldSorter, fieldGrouping);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Get GraphQL representations of nodes using a criteria object.
     *
     * @param criteria The criteria to fetch nodes by
     * @param fieldFilter Filter by GraphQL field values
     * @param environment The execution context
     * @return GraphQL representations of nodes fetched
     * @throws BaseGqlClientException In case of issues fetching nodes
     */
    @GraphQLField
    @GraphQLName("nodesByCriteria")
    @GraphQLDescription("handles query nodes with QOM factory")
    @GraphQLConnection(connectionFetcher = DXPaginatedDataConnectionFetcher.class)
    public DXPaginatedData<GqlJcrNode> getNodesByCriteria(
        @GraphQLName("criteria") @GraphQLNonNull @GraphQLDescription("The criteria to fetch nodes by") GqlJcrNodeCriteriaInput criteria,
        @GraphQLName("fieldFilter") @GraphQLDescription("Filter by GraphQL field values") FieldFiltersInput fieldFilter,
        @GraphQLName("fieldSorter") @GraphQLDescription("sort by GraphQL field values") FieldSorterInput fieldSorter,
        @GraphQLName("fieldGrouping") @GraphQLDescription("Group fields by criteria") FieldGroupingInput fieldGrouping,
        DataFetchingEnvironment environment
    ){
        try {
            Session session = getSession(criteria.getLanguage());
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            QueryObjectModelFactory factory = queryManager.getQOMFactory();
            Selector source = factory.selector(criteria.getNodeType(), "node");
            Constraint constraintTree = getConstraintTree(source.getSelectorName(), criteria, factory);
            Ordering ordering = getOrderingByProperty(source.getSelectorName(), criteria, factory);
            QueryObjectModel queryObjectModel = factory.createQuery(source, constraintTree, ordering == null ? null : new Ordering[] {ordering}, null);
            NodeIterator it = queryObjectModel.execute().getNodes();
            return NodeHelper.getPaginatedNodesList(it, null, null, null, fieldFilter, environment, fieldSorter, fieldGrouping);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Generate constraints by criteria input, the principle constraints for nodeType, paths and pathType are unique
     * In the nodeConstraint, it supports single constraint as well as complex constraints with multiple level child constraints
     * which are composite by all/any/none
     *
     * @param selector
     * @param criteria
     * @param factory
     * @return
     * @throws RepositoryException
     */
    private static Constraint getConstraintTree(final String selector, final GqlJcrNodeCriteriaInput criteria,
                                                final QueryObjectModelFactory factory) throws RepositoryException {

        final LinkedHashSet<Constraint> constraints = new LinkedHashSet<>();

        // Add path constraint if any.
        Collection<String> paths = criteria.getPaths();
        if (paths != null && !paths.isEmpty()) {
            Constraint principleConstraint;
            GqlJcrNodeCriteriaInput.PathType pathType = criteria.getPathType();
            if (pathType == null) {
                pathType = GqlJcrNodeCriteriaInput.PathType.ANCESTOR;
            }
            Iterator<String> pathsIt = paths.iterator();
            switch (pathType) {
                case ANCESTOR:
                    principleConstraint = factory.descendantNode(selector, pathsIt.next());
                    while (pathsIt.hasNext()) {
                        principleConstraint = factory.or(principleConstraint, factory.descendantNode(selector, pathsIt.next()));
                    }
                    break;
                case PARENT:
                    principleConstraint = factory.childNode(selector, pathsIt.next());
                    while (pathsIt.hasNext()) {
                        principleConstraint = factory.or(principleConstraint, factory.childNode(selector, pathsIt.next()));
                    }
                    break;
                case OWN:
                    principleConstraint = factory.sameNode(selector, pathsIt.next());
                    while (pathsIt.hasNext()) {
                        principleConstraint = factory.or(principleConstraint, factory.sameNode(selector, pathsIt.next()));
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown path type: " + pathType);
            }
            constraints.add(principleConstraint);
        }

        // Build default properties constraint.
        Constraint result = null;
        if (criteria.getNodeConstraint() != null && criteria.getNodeConstraint().getContains() != null) {
            DefaultConstraintHelper defaultConstraintHelper = new DefaultConstraintHelper(factory, selector);
            result = defaultConstraintHelper.buildDefaultPropertiesConstraint(criteria.getNodeConstraint().getContains());
        }

        // Build the result.
        if (constraints.isEmpty()) {
            return null;
        } else {
            if (criteria.getNodeConstraint()!=null) {
                constraints.add(compositeChildConstraints(selector, criteria.getNodeConstraint(), factory));
            }

            for(Constraint constraint : constraints){
                result = result != null ? factory.and(result, constraint) : constraint;
            }

            logger.debug("Generate composite constraints {} ", result);
            return result;
        }
    }

    /**
     * Support all/any/none composite constraints recursively
     * Only one type of composite constraint is allowed on each level
     * And if child has simple constraint then not allow any composite constraint
     *
     *
     * @param selector
     * @param constraintInput
     * @param factory
     * @return
     * @throws RepositoryException
     */
    private static Constraint compositeChildConstraints(final String selector, final GqlJcrNodeConstraintInput constraintInput,
                                                        final QueryObjectModelFactory factory) throws RepositoryException {
        validateNodeCompositeConstraintConflict(constraintInput);

        final List<GqlJcrNodeConstraintInput> allConstraintInputs = constraintInput.getAll();
        final List<GqlJcrNodeConstraintInput> anyConstraintInputs = constraintInput.getAny();
        final List<GqlJcrNodeConstraintInput> noneConstraintInputs = constraintInput.getNone();

        if (allConstraintInputs!=null && !allConstraintInputs.isEmpty()) {
            return getCompositeConstraints(selector, allConstraintInputs, factory, "all");
        } else if (anyConstraintInputs!=null && !anyConstraintInputs.isEmpty()) {
            return getCompositeConstraints(selector, anyConstraintInputs, factory, "any");
        } else if (noneConstraintInputs!=null && !noneConstraintInputs.isEmpty()) {
            return getCompositeConstraints(selector, noneConstraintInputs, factory, "none");
        } else {
            return convertToConstraint(selector, constraintInput, factory);
        }
    }

    /**
     *
     * @param selector
     * @param constraintInputs
     * @param factory
     * @param operator
     * @return
     * @throws RepositoryException
     */
    private static Constraint getCompositeConstraints(final String selector, List<GqlJcrNodeConstraintInput> constraintInputs,
                                                      final QueryObjectModelFactory factory, String operator) throws RepositoryException {
        Constraint result = null;
        for(GqlJcrNodeConstraintInput subConstraintInput : constraintInputs){
            Constraint subConstraint = compositeChildConstraints(selector, subConstraintInput ,factory);
            switch (operator){
                case "all"  :
                    result = (result!=null ? factory.and(result, subConstraint) : subConstraint);
                    break;
                case "any"  :
                    result = (result!=null ? factory.or(result, subConstraint) : subConstraint);
                    break;
                case "none" :
                    result = (result!=null ? factory.and(result, factory.not(subConstraint)) : factory.not(subConstraint));
                    break;
                default     :
                    //do nothing for default
            }
        }
        return result;
    }

    private static Constraint convertToConstraint(final String selector,
                                                  final GqlJcrNodeConstraintInput nodeConstraintInput,
                                                  final QueryObjectModelFactory factory) throws RepositoryException {
        if (nodeConstraintInput != null) {
            Constraint constraint = null;
            for (NodeConstraintConvertor nodeConstraintConvertor : NODE_CONSTRAINT_CONVERTORS) {
                Constraint c = nodeConstraintConvertor.convert(nodeConstraintInput, factory, selector);
                if (c == null) {
                    continue;
                }
                if (constraint != null) {
                    throwNoneOrMultipleNodeConstraintsException();
                }
                constraint = c;
            }
            if (constraint == null) {
                throwNoneOrMultipleNodeConstraintsException();
            }
            return constraint;
        }
        return null;
    }

    private static Ordering getOrderingByProperty(String selector, GqlJcrNodeCriteriaInput criteria, QueryObjectModelFactory factory) throws RepositoryException {
        GqlOrdering gqlOrdering = criteria.getOrdering();
        Ordering ordering = null;
        if (gqlOrdering != null) {
            switch (gqlOrdering.getOrderType()) {
                case ASC:
                    ordering = factory.ascending(factory.propertyValue(selector, gqlOrdering.getProperty()));
                    break;
                case DESC:
                    ordering = factory.descending(factory.propertyValue(selector, gqlOrdering.getProperty()));
                    break;
            }
        }
        return ordering;
    }

    private GqlJcrNode getGqlNodeByPath(String path) throws RepositoryException {
        return SpecializedTypesHandler.getNode(getSession().getNode(path));
    }

    private GqlJcrNode getGqlNodeById(String uuid) throws RepositoryException {
        return SpecializedTypesHandler.getNode(getSession().getNodeByIdentifier(uuid));
    }

    private JCRSessionWrapper getSession() throws RepositoryException {
        return JCRSessionFactory.getInstance().getCurrentUserSession(workspace.getValue());
    }

    private JCRSessionWrapper getSession(String language) throws RepositoryException {
        if (language == null) {
            return getSession();
        }
        Locale locale = LocaleUtils.toLocale(language);
        return JCRSessionFactory.getInstance().getCurrentUserSession(workspace.getValue(), locale);
    }

    private static void throwNoneOrMultipleNodeConstraintsException() {
        StringBuilder constraintNames = new StringBuilder();
        for (int i = 0; i < NODE_CONSTRAINT_CONVERTORS.length; i++) {
            NodeConstraintConvertor nodeConstraintConvertor = NODE_CONSTRAINT_CONVERTORS[i];
            constraintNames.append("'").append(nodeConstraintConvertor.getFieldName()).append("'");
            if (i < NODE_CONSTRAINT_CONVERTORS.length - 2) {
                constraintNames.append(", ");
            } else if (i == NODE_CONSTRAINT_CONVERTORS.length - 2) {
                constraintNames.append(" or ");
            }
        }
        throw new GqlJcrWrongInputException("Exactly one contraint field expected, either " + constraintNames);
    }

    /**
     * Validate if property field is missing when node constraint perform contains/like/equals/notEquals/lt/lte/gt/gte/exists ... etc
     * other than the composite constraint all/any/none
     *
     * @param nodeConstraint
     */
    private static void validateNodeConstraintProperty(GqlJcrNodeConstraintInput nodeConstraint) {
        if (nodeConstraint.getProperty() == null && nodeConstraint.getContains() == null
            && (nodeConstraint.getFunction() == null || (!nodeConstraint.getFunction().equals(NODE_NAME) && !nodeConstraint.getFunction().equals(NODE_LOCAL_NAME)))) {
            throw new GqlJcrWrongInputException("'property' field is required");
        }
    }

    /**
     * Validate if composite constraint all/any/none is missed with other constraints in the same level
     *
     * @param nodeConstraint
     */
    private static void validateNodeCompositeConstraintConflict(GqlJcrNodeConstraintInput nodeConstraint){
        if ((nodeConstraint.getAll()!=null || nodeConstraint.getAny()!=null || nodeConstraint.getNone()!=null)
                && (nodeConstraint.getContains()!=null || nodeConstraint.getLike()!=null || nodeConstraint.getFunction()!=null
                || nodeConstraint.getExists()!=null || nodeConstraint.getEquals()!=null || nodeConstraint.getGt()!=null
                || nodeConstraint.getGte()!=null || nodeConstraint.getLt()!=null || nodeConstraint.getLte()!=null
                || nodeConstraint.getLastDays()!=null || nodeConstraint.getProperty()!=null || nodeConstraint.getNotEquals()!=null))
            throw new GqlJcrWrongInputException("Composite constraints all/any/none cannot be mixed with other constraints in the same level");
    }

    private interface NodeConstraintConvertor {
        Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, QueryObjectModelFactory factory, String selector) throws RepositoryException;
        String getFieldName();
    }

    private static DynamicOperand applyConstraintFunctions(GqlJcrNodeConstraintInput nodeConstraint, String selector, QueryObjectModelFactory factory)
            throws RepositoryException {

        PropertyValue value = nodeConstraint.getProperty() != null ?  factory.propertyValue(selector, nodeConstraint.getProperty()) : null;
        GqlJcrNodeConstraintInput.QueryFunction function = nodeConstraint.getFunction();
        if (function == null) {
            return value;
        }

        switch (function) {
            case LOWER_CASE:
                return factory.lowerCase(value);
            case UPPER_CASE:
                return factory.upperCase(value);
            case NODE_NAME:
                return factory.nodeName(selector);
            case NODE_LOCAL_NAME:
                return factory.nodeLocalName(selector);
            default:
                return value;
        }
    }

    private static class NodeConstraintConvertorLike implements NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, QueryObjectModelFactory factory, String selector) throws RepositoryException {

            String value = nodeConstraint.getLike();
            if (value == null) {
                return null;
            }

            validateNodeConstraintProperty(nodeConstraint);
            return factory.comparison(applyConstraintFunctions(nodeConstraint, selector, factory),
                                QueryObjectModelConstants.JCR_OPERATOR_LIKE, factory.literal(new ValueImpl(value)));
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.LIKE.getValue();
        }
    }

    private static class NodeConstraintConvertorContains implements NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, QueryObjectModelFactory factory, String selector) throws RepositoryException {

            String value = nodeConstraint.getContains();
            if (value == null) {
                return null;
            }

            return factory.fullTextSearch(selector, nodeConstraint.getProperty(), factory.literal(new ValueImpl(value)));
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.CONTAINS.getValue();
        }
    }

    private static class NodeConstraintConvertorEquals implements NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, QueryObjectModelFactory factory, String selector) throws RepositoryException {

            String value = nodeConstraint.getEquals();
            if (value == null) {
                return null;
            }

            validateNodeConstraintProperty(nodeConstraint);
            return factory.comparison(applyConstraintFunctions(nodeConstraint, selector, factory),
                    QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, factory.literal(new ValueImpl(value)));
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.EQUALS.getValue();
        }
    }

    private static class NodeConstraintConvertorNotEquals implements NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, QueryObjectModelFactory factory, String selector) throws RepositoryException {

            String value = nodeConstraint.getNotEquals();
            if (value == null) {
                return null;
            }

            validateNodeConstraintProperty(nodeConstraint);
            return factory.comparison(applyConstraintFunctions(nodeConstraint, selector, factory),
                    QueryObjectModelConstants.JCR_OPERATOR_NOT_EQUAL_TO, factory.literal(new ValueImpl(value)));
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.NOTEQUALS.getValue();
        }
    }

    private static class NodeConstraintConvertorLessThan implements NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, QueryObjectModelFactory factory, String selector) throws RepositoryException {

            String value = nodeConstraint.getLt();
            if (value == null) {
                return null;
            }

            validateNodeConstraintProperty(nodeConstraint);
            return factory.comparison(factory.propertyValue(selector, nodeConstraint.getProperty()),
                    QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN, factory.literal(new ValueImpl(value)));
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.LT.getValue();
        }
    }

    private static class NodeConstraintConvertorGreaterThan implements NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, QueryObjectModelFactory factory, String selector) throws RepositoryException {

            String value = nodeConstraint.getGt();
            if (value == null) {
                return null;
            }

            validateNodeConstraintProperty(nodeConstraint);
            return factory.comparison(factory.propertyValue(selector, nodeConstraint.getProperty()),
                    QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN, factory.literal(new ValueImpl(value)));
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.GT.getValue();
        }
    }

    private static class NodeConstraintConvertorLessThanOrEqualsTo implements NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, QueryObjectModelFactory factory, String selector) throws RepositoryException {

            String value = nodeConstraint.getLte();
            if (value == null) {
                return null;
            }

            validateNodeConstraintProperty(nodeConstraint);
            return factory.comparison(factory.propertyValue(selector, nodeConstraint.getProperty()),
                    QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO, factory.literal(new ValueImpl(value)));
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.LTE.getValue();
        }
    }

    private static class NodeConstraintConvertorGreaterThanOrEqualsTo implements NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, QueryObjectModelFactory factory, String selector) throws RepositoryException {

            String value = nodeConstraint.getGte();
            if (value == null) {
                return null;
            }

            validateNodeConstraintProperty(nodeConstraint);
            return factory.comparison(factory.propertyValue(selector, nodeConstraint.getProperty()),
                    QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO, factory.literal(new ValueImpl(value)));
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.GTE.getValue();
        }
    }

    private static class NodeConstraintConvertorExists implements NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, QueryObjectModelFactory factory, String selector) throws RepositoryException {

            Boolean value = nodeConstraint.getExists();
            if (value == null) {
                return null;
            }

            Constraint constraint = factory.propertyExistence(selector, nodeConstraint.getProperty());
            return value ? constraint : factory.not(constraint);
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.EXISTS.getValue();
        }
    }

    private static class NodeConstraintConvertorLastDays implements NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, QueryObjectModelFactory factory, String selector) throws RepositoryException {

            Integer value = nodeConstraint.getLastDays();
            if (value == null) {
                return null;
            }

            validateNodeConstraintProperty(nodeConstraint);
            Date targetDate = DateUtils.addDays(new Date(), - value);
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSXXX");

            return factory.comparison(factory.propertyValue(selector, nodeConstraint.getProperty()),
                                        QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO,
                                        factory.literal(new ValueImpl(dateFormat.format(targetDate))));
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.LASTDAYS.getValue();
        }
    }

    public static class QueryLanguageDefaultValue implements Supplier<Object> {

        @Override
        public GqlJcrQuery.QueryLanguage get() {
            return SQL2;
        }
    }
}
