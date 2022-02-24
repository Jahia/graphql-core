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
package org.jahia.modules.graphql.provider.dxm.sdl.validation;

import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldSorterInput;
import org.jahia.modules.graphql.provider.dxm.predicate.SorterHelper;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLUtil;

import java.util.Map;

/**
 * Created at 12 Feb, 2019$
 *
 * @author chooliyip
 **/
public class ArgumentValidator {

    public enum ArgumentNames {

        VALUE("value"),
        DATE_RANGE("dateRange"), AFTER("after"), BEFORE("before"), LASTDAYS("lastDays"),
        SORT_BY("sortBy"), FIELD_NAME("fieldName"), SORT_TYPE("sortType"), IGNORE_CASE("ignoreCase");

        private String name;

        ArgumentNames(String name) {
            this.name = name;
        }

        public String getName(){ return name; }
    }

    public static boolean validate(ArgumentNames argumentNames, DataFetchingEnvironment environment) {
        switch (argumentNames) {
            case VALUE:
                return validateArgumentsExistence(ArgumentNames.VALUE.getName(), environment);

            case DATE_RANGE:
                return validateDateRangeArguments(environment);

            case SORT_BY:
                return validateSortByArguments(environment);

            default: return false;
        }
    }

    private static boolean validateArgumentsExistence(String argumentName, DataFetchingEnvironment environment) {
        if (SDLUtil.getArgument(argumentName, environment) == null) {
            throw new DataFetchingException(" Argument '" + argumentName + "' is missing");
        } else {
            return true;
        }
    }

    private static boolean validateSortByArguments(DataFetchingEnvironment environment) {
        FieldSorterInput sorterInput = getFieldSorterInput(environment);
        if (sorterInput==null) { return true; }
        if (!SDLUtil.isFieldInWrappedTypeFields(sorterInput.getFieldName(), environment)){
            throw new DataFetchingException("Cannot sort by invalid field name '" + sorterInput.getFieldName() + "'");
        } else {
            return true;
        }
    }

    private static boolean validateDateRangeArguments(DataFetchingEnvironment environment) {
        String after = (String) SDLUtil.getArgument(ArgumentNames.AFTER.getName(), environment);
        String before = (String) SDLUtil.getArgument(ArgumentNames.BEFORE.getName(), environment);
        Integer lastDays = (Integer) SDLUtil.getArgument(ArgumentNames.LASTDAYS.getName(), environment);

        if (SDLUtil.getArgumentsSize(environment) == 0
                || (lastDays == null && StringUtils.isBlank(after) && StringUtils.isBlank(before))) {
            throw new DataFetchingException("By date range data fetcher needs at least one argument of 'after', 'before' or 'lastDays'");
        } else if (lastDays!=null && (!StringUtils.isBlank(after) || !StringUtils.isBlank(before))) {
            throw new DataFetchingException("By date range data fetcher does not support argument 'lastDays' mixed with 'after' or 'before'");
        } else {
            return true;
        }
    }

    public static FieldSorterInput getFieldSorterInput(DataFetchingEnvironment environment) {
        Map sortByFilter = (Map) SDLUtil.getArgument(ArgumentNames.SORT_BY.getName(), environment);
        return sortByFilter != null ? new FieldSorterInput((String) sortByFilter.get(ArgumentNames.FIELD_NAME.getName()),
                (SorterHelper.SortType) sortByFilter.get(ArgumentNames.SORT_TYPE.getName()),
                (Boolean) sortByFilter.get(ArgumentNames.IGNORE_CASE.getName())) : null;
    }

}
