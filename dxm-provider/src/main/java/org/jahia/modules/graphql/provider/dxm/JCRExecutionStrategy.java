package org.jahia.modules.graphql.provider.dxm;

import graphql.ExecutionResult;
import graphql.annotations.EnhancedExecutionStrategy;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionParameters;
import graphql.language.Field;
import org.jahia.modules.graphql.provider.dxm.node.NodeMutations;

import javax.jcr.RepositoryException;
import java.util.List;

public class JCRExecutionStrategy extends EnhancedExecutionStrategy {
    @Override
    protected ExecutionResult completeValue(ExecutionContext executionContext, ExecutionParameters parameters, List<Field> fields) {
        ExecutionResult executionResult = super.completeValue(executionContext, parameters, fields);
        if (parameters.source() instanceof NodeMutations.GraphQLMutationJCR) {
            try {
                ((NodeMutations.GraphQLMutationJCR)parameters.source()).session.save();
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        }
        return executionResult;
    }
}
