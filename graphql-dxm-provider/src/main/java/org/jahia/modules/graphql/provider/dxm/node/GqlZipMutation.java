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
import org.jahia.services.content.JCRSessionWrapper;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

/**
 * zip mutations
 *
 * @author yousria
 */

@GraphQLName("ZipFileMutation")
public class GqlZipMutation {

    private JCRNodeWrapper file;

    public GqlZipMutation(JCRNodeWrapper file) {
        this.file = file;
    }

    /**
     * graphQL field to zip a file / several files / folder or add files to an existing zip file (without duplicates)
     *
     * @param paths
     * @param environment
     * @return
     * @throws DataFetchingException
     */
    @GraphQLField
    @GraphQLDescription("zip a file")
    public boolean addToZip(@GraphQLName("paths") @GraphQLNonNull @GraphQLDescription("list of paths or ids to zip") List<String> paths, DataFetchingEnvironment environment) throws DataFetchingException {
        List<JCRNodeWrapper> nodes = new ArrayList<>();
        for (String pathOrId : paths) {
            try {
                JCRNodeWrapper node = GqlJcrMutation.getNodeFromPathOrId(JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE), pathOrId);
                if (!node.isNodeType(Constants.JAHIANT_FILE) && !node.isNodeType(Constants.JAHIANT_FOLDER)) {
                    throw new DataFetchingException(node.getName() + " is neither a file nor a folder");
                }
                nodes.add(node);
            } catch (RepositoryException e) {
                throw new DataFetchingException(e);
            }
        }
        ZipUtils.addToZip(nodes, file);
        return true;
    }

    /**
     * GraphQl field to unzip a zip file into a destination directory
     *
     * @param path destination path to unzip the file we're executing the mutation on
     * @param environment
     * @return
     * @throws DataFetchingException
     */
    @GraphQLField
    @GraphQLDescription("unzip a zip file")
    public boolean unzip(@GraphQLName("path") @GraphQLNonNull @GraphQLDescription("destination path to unzip") String path, DataFetchingEnvironment environment) throws DataFetchingException {
        try {
            JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE);
            if (session.nodeExists(path)) {
                JCRNodeWrapper destinationNode = GqlJcrMutation.getNodeFromPathOrId(session, path);
                if (!destinationNode.isNodeType(Constants.JAHIANT_FOLDER)) {
                    throw new DataFetchingException(path + " is not a folder");
                }
                ZipUtils.unzip(destinationNode, file);
            } else {
                throw new DataFetchingException(path + " this path does not exist");
            }
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        return true;
    }
}
