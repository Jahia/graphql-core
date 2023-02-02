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
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.apache.commons.lang3.stream.Streams;
import org.apache.jackrabbit.util.ISO8601;
import org.jahia.bin.Jahia;
import org.jahia.modules.graphql.provider.dxm.acl.GqlAclRole;
import org.jahia.modules.graphql.provider.dxm.acl.service.JahiaAclService;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;
import org.jahia.settings.SettingsBean;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GraphQL root object for Admin related queries.
 */
@GraphQLName("AdminQuery")
@GraphQLDescription("Admin queries root")
public class GqlAdminQuery {

    @Inject
    @GraphQLOsgiService
    private JahiaAclService aclService;


    /**
     * Get Jahia admin query
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Get Jahia admin query")
    @GraphQLRequiresPermission(value = "graphqlAdminQuery")
    public GqlJahiaAdminQuery getJahia() {
        return new GqlJahiaAdminQuery();
    }

    /**
     * @deprecated replaced by jahia node
     */
    @Deprecated
    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("Version of the running Jahia instance")
    public String getVersion() {
        return Jahia.getFullProductVersion();
    }

    /**
     * Get Build Datetime
     *
     * @return String datetime in ISO8601 format
     */
    @GraphQLField
    @GraphQLDescription("Current datetime")
    public String getDatetime() {
        return ISO8601.format(Calendar.getInstance());
    }

    @GraphQLField
    @GraphQLDescription("Get available ACL roles")
    public List<GqlAclRole> getRoles() throws RepositoryException {
        return Streams.stream(aclService.getRoles())
                .map(GqlAclRole::new)
                .collect(Collectors.toList());
    }

    /**
     * Get getCluster
     *
     * @return GqlCluster
     */
    @GraphQLField
    @GraphQLDescription("Details about the Jahia cluster")
    public GqlCluster getCluster() {

        GqlCluster gqlCluster = new GqlCluster();
        gqlCluster.setIsActivated(SettingsBean.getInstance().isClusterActivated());
        return gqlCluster;
    }
}
