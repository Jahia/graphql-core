package org.jahia.modules.graphql.provider.dxm.node;

import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.GqlConstraintViolationException;
import org.jahia.services.content.CompositeConstraintViolationException;
import org.jahia.services.content.NodeConstraintViolationException;
import org.jahia.services.content.PropertyConstraintViolationException;

import javax.jcr.nodetype.ConstraintViolationException;
import java.util.*;

/**
 * Utility class to handle constraint violation error during mutation, in case an Exception happen during mutation.
 * You can call the transformException to build automatically the good exception
 *
 * In case of ConstraintViolation exceptions, it will build automatically the GqlConstraintViolationException and will contains the
 * constraint violations data information as extensions.
 */
public class NodeMutationConstraintViolationHandler {

    public static BaseGqlClientException transformException(Throwable e) {
        if (e instanceof NodeConstraintViolationException) {
            return new GqlConstraintViolationException(e, buildExtensions(Collections.singletonList(extractConstraintViolationError((NodeConstraintViolationException) e))));
        }
        if (e instanceof CompositeConstraintViolationException) {
            return new GqlConstraintViolationException(e, buildExtensions(extractConstraintViolationErrors((CompositeConstraintViolationException) e)));
        }
        return new DataFetchingException(e);
    }

    private static Map<String, Object> buildExtensions(List<Map<String, Object>> constraintViolations) {
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("constraintViolations", constraintViolations);
        return extensions;
    }

    private static Map<String, Object> extractConstraintViolationError(NodeConstraintViolationException violationException) {
        Map<String, Object> constraintViolation = new HashMap<>();
        if (violationException instanceof PropertyConstraintViolationException) {
            constraintViolation.put("propertyName", ((PropertyConstraintViolationException) violationException).getDefinition().getName());
        }
        constraintViolation.put("nodePath", violationException.getPath());
        constraintViolation.put("constraintMessage", violationException.getConstraintMessage());
        constraintViolation.put("locale", violationException.getLocale() != null ? violationException.getLocale().toString() : null);
        return constraintViolation;
    }

    private static List<Map<String, Object>> extractConstraintViolationErrors(CompositeConstraintViolationException violationExceptions) {
        List<Map<String, Object>> constraintViolations = new ArrayList<>();
        for (ConstraintViolationException violationException : violationExceptions.getErrors()) {
            if (violationException instanceof NodeConstraintViolationException) {
                constraintViolations.add(extractConstraintViolationError((NodeConstraintViolationException) violationException));
            }
        }
        return constraintViolations;
    }
}
