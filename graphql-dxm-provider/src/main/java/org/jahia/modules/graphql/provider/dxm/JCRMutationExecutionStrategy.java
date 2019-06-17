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
package org.jahia.modules.graphql.provider.dxm;

import graphql.execution.*;
import graphql.language.Field;
import graphql.language.SourceLocation;
import org.jahia.settings.SettingsBean;
import org.jahia.settings.readonlymode.ReadOnlyModeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Extends some aspects of the standard strategy.
 */
public class JCRMutationExecutionStrategy extends AsyncSerialExecutionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(JCRMutationExecutionStrategy.class);

    public JCRMutationExecutionStrategy(DataFetcherExceptionHandler exceptionHandler) {
        super(exceptionHandler);
    }

    /**
     * Extend the standard behavior to complete any GqlJcrMutation field via persisting any changes made to JCR during its execution.
     */
    @Override
    protected FieldValueInfo completeField(ExecutionContext executionContext, ExecutionStrategyParameters parameters, Object fetchedValue) {
        FieldValueInfo result = null;
        if(SettingsBean.getInstance().isFullReadOnlyMode()) {
            String message = "operation is not permitted for the current session as it is in read-only mode";
            logger.warn(message);
            List<SourceLocation> locations = parameters.getField().stream().map(Field::getSourceLocation).collect(Collectors.toList());
            executionContext.addError(new DXGraphQLError(new GqlReadOnlyModeException(message), parameters.getPath().toList(), locations));
            result = super.completeField(executionContext, parameters, null);
        } else {
            result = super.completeField(executionContext, parameters, fetchedValue);
            if (fetchedValue instanceof DXGraphQLFieldCompleter && executionContext.getErrors().isEmpty()) {
                // we only complete field if there were no errors on execution
                ((DXGraphQLFieldCompleter) fetchedValue).completeField();
            }
        }

        return result;
    }
}
