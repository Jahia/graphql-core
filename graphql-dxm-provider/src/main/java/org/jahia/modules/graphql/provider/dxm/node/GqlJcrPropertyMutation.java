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

import graphql.ErrorType;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRSessionWrapper;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import java.util.ArrayList;
import java.util.List;

import static org.jahia.modules.graphql.provider.dxm.node.GqlJcrMutation.getNodeFromPathOrId;
import static org.jahia.modules.graphql.provider.dxm.node.NodeHelper.getNodeInLanguage;

@GraphQLName("JCRPropertyMutation")
@GraphQLDescription("Mutations on a JCR property")
public class GqlJcrPropertyMutation {

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
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
    }


    @GraphQLField
    @GraphQLDescription("Get the graphQL representation of the property currently being mutated")
    public GqlJcrProperty getProperty() {
        try {
            return new GqlJcrProperty(node.getProperty(name), SpecializedTypesHandler.getNode(node));
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
    }

    @GraphQLField
    @GraphQLDescription("Get the path of the property currently being mutated")
    public String getPath() {
        return node.getPath() + "/" + name;
    }


    @GraphQLField
    @GraphQLName("setValue")
    @GraphQLDescription("Set property value")
    public boolean setValue(@GraphQLName("language") String language,
                         @GraphQLName("type") GqlJcrPropertyType type,
                         @GraphQLName("value") String value) {
        try {
            JCRNodeWrapper localizedNode = getNodeInLanguage(node, language);
            JCRSessionWrapper session = localizedNode.getSession();
            localizedNode.setProperty(name, getValue(type, value, session));
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
        return true;
    }

    @GraphQLField
    @GraphQLName("setValues")
    @GraphQLDescription("Set property values")
    public boolean setValues(@GraphQLName("language") String language,
                          @GraphQLName("type") GqlJcrPropertyType type,
                          @GraphQLName("values") List<String> values) {
        try {
            JCRNodeWrapper localizedNode = getNodeInLanguage(node, language);
            JCRSessionWrapper session = localizedNode.getSession();
            localizedNode.setProperty(name, getValues(type, values, session));
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Add a new value to this property")
    public boolean addValue(@GraphQLName("language") String language,
                         @GraphQLName("type") GqlJcrPropertyType type,
                         @GraphQLName("value") String value) {
        try {
            JCRNodeWrapper localizedNode = getNodeInLanguage(node, language);
            JCRSessionWrapper session = localizedNode.getSession();
            localizedNode.getProperty(name).addValue(getValue(type, value, session));
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Remove a new value from this property")
    public boolean removeValue(@GraphQLName("language") String language,
                            @GraphQLName("type") GqlJcrPropertyType type,
                            @GraphQLName("value") String value) {
        try {
            JCRNodeWrapper localizedNode = getNodeInLanguage(node, language);
            JCRSessionWrapper session = localizedNode.getSession();
            localizedNode.getProperty(name).removeValue(getValue(type, value, session));
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Add new values to this property")
    public boolean addValues(@GraphQLName("language") String language,
                         @GraphQLName("type") GqlJcrPropertyType type,
                         @GraphQLName("values") List<String> values) {
        try {
            JCRNodeWrapper localizedNode = getNodeInLanguage(node, language);
            JCRSessionWrapper session = localizedNode.getSession();
            localizedNode.getProperty(name).addValues(getValues(type, values, session));
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Remove values from this property")
    public boolean removeValues(@GraphQLName("language") String language,
                            @GraphQLName("type") GqlJcrPropertyType type,
                            @GraphQLName("values") List<String> values) {
        try {
            JCRNodeWrapper localizedNode = getNodeInLanguage(node, language);
            JCRSessionWrapper session = localizedNode.getSession();
            localizedNode.getProperty(name).removeValues(getValues(type, values, session));
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
        return true;
    }

    @GraphQLField
    @GraphQLDescription("Delete this property")
    public boolean delete(@GraphQLName("language") String language) {
        try {
            JCRNodeWrapper localizedNode = getNodeInLanguage(node, language);
            localizedNode.getProperty(name).remove();
        } catch (RepositoryException e) {
            throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
        }
        return true;
    }

    private Value getValue(@GraphQLName("type") GqlJcrPropertyType type, @GraphQLName("value") String value, JCRSessionWrapper session) throws ValueFormatException {
        int jcrType = type != null ? type.getValue() : PropertyType.STRING;
        if(jcrType == PropertyType.REFERENCE || jcrType == PropertyType.WEAKREFERENCE){
            JCRNodeWrapper referencedNode;
            try {
                referencedNode = getNodeFromPathOrId(session, value);
                return session.getValueFactory().createValue(referencedNode);
            } catch (RepositoryException e) {
                throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
            }
        }else {
            return session.getValueFactory().createValue(value, jcrType);
        }
    }

    private Value[] getValues(@GraphQLName("type") GqlJcrPropertyType type, @GraphQLName("values") List<String> values, JCRSessionWrapper session) throws ValueFormatException {
        int jcrType = type != null ? type.getValue() : PropertyType.STRING;

        List<Value> jcrValues = new ArrayList<>();
        for (String value : values) {
            if(jcrType == PropertyType.REFERENCE || jcrType == PropertyType.WEAKREFERENCE){
                JCRNodeWrapper referencedNode = null;
                try {
                    referencedNode = getNodeFromPathOrId(session, value);
                    jcrValues.add(session.getValueFactory().createValue(referencedNode));
                } catch (RepositoryException e) {
                    throw new BaseGqlClientException(e, ErrorType.DataFetchingException);
                }
            }else {
                jcrValues.add(session.getValueFactory().createValue(value, jcrType));
            }
        }

        return jcrValues.toArray(new Value[jcrValues.size()]);
    }
}
