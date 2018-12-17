package org.jahia.modules.graphql.provider.dxm.util;

import org.jahia.services.content.nodetypes.ValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.query.qom.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class DefaultConstraintHelper {

    public enum ConstraintOperator {
        LIKE,
        CONTAINS,
        EQUAL_TO
    }

    public enum ConstraintType {
        AND,
        OR
    }

    public enum ConstraintFunction {
        LOWERCASE,
        NONE
    }

    private final static Logger logger = LoggerFactory.getLogger(DefaultConstraintHelper.class);

    private final QueryObjectModelFactory factory;
    private final String selector;
    //j:nodename must be first property to be processed
    private final LinkedList<String> defaultProperties = new LinkedList<>(Arrays.asList("j:nodename", "jcr:title", "j:keywords", "j:tagList"));
    private Constraint result = null;
    private LinkedList<Constraint> buffer = new LinkedList<>();

    public DefaultConstraintHelper(QueryObjectModelFactory factory, String selector) {
        this.factory = factory;
        this.selector = selector;
    }

    public Constraint buildDefaultPropertiesConstraint(String searchCriteria) {
        defaultProperties.forEach (property -> {
            switch (property) {
                case "j:nodename":
                    String keywordsLowercase = searchCriteria.toLowerCase();
                    List<String> keywords = new LinkedList<>(Arrays.asList(keywordsLowercase.split(" ")));
                    //Order matters here as there is no current functionality to support nested statements
                    keywords.forEach(keyword -> buildConstraintOperator(ConstraintOperator.LIKE, ConstraintFunction.LOWERCASE, property, "%" + keyword + "%"));
                    buildConstraintType(ConstraintType.AND);
                    //CONTAINS constraint has to happen after all AND constraints have been processed
                    buildConstraintOperator(ConstraintOperator.CONTAINS, ConstraintFunction.NONE, property, searchCriteria);
                    buildConstraintType(ConstraintType.OR);
                    return;
                case "j:tagList":
                    String tagLowercase = searchCriteria.toLowerCase();
                    List<String> tags = new LinkedList<>(Arrays.asList(tagLowercase.split(" ")));
                    tags.forEach(tag -> {
                        buildConstraintOperator(ConstraintOperator.EQUAL_TO, ConstraintFunction.NONE, property, tag);
                    });
                    buildConstraintType(ConstraintType.OR);
                    return;
                default:
                    buildConstraintOperator(ConstraintOperator.CONTAINS, ConstraintFunction.NONE, property, searchCriteria);
                    buildConstraintType(ConstraintType.OR);
            }
        });
        return result;
    }

    private void buildConstraintOperator(ConstraintOperator operator, ConstraintFunction func, String property, String searchCriteria) {
        try {
            switch (operator) {
                case LIKE:
                    buffer.add(factory.comparison(resolveConstraintFunction(func, property), QueryObjectModelConstants.JCR_OPERATOR_LIKE, factory.literal(new ValueImpl(searchCriteria))));
                    return;
                case CONTAINS:
                    buffer.add(factory.fullTextSearch(selector, property, factory.literal(new ValueImpl(searchCriteria))));
                    return;
                case EQUAL_TO:
                    buffer.add(factory.comparison(resolveConstraintFunction(func, property), QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, factory.literal(new ValueImpl(searchCriteria))));
                    return;
                default:
            }
        } catch(RepositoryException ex) {
            logger.error("Failed to build constraint operator.", ex);
        }
    }

    //@TODO implement nested constraints functionality if there is ever a need for it.
    private void buildConstraintType(ConstraintType type) {
        try {
            switch (type) {
                case OR:
                    while(buffer.size() > 0) {
                        result = result != null ? factory.or(result, buffer.removeFirst()) : buffer.removeFirst();
                    }
                    return;
                case AND:
                    while(buffer.size() > 0) {
                        result = result != null ? factory.and(result, buffer.removeFirst()) : buffer.removeFirst();
                    }
                    return;
                default:
            }
        } catch(RepositoryException ex) {
            logger.error("Failed to build constraint type.", ex);
        }
    }
    private DynamicOperand resolveConstraintFunction(ConstraintFunction func, String property) throws RepositoryException {
        PropertyValue propertyValue = factory.propertyValue(selector, property);
        switch(func) {
            case LOWERCASE:
                return factory.lowerCase(propertyValue);
            case NONE:
            default:
                return propertyValue;
        }
    }
}