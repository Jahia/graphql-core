/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2021 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.graphql.provider.dxm.admin;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

public class GqlJahiaVersion {

    private String release;
    private String build;
    private boolean snapshot;

    public GqlJahiaVersion() {
    }

    public GqlJahiaVersion(String release, String build, boolean snapshot) {
        this.release = release;
        this.build = build;
        this.snapshot = snapshot;
    }

    @GraphQLField
    @GraphQLName("release")
    public String getRelease() {
        return release;
    }

    @GraphQLField
    @GraphQLName("build")
    public String getBuild() {
        return build;
    }

    @GraphQLField
    @GraphQLName("isSnapshot")
    public boolean isSnapshot() {
        return snapshot;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public void setBuild(String build) {
        this.build = build;
    }

    public void setSnapshot(boolean snapshot) {
        this.snapshot = snapshot;
    }
}


