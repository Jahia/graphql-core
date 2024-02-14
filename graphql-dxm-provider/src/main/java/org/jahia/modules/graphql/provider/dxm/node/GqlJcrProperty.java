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
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.bin.Render;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.render.RenderRequestAttributeInput;
import org.jahia.modules.graphql.provider.dxm.util.ContextUtil;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.initializers.ChoiceListInitializerService;
import org.jahia.services.content.nodetypes.renderer.ChoiceListRenderer;
import org.jahia.services.content.nodetypes.renderer.ChoiceListRendererService;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.RenderService;
import org.jahia.services.render.Resource;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.EncryptionUtils;
import org.jahia.utils.LanguageCodeConverters;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.jahia.modules.graphql.provider.dxm.node.GqlJcrMutationSupport.DEFAULT_DATE_FORMAT;

/**
 * GraphQL representation of a JCR property.
 */
@GraphQLName("JCRProperty")
@GraphQLDescription("GraphQL representation of a JCR property.")
public class GqlJcrProperty {
    private JCRPropertyWrapper property;
    private GqlJcrNode node;

    /**
     * Create an instance that represents a JCR property to GraphQL.
     *
     * @param property The JCR property to represent
     * @param node     The GraphQL representation of the JCR node the property belongs to
     */
    public GqlJcrProperty(JCRPropertyWrapper property, GqlJcrNode node) {
        this.property = property;
        this.node = node;
    }

    /**
     * Get underlying JCR property.
     *
     * @return underlying JCR property
     */
    public JCRPropertyWrapper getProperty() {
        return property;
    }

    /**
     * @return The name of the JCR property
     */
    @GraphQLField
    @GraphQLName("name")
    @GraphQLNonNull
    @GraphQLDescription("The name of the JCR property")
    public String getName() {
        try {
            return property.getName();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * @return The path of the JCR property
     */
    @GraphQLField
    @GraphQLName("path")
    @GraphQLNonNull
    @GraphQLDescription("The path of the JCR property")
    public String getPath() {
        try {
            return property.getPath();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * @return The type of the JCR property
     */
    @GraphQLField
    @GraphQLName("type")
    @GraphQLNonNull
    @GraphQLDescription("The type of the JCR property")
    public GqlJcrPropertyType getType() {
        try {
            return GqlJcrPropertyType.fromValue(property.getType());
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * @return Whether the property is internationalized
     */
    @GraphQLField
    @GraphQLName("internationalized")
    @GraphQLNonNull
    @GraphQLDescription("Whether the property is internationalized")
    public boolean isInternationalized() {
        ExtendedPropertyDefinition propertyDefinition;
        try {
            propertyDefinition = node.getNode().getApplicablePropertyDefinition(getName(), property.getType(), property.isMultiple());
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        return propertyDefinition.isInternationalized();
    }

    /**
     * @return The language the property value was obtained in for internationalized properties; null for non-internationalized ones
     */
    @GraphQLField
    @GraphQLName("language")
    @GraphQLDescription("The language the property value was obtained in for internationalized properties; null for non-internationalized ones")
    public String getLanguage() {
        try {
            return property.getLocale();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * @return The value of the JCR property as a String in case the property is single-valued, null otherwise
     */
    @GraphQLField
    @GraphQLName("value")
    @GraphQLDescription("The value of the JCR property as a String in case the property is single-valued, null otherwise")
    public String getValue() {
        try {
            if (property.isMultiple()) {
                return null;
            }
            return property.getValue().getString();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * @return The value of the JCR property casted as date and returned in this string format: [yyyy-MM-dd'T'HH:mm:ss.SSS]
     * in case the property is single-valued, null otherwise
     */
    @GraphQLField
    @GraphQLName("notZonedDateValue")
    @GraphQLDescription("The value of the JCR property casted as date and returned in this string format: [yyyy-MM-dd'T'HH:mm:ss.SSS] in case the property is single-valued, null otherwise")
    public String getNotZonedDateValue() {
        try {
            if (property.isMultiple() || property.getType() != PropertyType.DATE) {
                return null;
            }

            SimpleDateFormat defaultDataFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);

            return defaultDataFormat.format(property.getValue().getTime());
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * @return The decrypted value of the JCR encrypted property as a String in case the property is single-valued, null otherwise
     */
    @GraphQLField
    @GraphQLName("decryptedValue")
    @GraphQLDescription("The decrypted value of the JCR encrypted property as a String in case the property is single-valued, null otherwise")
    public String getDecryptedValue() throws RepositoryException {
        try {
            if (property.isMultiple()) {
                return null;
            }

            return EncryptionUtils.passwordBaseDecrypt(property.getValue().getString());
        } catch (EncryptionOperationNotPossibleException e) {
            return null;
        }
    }

    /**
     * @return The values of the JCR property as a Strings in case the property is multiple-valued, null otherwise
     */
    @GraphQLField
    @GraphQLName("values")
    @GraphQLDescription("The values of the JCR property as Strings in case the property is multiple-valued, null otherwise")
    public List<String> getValues() {
        try {
            if (!property.isMultiple()) {
                return null;
            }
            JCRValueWrapper[] values = property.getValues();
            List<String> result = new ArrayList<>(values.length);
            for (JCRValueWrapper value : values) {
                result.add(value.getString());
            }
            return result;
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * @return The values of the JCR property casted as date and returned in this string format: [yyyy-MM-dd'T'HH:mm:ss.SSS]
     * in case the property is multiple-valued, null otherwise
     */
    @GraphQLField
    @GraphQLName("notZonedDateValues")
    @GraphQLDescription("The values of the JCR property casted as date and returned in this string format: [yyyy-MM-dd'T'HH:mm:ss.SSS] in case the property is multiple-valued, null otherwise")
    public List<String> getNotZonedDateValues() {
        try {
            if (!property.isMultiple() || property.getType() != PropertyType.DATE) {
                return null;
            }
            JCRValueWrapper[] notZonedDateValues = property.getValues();
            List<String> result = new ArrayList<>(notZonedDateValues.length);

            SimpleDateFormat defaultDateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);

            for (JCRValueWrapper value : notZonedDateValues) {
                result.add(defaultDateFormat.format(value.getDate().getTime()));
            }
            return result;
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * @return The decrypted value of the JCR encrypted property as a String in case the property is single-valued, null otherwise
     */
    @GraphQLField
    @GraphQLName("decryptedValues")
    @GraphQLDescription("The decrypted values of the JCR encrypted property as a Strings in case the property is multiple-valued, null otherwise")
    public List<String> getDecryptedValues() throws RepositoryException {
        try {
            if (!property.isMultiple()) {
                return Collections.emptyList();
            }

            List<String> result = new ArrayList<>();
            for (JCRValueWrapper value : property.getValues()) {
                result.add(EncryptionUtils.passwordBaseDecrypt(value.getString()));
            }
            return result;
        } catch (EncryptionOperationNotPossibleException e) {
            return Collections.emptyList();
        }
    }

    /**
     * @return The value of the JCR property as a Long in case the property is single-valued, null otherwise
     */
    @GraphQLField
    @GraphQLName("longValue")
    @GraphQLDescription("The value of the JCR property as a Long in case the property is single-valued, null otherwise")
    public Long getLongValue() {
        try {
            if (property.isMultiple()) {
                return null;
            }
            return property.getValue().getLong();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * @return The binary size of the JCR node as a Long, null otherwise
     */
    @GraphQLField
    @GraphQLName("size")
    @GraphQLDescription("The binary size of the JCR node as a Long, null otherwise")
    public Long getSize() {
        try {
            if (property.isMultiple() && property.getType() != PropertyType.BINARY) {
                return null;
            }
            return property.getBinary().getSize();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * @return The values of the JCR property as Longs in case the property is multiple-valued, null otherwise
     */
    @GraphQLField
    @GraphQLName("longValues")
    @GraphQLDescription("The values of the JCR property as Longs in case the property is multiple-valued, null otherwise")
    public List<Long> getLongValues() {
        try {
            if (!property.isMultiple()) {
                return null;
            }
            JCRValueWrapper[] values = property.getValues();
            List<Long> result = new ArrayList<>(values.length);
            for (JCRValueWrapper value : values) {
                result.add(value.getLong());
            }
            return result;
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * @return The value of the JCR property as a Float in case the property is single-valued, null otherwise
     */
    @GraphQLField
    @GraphQLName("floatValue")
    @GraphQLDescription("The value of the JCR property as a Float in case the property is single-valued, null otherwise")
    public Double getFloatValue() {
        try {
            if (property.isMultiple()) {
                return null;
            }
            return property.getValue().getDouble();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * @return The values of the JCR property as Floats in case the property is multiple-valued, null otherwise
     */
    @GraphQLField
    @GraphQLName("floatValues")
    @GraphQLDescription("The values of the JCR property as Floats in case the property is multiple-valued, null otherwise")
    public List<Double> getFloatValues() {
        try {
            if (!property.isMultiple()) {
                return null;
            }
            JCRValueWrapper[] values = property.getValues();
            List<Double> result = new ArrayList<>(values.length);
            for (JCRValueWrapper value : values) {
                result.add(value.getDouble());
            }
            return result;
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * @return GraphQL representation of the node this property references in case the property is single-valued, null otherwise
     * @throws GqlJcrUnresolvedNodeReferenceException In case either the type (must be REFEENCE, WEAKREFERENCE or STRING) or the actual value of the property do not allow to resolve the node reference
     */
    @GraphQLField
    @GraphQLName("refNode")
    @GraphQLDescription("GraphQL representation of the node this property references in case the property is single-valued, null otherwise")
    public GqlJcrNode getRefNode() throws GqlJcrUnresolvedNodeReferenceException {
        try {
            if (property.isMultiple() || (property.getType() != PropertyType.REFERENCE && property.getType() != PropertyType.WEAKREFERENCE && property.getType() != PropertyType.STRING)) {
                return null;
            }
            return getRefNode(property.getValue());
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * @return The value of the JCR property as a Boolean in case the property is single-valued, null otherwise
     */
    @GraphQLField
    @GraphQLName("booleanValue")
    @GraphQLDescription("The value of the JCR property as a Boolean in case the property is single-valued, null otherwise")
    public Boolean getBooleanValue() {
        try {
            if (property.isMultiple()) {
                return null;
            }
            return property.getValue().getBoolean();
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    @GraphQLField
    @GraphQLName("choicelistValue")
    @GraphQLDescription("The value of the JCR property rendered by the specified choicelist renderer in case the property is single-valued, null otherwise")
    public String getChoicelistValue(@GraphQLName("renderer") @GraphQLDescription("The choicelist renderer name to be used") String renderer, @GraphQLName("language") @GraphQLDescription("The language") String language) {
        try {
            if (property.isMultiple()) {
                return null;
            }
            Locale locale = LanguageCodeConverters.languageCodeToLocale(language);
            ChoiceListRenderer choiceListRenderer = ChoiceListRendererService.getInstance().getRenderers().get(renderer);
            return choiceListRenderer.getStringRendering(locale,
                    (ExtendedPropertyDefinition) property.getDefinition(),
                    property.getValue());
        } catch (RepositoryException e) {
            try {
                return property.getValue().getString();
            } catch (RepositoryException ex) {
                throw new DataFetchingException(e);
            }
        }
    }

    @GraphQLField
    @GraphQLName("choicelistValues")
    @GraphQLDescription("The value of the JCR property rendered by the specified choicelist renderer in case the property is multiple-valued, null otherwise")
    public List<String> getChoicelistValues(@GraphQLName("renderer") @GraphQLDescription("The choicelist renderer name to be used") String renderer, @GraphQLName("language") @GraphQLDescription("The language") String language) {
        try {
            if (!property.isMultiple()) {
                return null;
            }
            Locale locale = LanguageCodeConverters.languageCodeToLocale(language);
            ChoiceListRenderer choiceListRenderer = ChoiceListRendererService.getInstance().getRenderers().get(renderer);
            JCRValueWrapper[] values = property.getValues();
            List<String> result = new ArrayList<>(values.length);
            for (JCRValueWrapper value : values) {
                result.add(choiceListRenderer.getStringRendering(locale,
                        (ExtendedPropertyDefinition) property.getDefinition(), value));
            }
            return result;
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * @return The values of the JCR property as Booleans in case the property is multiple-valued, null otherwise
     */
    @GraphQLField
    @GraphQLName("booleanValues")
    @GraphQLDescription("The values of the JCR property as Booleans in case the property is multiple-valued, null otherwise")
    public List<Boolean> getBooleanValues() {
        try {
            if (!property.isMultiple()) {
                return null;
            }
            JCRValueWrapper[] values = property.getValues();
            List<Boolean> result = new ArrayList<>(values.length);
            for (JCRValueWrapper value : values) {
                result.add(value.getBoolean());
            }
            return result;
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * @return GraphQL representations of the nodes this property references in case the property is multiple-valued, null otherwise
     * @throws GqlJcrUnresolvedNodeReferenceException In case either the type (must be REFEENCE, WEAKREFERENCE or STRING) or any of the actual values of the property do not allow to resolve the node reference
     */
    @GraphQLField
    @GraphQLName("refNodes")
    @GraphQLDescription("GraphQL representations of the nodes this property references in case the property is multiple-valued, null otherwise")
    public List<GqlJcrNode> getRefNodes() throws GqlJcrUnresolvedNodeReferenceException {
        try {
            if (!property.isMultiple() || (property.getType() != PropertyType.REFERENCE && property.getType() != PropertyType.WEAKREFERENCE && property.getType() != PropertyType.STRING)) {
                return null;
            }
            JCRValueWrapper[] values = property.getValues();
            List<GqlJcrNode> result = new ArrayList<>(values.length);
            for (JCRValueWrapper value : values) {
                result.add(getRefNode(value));
            }
            return result;
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * @return The GraphQL representation of the JCR node the property belongs to.
     */
    @GraphQLField
    @GraphQLName("node")
    @GraphQLNonNull
    @GraphQLDescription("The GraphQL representation of the JCR node the property belongs to.")
    public GqlJcrNode getNode() {
        return node;
    }

    private GqlJcrNode getRefNode(JCRValueWrapper value) throws RepositoryException {
        JCRNodeWrapper refNode;
        try {
            refNode = value.getNode();
        } catch (ValueFormatException e) {
            throw new GqlJcrUnresolvedNodeReferenceException("The '" + property.getName() + "' property is not of a reference type", e);
        }

        return refNode == null ? null : SpecializedTypesHandler.getNode(refNode);
    }
}
