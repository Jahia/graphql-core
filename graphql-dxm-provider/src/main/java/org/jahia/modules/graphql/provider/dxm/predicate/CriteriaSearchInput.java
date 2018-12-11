/*
 *  ==========================================================================================
 *  =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 *  ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 *      Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *      THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *      1/GPL OR 2/JSEL
 *
 *      1/ GPL
 *      ==================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as lastPublished by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *      2/ JSEL - Commercial and Supported Versions of the program
 *      ===================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      Alternatively, commercial and supported versions of the program - also known as
 *      Enterprise Distributions - must be used in accordance with the terms and conditions
 *      contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *      If you are unsure which license is appropriate for your use,
 *      please contact the sales department at sales@jahia.com.
 *
 */
package org.jahia.modules.graphql.provider.dxm.predicate;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.List;

@GraphQLDescription("Input object representing terms text criteria and either a date criteria of node creation / modification / publish")
public class CriteriaSearchInput {

    private List<TermSearchCriteriaInput> text;

    private DateSearchCriteriaInput created;

    private String createdBy;

    private DateSearchCriteriaInput lastModified;

    private String lastModifiedBy;

    private DateSearchCriteriaInput lastPublished;

    private String lastPublishedBy;

    private String basePath;

    private List<String> sites;

    private List<String> nodeTypes;

    private String language;


    public CriteriaSearchInput(@GraphQLName("text") List<TermSearchCriteriaInput> text,
                               @GraphQLName("created") DateSearchCriteriaInput created,
                               @GraphQLName("createdBy") String createdBy,
                               @GraphQLName("lastModified") DateSearchCriteriaInput lastModified,
                               @GraphQLName("lastModifiedBy") String lastModifiedBy,
                               @GraphQLName("lastPublished") DateSearchCriteriaInput lastPublished,
                               @GraphQLName("lastPublishedBy") String lastPublishedBy,
                               @GraphQLName("basePath") String basePath,
                               @GraphQLName("sites") List<String> sites,
                               @GraphQLName("nodeTypes") List<String> nodeTypes,
                               @GraphQLName("language") String language){
        this.text = text;
        this.created = created;
        this.createdBy = createdBy;
        this.lastModified = lastModified;
        this.lastModifiedBy = lastModifiedBy;
        this.lastPublished = lastPublished;
        this.lastPublishedBy = lastPublishedBy;
        this.basePath = basePath;
        this.sites = sites;
        this.nodeTypes = nodeTypes;
        this.language = language;
    }

    @GraphQLField
    @GraphQLDescription("basic jcr node path ")
    public String getBasePath() {
        return basePath;
    }

    @GraphQLField
    @GraphQLDescription("site name list")
    public List<String> getSites() {
        return sites;
    }

    @GraphQLField
    @GraphQLDescription("jcr node type")
    public List<String> getNodeTypes() {
        return nodeTypes;
    }

    @GraphQLField
    @GraphQLDescription("site language")
    public String getLanguage() {
        return language;
    }

    @GraphQLField
    @GraphQLDescription("date criteria for creation")
    public DateSearchCriteriaInput getCreated() {
        return created;
    }

    @GraphQLField
    @GraphQLDescription("date criteria for last modification")
    public DateSearchCriteriaInput getLastModified() {
        return lastModified;
    }

    @GraphQLField
    @GraphQLDescription("date criteria for publication")
    public DateSearchCriteriaInput getLastPublished() {
        return lastPublished;
    }

    @GraphQLField
    @GraphQLDescription("term search criteria")
    public List<TermSearchCriteriaInput> getText() {
        return text;
    }

    @GraphQLField
    @GraphQLDescription("the user who create it")
    public String getCreatedBy() {
        return createdBy;
    }

    @GraphQLField
    @GraphQLDescription("the user who modify it")
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    @GraphQLField
    @GraphQLDescription("the user who publish it")
    public String getLastPublishedBy() {
        return lastPublishedBy;
    }
}
