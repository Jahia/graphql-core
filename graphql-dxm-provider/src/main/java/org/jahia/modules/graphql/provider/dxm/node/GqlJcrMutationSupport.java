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

import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.apache.commons.io.FileUtils;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.upload.UploadHelper;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.importexport.DocumentViewImportHandler;
import org.jahia.services.importexport.ImportExportBaseService;
import org.jahia.services.importexport.validation.ValidationResults;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.EncryptionUtils;
import org.springframework.core.io.FileSystemResource;

import javax.jcr.*;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Contains resources commonly used by GraphQL JCR mutations internally.
 */
public class GqlJcrMutationSupport {
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    /**
     * Add a child node to the specified one.
     *
     * @param parent The JCR node to add a child to
     * @param node   GraphQL representation of the child node to be added
     * @return The child JCR node that was added
     */
    public static JCRNodeWrapper addNode(JCRNodeWrapper parent, GqlJcrNodeInput node) {
        JCRNodeWrapper jcrNode;
        try {
            String nodeName = JCRContentUtils.escapeLocalNodeName(node.getName());
            Boolean useAvailableNodeName = node.useAvailableNodeName();
            if (useAvailableNodeName != null && useAvailableNodeName) {
                nodeName = JCRContentUtils.findAvailableNodeName(parent, nodeName);
            }
            jcrNode = parent.addNode(nodeName, node.getPrimaryNodeType());
            if (node.getMixins() != null) {
                for (String mixin : node.getMixins()) {
                    jcrNode.addMixin(mixin);
                }
            }
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
        if (node.getProperties() != null) {
            setProperties(jcrNode, node.getProperties());
        }
        if (node.getChildren() != null) {
            for (GqlJcrNodeInput child : node.getChildren()) {
                addNode(jcrNode, child);
            }
        }
        return jcrNode;
    }

    /**
     * Set the provided properties to the specified node.
     *
     * @param node       The JCR node to set properties to
     * @param properties the collection of properties to be set
     * @return The result of the operation, containing list of modified JCR properties
     */
    public static List<JCRPropertyWrapper> setProperties(JCRNodeWrapper node, Collection<GqlJcrPropertyInput> properties) {
        try {
            List<JCRPropertyWrapper> result = new ArrayList<>();
            for (GqlJcrPropertyInput property : properties) {
                JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, property.getLanguage());
                JCRSessionWrapper session = localizedNode.getSession();
                int type = (property.getType() != null ? property.getType().getValue() : PropertyType.STRING);

                if (property.getValue() != null) {
                    Value v = getValue(type, property.getOption(), property.getValue(), session, null);
                    result.add(localizedNode.setProperty(property.getName(), v));
                } else if (property.getValues() != null) {
                    List<Value> values = new ArrayList<>();
                    if (property.getValues() != null) {
                        for (String value : property.getValues()) {
                            values.add(getValue(type, property.getOption(), value, session, null));
                        }
                    }
                    result.add(localizedNode.setProperty(property.getName(), values.toArray(new Value[0])));
                }
            }
            return result;
        } catch (RepositoryException | FileSizeLimitExceededException | IOException e) {
            throw NodeMutationConstraintViolationHandler.transformException(e);
        }
    }

    /**
     * Delete the provided properties to the specified node.
     *
     * @param node       The JCR node to delete properties from
     * @param properties the collection of properties to be deleted
     * @return Boolean
     */
    public static boolean deleteProperties(JCRNodeWrapper node, Collection<GqlJcrDeletedPropertyInput> properties) {
        if (properties == null) {
            return false;
        }

        for (GqlJcrDeletedPropertyInput prop : properties) {
            try {
                JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, prop.getLanguage());
                if (localizedNode.hasProperty(prop.getName())) {
                    localizedNode.getProperty(prop.getName()).remove();
                }
            } catch (RepositoryException e) {
                throw new DataFetchingException(e);
            }
        }

        return true;
    }

    /**
     * Retrieve the specified JCR node by its path or UUID.
     *
     * @param session  JCR session to be used for node retrieval
     * @param pathOrId The string with either node UUID or its path
     * @return The requested JCR node
     */
    public static JCRNodeWrapper getNodeFromPathOrId(JCRSessionWrapper session, String pathOrId) {
        try {
            return ('/' == pathOrId.charAt(0) ? session.getNode(JCRContentUtils.escapeNodePath(pathOrId)) : session.getNodeByIdentifier(pathOrId));
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * Import file upload content under a node. Rename the imported content if
     * a content with the same name is already present under the node
     *
     * @param partName    Name of the request part that contains desired file upload
     * @param node        Parent node to import content under
     * @param environment Data fetching environment
     */
    public static void importFileUpload(String partName, JCRNodeWrapper node, DataFetchingEnvironment environment) throws BaseGqlClientException {
        importFileUpload(partName, node, DocumentViewImportHandler.ROOT_BEHAVIOUR_RENAME, environment);
    }

    /**
     * Import file upload content under a node.
     *
     * @param partName    Name of the request part that contains desired file upload
     * @param node        Parent node to import content under
     * @param rootBehaviour Specify the behaviour in case of existing content
     * @param environment Data fetching environment
     */
    public static void importFileUpload(String partName, JCRNodeWrapper node, int rootBehaviour, DataFetchingEnvironment environment) throws BaseGqlClientException {
        try {
            Part part = UploadHelper.getFileUpload(partName, environment);
            ImportExportBaseService importExportBaseService = ImportExportBaseService.getInstance();
            switch (part.getContentType()) {
                case "application/x-zip-compressed":
                case "application/zip":
                    File fileToImport = File.createTempFile("import", ".zip");
                    try {
                        FileUtils.copyInputStreamToFile(part.getInputStream(), fileToImport);
                        ValidationResults results = importExportBaseService.validateImportFile(node.getSession(),
                                FileUtils.openInputStream(fileToImport), "application/zip", null);
                        if (!results.isSuccessful()) {
                            throw new DataFetchingException(results.toString());
                        }
                        importExportBaseService.importZip(node.getPath(), new FileSystemResource(fileToImport), rootBehaviour);
                    } finally {
                        FileUtils.deleteQuietly(fileToImport);
                    }
                    break;
                case "application/xml":
                case "text/xml":
                    importExportBaseService.importXML(node.getPath(), part.getInputStream(), rootBehaviour);
                    break;
                default:
                    throw new GqlJcrWrongInputException("Wrong file type");
            }
        } catch (IllegalStateException e) {
            // Re-wrap exception for exceeding zip file size
            Exception ex = (e.getMessage().contains("Zip file being extracted is too big")) ?
                    new FileSizeLimitExceededException(e.getMessage(), -1, SettingsBean.getInstance().getJahiaFileUploadMaxSize()) : e;
            throw new DataFetchingException(ex);
        } catch (Exception e) {
            throw new DataFetchingException(e);
        }
    }

    /**
     * The value according to the JCR node type
     *
     * @param jcrType     the jcr node type
     * @param option      the option of the property
     * @param value       the value to create
     * @param session     JCR session to be used for node retrieval
     * @param environment the execution content instance
     * @return Value
     * @throws RepositoryException                           in case of JCR-related errors
     * @throws IOException                                   in case of input stream errors
     * @throws FileUploadBase.FileSizeLimitExceededException in case of file upload errors
     */
    public static Value getValue(int jcrType, GqlJcrPropertyOption option, String value, JCRSessionWrapper session, DataFetchingEnvironment environment) throws RepositoryException, FileUploadBase.FileSizeLimitExceededException, IOException {
        ValueFactory valueFactory = session.getValueFactory();

        if (option == GqlJcrPropertyOption.ENCRYPTED && jcrType == PropertyType.STRING) {
            return valueFactory.createValue(EncryptionUtils.passwordBaseEncrypt(value), jcrType);
        } else if (option == GqlJcrPropertyOption.NOT_ZONED_DATE) {
            try {
                SimpleDateFormat defaultDateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

                Date date = defaultDateFormat.parse(value);

                return valueFactory.createValue(simpleDateFormat.format(date), jcrType);
            } catch (ParseException e) {
                throw new GqlJcrWrongInputException("Unable to parse the date value", e);
            }
        }

        switch (jcrType) {
            case PropertyType.REFERENCE:
                return valueFactory.createValue(getNodeFromPathOrId(session, value));
            case PropertyType.WEAKREFERENCE:
                return valueFactory.createValue(getNodeFromPathOrId(session, value), true);
            case PropertyType.BINARY:
                if (UploadHelper.isValidFileUpload(value, environment)) {
                    return valueFactory.createValue(valueFactory.createBinary(UploadHelper.getFileUpload(value, environment).getInputStream()));
                } else {
                    return valueFactory.createValue(value, jcrType);
                }
            default:
                return valueFactory.createValue(value, jcrType);
        }
    }
}
