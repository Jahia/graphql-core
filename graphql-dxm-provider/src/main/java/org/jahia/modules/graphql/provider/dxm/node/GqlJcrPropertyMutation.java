/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
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
                            @GraphQLName("value") String value,
                            @GraphQLName("notZonedDateValue") String notZonedDateValue,
                            DataFetchingEnvironment environment)
            throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            localizedNode.setProperty(name, getValue(type, value, notZonedDateValue, localizedNode.getSession(), environment));
        } catch (RepositoryException | IOException | FileSizeLimitExceededException e) {
            throw new DataFetchingException(e);
        }
        return true;
    }

    @GraphQLField
    @GraphQLName("setValues")
    @GraphQLDescription("Set property values")
    public boolean setValues(@GraphQLName("language") String language,
                             @GraphQLName("type") GqlJcrPropertyType type,
                             @GraphQLName("values") List<String> values,
                             @GraphQLName("notZonedDateValues") List<String> notZonedDateValues,
                             DataFetchingEnvironment environment)
            throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            localizedNode.setProperty(name, getValues(type, values, notZonedDateValues, localizedNode.getSession(), environment));
        } catch (RepositoryException | IOException | FileSizeLimitExceededException e) {
            throw new DataFetchingException(e);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Add a new value to this property")
    public boolean addValue(@GraphQLName("language") String language,
                            @GraphQLName("type") GqlJcrPropertyType type,
                            @GraphQLName("value") String value,
                            @GraphQLName("notZonedDateValue") String notZonedDateValue,
                            DataFetchingEnvironment environment)
            throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            localizedNode.getProperty(name).addValue(getValue(type, value, notZonedDateValue, localizedNode.getSession(), environment));
        } catch (RepositoryException | IOException | FileSizeLimitExceededException e) {
            throw new DataFetchingException(e);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Remove a new value from this property")
    public boolean removeValue(@GraphQLName("language") String language,
                               @GraphQLName("type") GqlJcrPropertyType type,
                               @GraphQLName("value") String value,
                               @GraphQLName("notZonedDateValue") String notZonedDateValue,
                               DataFetchingEnvironment environment)
            throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            localizedNode.getProperty(name).removeValue(getValue(type, value, notZonedDateValue, localizedNode.getSession(), environment));
        } catch (RepositoryException | IOException | FileSizeLimitExceededException e) {
            throw new DataFetchingException(e);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Add new values to this property")
    public boolean addValues(@GraphQLName("language") String language,
                             @GraphQLName("type") GqlJcrPropertyType type,
                             @GraphQLName("values") List<String> values,
                             @GraphQLName("notZonedDateValues") List<String> notZonedDateValues,
                             DataFetchingEnvironment environment)
            throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            localizedNode.getProperty(name).addValues(getValues(type, values, notZonedDateValues, localizedNode.getSession(), environment));
        } catch (RepositoryException | IOException | FileSizeLimitExceededException e) {
            throw new DataFetchingException(e);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Remove values from this property")
    public boolean removeValues(@GraphQLName("language") String language,
                                @GraphQLName("type") GqlJcrPropertyType type,
                                @GraphQLName("values") List<String> values,
                                @GraphQLName("notZonedDateValues") List<String> notZonedDateValues,
                                DataFetchingEnvironment environment)
            throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            localizedNode.getProperty(name).removeValues(getValues(type, values, notZonedDateValues, localizedNode.getSession(), environment));
        } catch (RepositoryException | IOException | FileSizeLimitExceededException e) {
            throw new DataFetchingException(e);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Delete this property")
    public boolean delete(@GraphQLName("language") String language) throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            localizedNode.getProperty(name).remove();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
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

    private Value getValue(GqlJcrPropertyType type, String value, String notZonedDateValue, JCRSessionWrapper session, DataFetchingEnvironment environment) throws RepositoryException, IOException, FileSizeLimitExceededException {
        return getValue(getPropertyType(type), value, notZonedDateValue, session, environment);
    }

    private Value[] getValues(GqlJcrPropertyType type, List<String> values, List<String> notZonedDateValues, JCRSessionWrapper session, DataFetchingEnvironment environment) throws RepositoryException, IOException, FileSizeLimitExceededException {
        List<Value> jcrValues = new ArrayList<>();
        int jcrType = getPropertyType(type);
        for (String value : values) {
            jcrValues.add(getValue(jcrType, value, null, session, environment));
        }

        for (String notZonedDateValue : notZonedDateValues) {
            jcrValues.add(getValue(jcrType, null, notZonedDateValue, session, environment));
        }

        return jcrValues.toArray(new Value[jcrValues.size()]);
    }
}
