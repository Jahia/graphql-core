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
package org.jahia.modules.graphql.provider.dxm.admin;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName("JahiaVersion")
@GraphQLDescription("Version of the running Jahia instance")
public class GqlJahiaVersion {

    private String release;
    private String build;
    private String buildDate;
    private boolean snapshot;

    public GqlJahiaVersion() {
    }

    @GraphQLField
    @GraphQLName("release")
    @GraphQLDescription("Release of the running Jahia instance")
    public String getRelease() {
        return release;
    }

    @GraphQLField
    @GraphQLName("build")
    @GraphQLDescription("Build number of the running Jahia instance")
    public String getBuild() {
        return build;
    }

    @GraphQLField
    @GraphQLName("buildDate")
    @GraphQLDescription("Build date of the running Jahia instance")
    public String getBuildDate() {
        return buildDate;
    }

    @GraphQLField
    @GraphQLName("isSnapshot")
    @GraphQLDescription("Flag returning if running Jahia instance is a SNAPSHOT")
    public boolean isSnapshot() {
        return snapshot;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public void setBuild(String build) {
        this.build = build;
    }

    public void setBuildDate(String buildDate) {
        this.buildDate = buildDate;
    }

    public void setSnapshot(boolean snapshot) {
        this.snapshot = snapshot;
    }
}


