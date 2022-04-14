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
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;

import javax.jcr.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mutation object for JCR node property modifications.
 */
@GraphQLName("JCRPropertyMutation")
@GraphQLDescription("Mutations on a JCR property")
public class GqlJcrPropertyMutation extends GqlJcrMutationSupport {

    private JCRNodeWrapper node;
    private String name;

    /**
     * Initializes an instance of this class.
     *
     * @param node the corresponding JCR node
     * @param name the name of the node property
     */
    public GqlJcrPropertyMutation(JCRNodeWrapper node, String name) {
        this.node = node;
        this.name = name;
    }

    /**
     * Initializes an instance of this class.
     *
     * @param property the corresponding JCR node property
     */
    public GqlJcrPropertyMutation(JCRPropertyWrapper property) {
        try {
            this.node = property.getParent();
            this.name = property.getName();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    @GraphQLField
    @GraphQLName("property")
    @GraphQLDescription("Get the graphQL representation of the property currently being mutated")
    public GqlJcrProperty getProperty() throws BaseGqlClientException {
        try {
            return new GqlJcrProperty(node.getProperty(name), SpecializedTypesHandler.getNode(node));
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    @GraphQLField
    @GraphQLName("path")
    @GraphQLDescription("Get the path of the property currently being mutated")
    public String getPath() throws BaseGqlClientException {
        return node.getPath() + "/" + name;
    }


    @GraphQLField
    @GraphQLName("setValue")
    @GraphQLDescription("Set property value")
    public boolean setValue(@GraphQLName("language") String language,
                            @GraphQLName("type") GqlJcrPropertyType type,
                            @GraphQLName("option") GqlJcrPropertyOption option,
                            @GraphQLName("value") String value,
                            DataFetchingEnvironment environment)
            throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            localizedNode.setProperty(name, getValue(getPropertyType(type), option, value, localizedNode.getSession(), environment));
            localizedNode.getSession().validate();
        } catch (RepositoryException | IOException | FileSizeLimitExceededException e) {
            throw NodeMutationConstraintViolationHandler.transformException(e);
        }
        return true;
    }

    @GraphQLField
    @GraphQLName("setValues")
    @GraphQLDescription("Set property values")
    public boolean setValues(@GraphQLName("language") String language,
                             @GraphQLName("type") GqlJcrPropertyType type,
                             @GraphQLName("option") GqlJcrPropertyOption option,
                             @GraphQLName("values") List<String> values,
                             DataFetchingEnvironment environment)
            throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            localizedNode.setProperty(name, getValues(type, option, values, localizedNode.getSession(), environment));
            localizedNode.getSession().validate();
        } catch (RepositoryException | IOException | FileSizeLimitExceededException e) {
            throw NodeMutationConstraintViolationHandler.transformException(e);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Add a new value to this property")
    public boolean addValue(@GraphQLName("language") String language,
                            @GraphQLName("type") GqlJcrPropertyType type,
                            @GraphQLName("option") GqlJcrPropertyOption option,
                            @GraphQLName("value") String value,
                            DataFetchingEnvironment environment)
            throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            localizedNode.getProperty(name).addValue(getValue(getPropertyType(type), option, value, localizedNode.getSession(), environment));
            localizedNode.getSession().validate();
        } catch (RepositoryException | IOException | FileSizeLimitExceededException e) {
            throw NodeMutationConstraintViolationHandler.transformException(e);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Remove a new value from this property")
    public boolean removeValue(@GraphQLName("language") String language,
                               @GraphQLName("type") GqlJcrPropertyType type,
                               @GraphQLName("option") GqlJcrPropertyOption option,
                               @GraphQLName("value") String value,
                               DataFetchingEnvironment environment)
            throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            localizedNode.getProperty(name).removeValue(getValue(getPropertyType(type), option, value, localizedNode.getSession(), environment));
            localizedNode.getSession().validate();
        } catch (RepositoryException | IOException | FileSizeLimitExceededException e) {
            throw NodeMutationConstraintViolationHandler.transformException(e);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Add new values to this property")
    public boolean addValues(@GraphQLName("language") String language,
                             @GraphQLName("type") GqlJcrPropertyType type,
                             @GraphQLName("option") GqlJcrPropertyOption option,
                             @GraphQLName("values") List<String> values,
                             DataFetchingEnvironment environment)
            throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            localizedNode.getProperty(name).addValues(getValues(type, option, values, localizedNode.getSession(), environment));
            localizedNode.getSession().validate();
        } catch (RepositoryException | IOException | FileSizeLimitExceededException e) {
            throw NodeMutationConstraintViolationHandler.transformException(e);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Remove values from this property")
    public boolean removeValues(@GraphQLName("language") String language,
                                @GraphQLName("type") GqlJcrPropertyType type,
                                @GraphQLName("option") GqlJcrPropertyOption option,
                                @GraphQLName("values") List<String> values,
                                DataFetchingEnvironment environment)
            throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            localizedNode.getProperty(name).removeValues(getValues(type, option, values, localizedNode.getSession(), environment));
            localizedNode.getSession().validate();
        } catch (RepositoryException | IOException | FileSizeLimitExceededException e) {
            throw NodeMutationConstraintViolationHandler.transformException(e);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Delete this property")
    public boolean delete(@GraphQLName("language") String language) throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            localizedNode.getProperty(name).remove();
            localizedNode.getSession().validate();
        } catch (RepositoryException e) {
            throw NodeMutationConstraintViolationHandler.transformException(e);
        }
        return true;
    }

    private int getPropertyType(GqlJcrPropertyType type) throws RepositoryException {
        if (type != null) {
            return type.getValue();
        }
        ExtendedPropertyDefinition def = node.getApplicablePropertyDefinition(this.name);
        return def != null && def.getRequiredType() != PropertyType.UNDEFINED ? def.getRequiredType()
                : PropertyType.STRING;
    }

    private Value[] getValues(GqlJcrPropertyType type, GqlJcrPropertyOption option, List<String> values, JCRSessionWrapper session, DataFetchingEnvironment environment) throws RepositoryException, IOException, FileSizeLimitExceededException {
        List<Value> jcrValues = new ArrayList<>();
        int jcrType = getPropertyType(type);

        for (String value : (values == null ? Collections.<String>emptyList() : values)) {
            jcrValues.add(getValue(jcrType, option, value, session, environment));
        }

        return jcrValues.toArray(new Value[0]);
    }
}
