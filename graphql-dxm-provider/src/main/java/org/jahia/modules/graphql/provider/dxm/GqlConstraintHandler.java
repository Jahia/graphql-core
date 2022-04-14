/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.graphql.provider.dxm;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.ISO8601;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeConstraintInput;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeCriteriaInput;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.modules.graphql.provider.dxm.node.GqlOrdering;
import org.jahia.services.content.nodetypes.ValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.touk.throwing.ThrowingBiFunction;
import pl.touk.throwing.ThrowingFunction;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.query.qom.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeConstraintInput.QueryFunction.NODE_LOCAL_NAME;
import static org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeConstraintInput.QueryFunction.NODE_NAME;

public class GqlConstraintHandler {
    private static final Logger logger = LoggerFactory.getLogger(GqlConstraintHandler.class);

    public static final Pattern LONG_PATTERN = Pattern.compile("[+-]?\\d+");
    public static final Pattern DOUBLE_PATTERN = Pattern.compile("^[+-]?\\d+\\.?\\d+?$");
    
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

    private QueryObjectModelFactory qomFactory;
    private ValueFactory valueFactory;

    public GqlConstraintHandler(QueryObjectModelFactory qomFactory, ValueFactory valueFactory) {
        this.qomFactory = qomFactory;
        this.valueFactory = valueFactory;
    }

    public QueryObjectModelFactory getQomFactory() {
        return qomFactory;
    }

    public ValueFactory getValueFactory() {
        return valueFactory;
    }

    /**
     * Generate constraints by criteria input, the principle constraints for nodeType, paths and pathType are unique
     * In the nodeConstraint, it supports single constraint as well as complex constraints with multiple level child constraints
     * which are composite by all/any/none
     *
     * @param selector
     * @param criteria
     * @return
     * @throws RepositoryException
     */
    public Constraint getConstraintTree(final String selector, final GqlJcrNodeCriteriaInput criteria) throws RepositoryException {

        final LinkedHashSet<Constraint> constraints = new LinkedHashSet<>();

        // Add path constraint if any.
        Collection<String> paths = criteria.getPaths();
        if (paths != null && !paths.isEmpty()) {
            Function<String, Constraint> constraintFunction = getPathConstraintFunction(selector, criteria.getPathType());
            constraints.add(paths.stream().map(constraintFunction).reduce(ThrowingBiFunction.unchecked(qomFactory::or)::apply).orElse(null));
        }

        if (criteria.getNodeConstraint() != null) {
            constraints.add(compositeChildConstraints(selector, criteria.getNodeConstraint()));
        }

        // Build the result.
        return constraints.stream().reduce(ThrowingBiFunction.unchecked(qomFactory::and)::apply).orElse(null);
    }

    private Function<String, Constraint> getPathConstraintFunction(String selector, GqlJcrNodeCriteriaInput.PathType pathType) {
        Function<String, Constraint> function = ThrowingFunction.unchecked(p -> qomFactory.descendantNode(selector, p));
        if (pathType == GqlJcrNodeCriteriaInput.PathType.PARENT) {
            function = ThrowingFunction.unchecked(p -> qomFactory.childNode(selector, p));
        } else if (pathType == GqlJcrNodeCriteriaInput.PathType.OWN) {
            function = ThrowingFunction.unchecked(p -> qomFactory.sameNode(selector, p));
        }
        return function;
    }

    /**
     * Support all/any/none composite constraints recursively
     * Only one type of composite constraint is allowed on each level
     * And if child has simple constraint then not allow any composite constraint
     *
     * @param selector
     * @param constraintInput
     * @return
     * @throws RepositoryException
     */
    private Constraint compositeChildConstraints(final String selector, final GqlJcrNodeConstraintInput constraintInput) throws RepositoryException {
        validateNodeCompositeConstraintConflict(constraintInput);

        final List<GqlJcrNodeConstraintInput> allConstraintInputs = constraintInput.getAll();
        final List<GqlJcrNodeConstraintInput> anyConstraintInputs = constraintInput.getAny();
        final List<GqlJcrNodeConstraintInput> noneConstraintInputs = constraintInput.getNone();
        Function<GqlJcrNodeConstraintInput, Constraint> convert = ThrowingFunction.unchecked((c) -> compositeChildConstraints(selector, c));

        if (allConstraintInputs == null && anyConstraintInputs == null && noneConstraintInputs == null) {
            return convertToConstraint(selector, constraintInput);
        } else if (allConstraintInputs != null && anyConstraintInputs == null && noneConstraintInputs == null) {
            return allConstraintInputs.stream().map(convert).reduce(ThrowingBiFunction.unchecked(qomFactory::and)::apply).orElse(null);
        } else if (allConstraintInputs == null && anyConstraintInputs != null && noneConstraintInputs == null) {
            return anyConstraintInputs.stream().map(convert).reduce(ThrowingBiFunction.unchecked(qomFactory::or)::apply).orElse(null);
        } else if (allConstraintInputs == null && anyConstraintInputs == null && noneConstraintInputs != null) {
            return noneConstraintInputs.stream().map(convert).map(ThrowingFunction.<Constraint, Constraint, RepositoryException>unchecked(qomFactory::not)).reduce(ThrowingBiFunction.unchecked(qomFactory::and)::apply).orElse(null);
        } else {
            throw new GqlJcrWrongInputException("Composite constraints all/any/none cannot be mixed with other constraints in the same level");
        }
    }

    /**
     * Validate if composite constraint all/any/none is mixed with other constraints in the same level
     *
     * @param nodeConstraint
     */
    private void validateNodeCompositeConstraintConflict(GqlJcrNodeConstraintInput nodeConstraint) {
        if ((nodeConstraint.getAll() != null || nodeConstraint.getAny() != null || nodeConstraint.getNone() != null)
                && (nodeConstraint.getContains() != null || nodeConstraint.getLike() != null || nodeConstraint.getFunction() != null
                || nodeConstraint.getExists() != null || nodeConstraint.getEquals() != null || nodeConstraint.getGt() != null
                || nodeConstraint.getGte() != null || nodeConstraint.getLt() != null || nodeConstraint.getLte() != null
                || nodeConstraint.getLastDays() != null || nodeConstraint.getProperty() != null || nodeConstraint.getNotEquals() != null))
            throw new GqlJcrWrongInputException("Composite constraints all/any/none cannot be mixed with other constraints in the same level");
    }

    private Constraint convertToConstraint(final String selector,
                                                  final GqlJcrNodeConstraintInput nodeConstraintInput) throws RepositoryException {
        return Arrays.stream(NODE_CONSTRAINT_CONVERTORS)
                .map(ThrowingFunction.unchecked(nodeConstraintConvertor -> nodeConstraintConvertor.convert(nodeConstraintInput, selector, this)))
                .filter(Objects::nonNull)
                .reduce(ThrowingBiFunction.unchecked(qomFactory::and)::apply)
                .orElseThrow(() -> new GqlJcrWrongInputException("At least one of the following constraint field is expected: " + StringUtils.join(Arrays.stream(NODE_CONSTRAINT_CONVERTORS).map(NodeConstraintConvertor::getFieldName).collect(Collectors.toSet()),',')));
    }

    public Ordering getOrderingByProperty(String selector, GqlJcrNodeCriteriaInput criteria) throws RepositoryException {
        GqlOrdering gqlOrdering = criteria.getOrdering();
        Ordering ordering = null;
        if (gqlOrdering != null) {
            switch (gqlOrdering.getOrderType()) {
                case ASC:
                    ordering = qomFactory.ascending(qomFactory.propertyValue(selector, gqlOrdering.getProperty()));
                    break;
                case DESC:
                    ordering = qomFactory.descending(qomFactory.propertyValue(selector, gqlOrdering.getProperty()));
                    break;
            }
        }
        return ordering;
    }

    private DynamicOperand applyConstraintFunctions(GqlJcrNodeConstraintInput nodeConstraint, String selector)
            throws RepositoryException {

        PropertyValue value = nodeConstraint.getProperty() != null ? qomFactory.propertyValue(selector, nodeConstraint.getProperty()) : null;
        GqlJcrNodeConstraintInput.QueryFunction function = nodeConstraint.getFunction();
        if (function == null) {
            return value;
        }

        switch (function) {
            case LOWER_CASE:
                return qomFactory.lowerCase(value);
            case UPPER_CASE:
                return qomFactory.upperCase(value);
            case NODE_NAME:
                return qomFactory.nodeName(selector);
            case NODE_LOCAL_NAME:
                return qomFactory.nodeLocalName(selector);
            default:
                return value;
        }
    }

    private Value convertValueByParsingSuccess(String value) {
        if (LONG_PATTERN.matcher(value).matches()) {
            return valueFactory.createValue(Long.parseLong(value));
        }

        if (DOUBLE_PATTERN.matcher(value).matches()) {
            return valueFactory.createValue(Double.parseDouble(value));
        }
        try {
            Calendar c = ISO8601.parse(value);
            if (c != null) {
                return valueFactory.createValue(c);
            }
        } catch (IllegalArgumentException e) {
            logger.debug("Cannot parse date", e);
        }

        throw new DataFetchingException("Invalid value " + value);
    }

    private abstract static class NodeConstraintConvertor {
        abstract Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, String selector, GqlConstraintHandler handler) throws RepositoryException;

        abstract String getFieldName();

        protected void validateNodeConstraintProperty(GqlJcrNodeConstraintInput nodeConstraint) {
            if (nodeConstraint.getProperty() == null
                    && (nodeConstraint.getFunction() == null || (!nodeConstraint.getFunction().equals(NODE_NAME) && !nodeConstraint.getFunction().equals(NODE_LOCAL_NAME)))) {
                throw new GqlJcrWrongInputException("'property' field is required");
            }
        }

    }

    private static class NodeConstraintConvertorLike extends NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, String selector, GqlConstraintHandler handler) throws RepositoryException {

            String value = nodeConstraint.getLike();
            if (value == null) {
                return null;
            }

            validateNodeConstraintProperty(nodeConstraint);
            return handler.getQomFactory().comparison(handler.applyConstraintFunctions(nodeConstraint, selector),
                    QueryObjectModelConstants.JCR_OPERATOR_LIKE, handler.getQomFactory().literal(new ValueImpl(value)));
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.LIKE.getValue();
        }
    }

    private static class NodeConstraintConvertorContains extends NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, String selector, GqlConstraintHandler handler) throws RepositoryException {

            String value = nodeConstraint.getContains();
            if (value == null) {
                return null;
            }

            return handler.getQomFactory().fullTextSearch(selector, nodeConstraint.getProperty(), handler.getQomFactory().literal(new ValueImpl(value)));
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.CONTAINS.getValue();
        }
    }

    private static class NodeConstraintConvertorEquals extends NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, String selector, GqlConstraintHandler handler) throws RepositoryException {

            String value = nodeConstraint.getEquals();
            if (value == null) {
                return null;
            }

            validateNodeConstraintProperty(nodeConstraint);
            return handler.getQomFactory().comparison(handler.applyConstraintFunctions(nodeConstraint, selector),
                    QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, handler.getQomFactory().literal(new ValueImpl(value)));
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.EQUALS.getValue();
        }
    }

    private static class NodeConstraintConvertorNotEquals extends NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, String selector, GqlConstraintHandler handler) throws RepositoryException {

            String value = nodeConstraint.getNotEquals();
            if (value == null) {
                return null;
            }

            validateNodeConstraintProperty(nodeConstraint);
            return handler.getQomFactory().comparison(handler.applyConstraintFunctions(nodeConstraint, selector),
                    QueryObjectModelConstants.JCR_OPERATOR_NOT_EQUAL_TO, handler.getQomFactory().literal(new ValueImpl(value)));
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.NOTEQUALS.getValue();
        }
    }

    private static class NodeConstraintConvertorLessThan extends NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, String selector, GqlConstraintHandler handler) throws RepositoryException {

            String value = nodeConstraint.getLt();
            if (value == null) {
                return null;
            }

            validateNodeConstraintProperty(nodeConstraint);
            return handler.getQomFactory().comparison(handler.applyConstraintFunctions(nodeConstraint, selector),
                    QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN, handler.getQomFactory().literal(handler.convertValueByParsingSuccess(value)));
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.LT.getValue();
        }
    }

    private static class NodeConstraintConvertorGreaterThan extends NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, String selector, GqlConstraintHandler handler) throws RepositoryException {

            String value = nodeConstraint.getGt();
            if (value == null) {
                return null;
            }

            validateNodeConstraintProperty(nodeConstraint);
            return handler.getQomFactory().comparison(handler.applyConstraintFunctions(nodeConstraint, selector),
                    QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN, handler.getQomFactory().literal(handler.convertValueByParsingSuccess(value)));
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.GT.getValue();
        }
    }

    private static class NodeConstraintConvertorLessThanOrEqualsTo extends NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, String selector, GqlConstraintHandler handler) throws RepositoryException {

            String value = nodeConstraint.getLte();
            if (value == null) {
                return null;
            }

            validateNodeConstraintProperty(nodeConstraint);
            return handler.getQomFactory().comparison(handler.applyConstraintFunctions(nodeConstraint, selector),
                    QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO, handler.getQomFactory().literal(handler.convertValueByParsingSuccess(value)));
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.LTE.getValue();
        }
    }

    private static class NodeConstraintConvertorGreaterThanOrEqualsTo extends NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, String selector, GqlConstraintHandler handler) throws RepositoryException {

            String value = nodeConstraint.getGte();
            if (value == null) {
                return null;
            }

            validateNodeConstraintProperty(nodeConstraint);
            return handler.getQomFactory().comparison(handler.applyConstraintFunctions(nodeConstraint, selector),
                    QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO, handler.getQomFactory().literal(handler.convertValueByParsingSuccess(value)));
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.GTE.getValue();
        }
    }

    private static class NodeConstraintConvertorExists extends NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, String selector, GqlConstraintHandler handler) throws RepositoryException {

            Boolean value = nodeConstraint.getExists();
            if (value == null) {
                return null;
            }
            if (nodeConstraint.getProperty() == null) {
                throw new GqlJcrWrongInputException("'property' field is required");
            }
            Constraint constraint = handler.getQomFactory().propertyExistence(selector, nodeConstraint.getProperty());
            return value ? constraint : handler.getQomFactory().not(constraint);
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.EXISTS.getValue();
        }
    }

    private static class NodeConstraintConvertorLastDays extends NodeConstraintConvertor {

        @Override
        public Constraint convert(GqlJcrNodeConstraintInput nodeConstraint, String selector, GqlConstraintHandler handler) throws RepositoryException {

            Integer value = nodeConstraint.getLastDays();
            if (value == null) {
                return null;
            }

            //Constraint validation
            validateNodeConstraintProperty(nodeConstraint);

            if (nodeConstraint.getLastDays() != null && nodeConstraint.getLastDays() < 0) {
                throw new GqlJcrWrongInputException("lastDays value should not be negative");
            }

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -value);

            return handler.getQomFactory().comparison(handler.getQomFactory().propertyValue(selector, nodeConstraint.getProperty()),
                    QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO,
                    handler.getQomFactory().literal(new ValueImpl(cal)));
        }

        @Override
        public String getFieldName() {
            return GqlJcrNodeConstraintInput.FieldNames.LASTDAYS.getValue();
        }
    }
}
