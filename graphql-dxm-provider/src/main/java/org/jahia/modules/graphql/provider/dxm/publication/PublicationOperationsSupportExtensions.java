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
package org.jahia.modules.graphql.provider.dxm.publication;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlOperationsSupport;
import org.jahia.services.content.JCRPublicationService;

import javax.jcr.RepositoryException;

@GraphQLTypeExtension(GqlOperationsSupport.class)
public class PublicationOperationsSupportExtensions {

    private GqlJcrNode gqlJcrNode;

    public PublicationOperationsSupportExtensions(GqlOperationsSupport gqlOperationsSupport) {
        this.gqlJcrNode = gqlOperationsSupport.getNode();
    }

    /**
     * Returns if the node supports publication
     *
     * @return  does the node supports publication
     */
    @GraphQLField
    @GraphQLNonNull
    @GraphQLDescription("does the node supports publication")
    public boolean publication() {
        try {
            return JCRPublicationService.supportsPublication(gqlJcrNode.getNode().getSession(), gqlJcrNode.getNode());
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }
}
