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
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName("JCRPropertyOption")
public enum GqlJcrPropertyOption {
    ENCRYPTED,

    /**
     * @deprecated pass an offset-bearing ISO-8601 value with no option instead
     */
    // graphql-java-annotations' EnumBuilder reads only @GraphQLName and @GraphQLDescription on a constant, so
    // @GraphQLDeprecate would never reach the schema: the notice has to travel in the description.
    @Deprecated
    @GraphQLDescription("Deprecated: set the property with an ISO-8601 value carrying its time zone offset and no option instead. "
            + "This option reads the value in the server default time zone, so the instant it stores depends on the server.")
    NOT_ZONED_DATE
}
