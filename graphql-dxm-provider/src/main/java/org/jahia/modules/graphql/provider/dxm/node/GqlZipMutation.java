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
 *     Copyright (C) 2002-2019 Jahia Solutions Group. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.util.ZipUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

/**
 * zip mutations
 *
 * @author yousria
 */
public class GqlZipMutation {

    private JCRNodeWrapper file;

    public GqlZipMutation(JCRNodeWrapper file) {
        this.file = file;
    }

    /**
     *
     * @param pathsOrIds
     * @param environment
     * @return
     * @throws DataFetchingException
     */
    @GraphQLField
    @GraphQLDescription("zip a file")
    public boolean addToZip(@GraphQLName("pathsOrIds") @GraphQLNonNull @GraphQLDescription("list of paths or ids to zip") List<String> pathsOrIds, DataFetchingEnvironment environment) throws DataFetchingException {
        List<JCRNodeWrapper> nodes = new ArrayList<>();
        for (String pathOrId : pathsOrIds) {
            try {
                nodes.add(GqlJcrMutation.getNodeFromPathOrId(JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE), pathOrId));
            } catch (RepositoryException e) {
                throw new DataFetchingException(e);
            }
        }
        ZipUtils.addToZip(nodes, file);
        return true;
    }

    /**
     * GraphQL field to zip a folder with its sub contents
     * @param pathOrId path of the folder to zip
     * @param environment data fetching environment
     * @return always true
     * @throws DataFetchingException
     *//*
    @GraphQLField
    @GraphQLDescription("zip a whole folder with its child nodes")
    public boolean zipFolder(
            @GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the folder node") String pathOrId,
            DataFetchingEnvironment environment
    ) throws DataFetchingException {
        zipFolder(getNodeFromPathOrId(getSession(), pathOrId), getSession(), environment);
        return true;
    }

    *//**
     * GraphQL field to zip several files
     * @param parentPathOrId parent path or Id
     * @param paths list of file paths to zip together
     * @param environment data fetching environment
     * @return always true
     *//*
    @GraphQLField
    @GraphQLDescription("zip several files")
    public boolean zipFiles(
            @GraphQLName("parentPathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the parent node") String parentPathOrId,
            @GraphQLName("paths") @GraphQLNonNull @GraphQLDescription("list of file paths") List<String> paths,
            DataFetchingEnvironment environment) {
        List<JCRNodeWrapper> nodes = new ArrayList<>();
        for (String path : paths) {
            nodes.add(getNodeFromPathOrId(getSession(), path));
        }
        zipFiles(nodes, getNodeFromPathOrId(getSession(), parentPathOrId), getSession(), environment);
        return true;
    }

    *//**
     * GraphQL field to unzip a zip file
     * @param pathOrId of the file to unzip
     * @param parentPathOrId parent path
     * @param environment data fetching environment
     * @return always true
     * @throws DataFetchingException
     *//*
    @GraphQLField
    @GraphQLDescription("unzip file")
    public boolean unzipFile(@GraphQLName("pathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the file node") String pathOrId,
            @GraphQLName("parentPathOrId") @GraphQLNonNull @GraphQLDescription("The path or id of the parent node") String parentPathOrId,
            DataFetchingEnvironment environment) throws DataFetchingException {
        JCRNodeWrapper file = getNodeFromPathOrId(getSession(), pathOrId);
        JCRNodeWrapper parent = getNodeFromPathOrId(getSession(), parentPathOrId);
        unzipFile(file, parent, getSession());
        return true;
    }*/
}
