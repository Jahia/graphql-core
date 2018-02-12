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
 *      it under the terms of the GNU General Public License as published by
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

package org.jahia.modules.graphql.provider.dxm.node;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.fileupload.FileItem;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.DataModificationException;
import org.jahia.modules.graphql.provider.dxm.upload.UploadHelper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;

import javax.jcr.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@GraphQLName("JCRPropertyMutation")
@GraphQLDescription("Mutations on a JCR property")
public class GqlJcrPropertyMutation extends GqlJcrMutationSupport {

    private JCRNodeWrapper node;
    private String name;

    public GqlJcrPropertyMutation(JCRNodeWrapper node, String name) {
        this.node = node;
        this.name = name;
    }

    public GqlJcrPropertyMutation(JCRPropertyWrapper property) {
        try {
            this.node = property.getParent();
            this.name = property.getName();
        } catch (RepositoryException e) {
            throw new DataModificationException(e);
        }
    }

    @GraphQLField
    @GraphQLDescription("Get the graphQL representation of the property currently being mutated")
    public GqlJcrProperty getProperty() throws BaseGqlClientException {
        try {
            return new GqlJcrProperty(node.getProperty(name), SpecializedTypesHandler.getNode(node));
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    @GraphQLField
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
            DataFetchingEnvironment environment)
    throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            JCRSessionWrapper session = localizedNode.getSession();
            localizedNode.setProperty(name, getValue(type, value, session, environment));
        } catch (RepositoryException | IOException e) {
            throw new DataModificationException(e);
        }
        return true;
    }

    @GraphQLField
    @GraphQLName("setValues")
    @GraphQLDescription("Set property values")
    public boolean setValues(@GraphQLName("language") String language,
            @GraphQLName("type") GqlJcrPropertyType type,
            @GraphQLName("values") List<String> values, DataFetchingEnvironment environment)
    throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            JCRSessionWrapper session = localizedNode.getSession();
            localizedNode.setProperty(name, getValues(type, values, session, environment));
        } catch (RepositoryException | IOException e) {
            throw new DataModificationException(e);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Add a new value to this property")
    public boolean addValue(@GraphQLName("language") String language,
            @GraphQLName("type") GqlJcrPropertyType type,
            @GraphQLName("value") String value, DataFetchingEnvironment environment)
    throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            JCRSessionWrapper session = localizedNode.getSession();
            localizedNode.getProperty(name).addValue(getValue(type, value, session, environment));
        } catch (RepositoryException | IOException e) {
            throw new DataModificationException(e);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Remove a new value from this property")
    public boolean removeValue(@GraphQLName("language") String language,
            @GraphQLName("type") GqlJcrPropertyType type,
            @GraphQLName("value") String value, DataFetchingEnvironment environment)
    throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            JCRSessionWrapper session = localizedNode.getSession();
            localizedNode.getProperty(name).removeValue(getValue(type, value, session, environment));
        } catch (RepositoryException | IOException e) {
            throw new DataModificationException(e);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Add new values to this property")
    public boolean addValues(@GraphQLName("language") String language,
            @GraphQLName("type") GqlJcrPropertyType type,
            @GraphQLName("values") List<String> values, DataFetchingEnvironment environment)
    throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            JCRSessionWrapper session = localizedNode.getSession();
            localizedNode.getProperty(name).addValues(getValues(type, values, session, environment));
        } catch (RepositoryException | IOException e) {
            throw new DataModificationException(e);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Remove values from this property")
    public boolean removeValues(@GraphQLName("language") String language,
            @GraphQLName("type") GqlJcrPropertyType type,
            @GraphQLName("values") List<String> values, DataFetchingEnvironment environment)
    throws BaseGqlClientException {
        try {
            JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, language);
            JCRSessionWrapper session = localizedNode.getSession();
            localizedNode.getProperty(name).removeValues(getValues(type, values, session, environment));
        } catch (RepositoryException | IOException e) {
            throw new DataModificationException(e);
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
            throw new DataModificationException(e);
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

    private Value getValue(@GraphQLName("type") GqlJcrPropertyType type, @GraphQLName("value") String value, JCRSessionWrapper session, DataFetchingEnvironment environment) throws RepositoryException, IOException {
        Value result = null;
        ValueFactory valueFactory = session.getValueFactory();
        JCRNodeWrapper referencedNode;
        int jcrType = getPropertyType(type);
        switch (jcrType){
            case PropertyType.REFERENCE:
                referencedNode = getNodeFromPathOrId(session, value);
                result = valueFactory.createValue(referencedNode);
                break;
            case PropertyType.WEAKREFERENCE:
                referencedNode = getNodeFromPathOrId(session, value);
                result = valueFactory.createValue(referencedNode, true);
                break;
            case PropertyType.BINARY:
                if(UploadHelper.isFileUpload(value, environment)){
                    FileItem file = UploadHelper.getFileUpload(value, environment);
                    Binary binary = valueFactory.createBinary(file.getInputStream());
                    result = valueFactory.createValue(binary);
                }else{
                    result = session.getValueFactory().createValue(value, jcrType);
                }
                break;
            default:
                result = session.getValueFactory().createValue(value, jcrType);
        }
        return result;
    }

    private Value[] getValues(@GraphQLName("type") GqlJcrPropertyType type, @GraphQLName("values") List<String> values, JCRSessionWrapper session, DataFetchingEnvironment environment) throws RepositoryException, IOException {
        List<Value> jcrValues = new ArrayList<>();
        JCRNodeWrapper referencedNode;
        ValueFactory valueFactory = session.getValueFactory();
        int jcrType = getPropertyType(type);
        for (String value : values) {
            switch (jcrType){
                case PropertyType.REFERENCE:
                    referencedNode = getNodeFromPathOrId(session, value);
                    jcrValues.add(valueFactory.createValue(referencedNode));
                    break;
                case PropertyType.WEAKREFERENCE:
                    referencedNode = getNodeFromPathOrId(session, value);
                    jcrValues.add(valueFactory.createValue(referencedNode, true));
                    break;
                case PropertyType.BINARY:
                    if (UploadHelper.isFileUpload(value, environment)) {
                        FileItem file = UploadHelper.getFileUpload(value, environment);
                        Binary binary = valueFactory.createBinary(file.getInputStream());
                        jcrValues.add(valueFactory.createValue(binary));
                    } else {
                        jcrValues.add(session.getValueFactory().createValue(value, jcrType));
                    }
                    break;
                default:
                    jcrValues.add(session.getValueFactory().createValue(value, jcrType));
            }
        }
        return jcrValues.toArray(new Value[jcrValues.size()]);
    }
}
