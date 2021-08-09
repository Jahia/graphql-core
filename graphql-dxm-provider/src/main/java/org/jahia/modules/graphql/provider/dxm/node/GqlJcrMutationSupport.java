/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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

import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.fileupload.FileUploadBase;
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
            HashMap<String, JCRSessionWrapper> sessions = new HashMap<>();
            List<JCRPropertyWrapper> result = new ArrayList<>();
            for (GqlJcrPropertyInput property : properties) {
                JCRNodeWrapper localizedNode = NodeHelper.getNodeInLanguage(node, property.getLanguage());
                JCRSessionWrapper session = localizedNode.getSession();
                sessions.put(property.getLanguage(), session);

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

            for (JCRSessionWrapper session : sessions.values()) {
                session.validate();
            }

            return result;
        } catch (RepositoryException | FileUploadBase.FileSizeLimitExceededException | IOException e) {
            throw NodeMutationConstraintViolationHandler.transformException(e);
        }
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
     * Import file upload content under a node.
     *
     * @param partName    Name of the request part that contains desired file upload
     * @param node        Parent node to import content under
     * @param environment Data fetching environment
     */
    public static void importFileUpload(String partName, JCRNodeWrapper node, DataFetchingEnvironment environment) throws BaseGqlClientException {
        try {
            Part part = UploadHelper.getFileUpload(partName, environment);
            ImportExportBaseService importExportBaseService = ImportExportBaseService.getInstance();
            switch (part.getContentType()) {
                case "application/x-zip-compressed":
                case "application/zip":
                    File fileToImport = File.createTempFile("import", ".zip");
                    try {
                        FileUtils.copyInputStreamToFile(part.getInputStream(), fileToImport);
                        importExportBaseService.importZip(node.getPath(), new FileSystemResource(fileToImport), DocumentViewImportHandler.ROOT_BEHAVIOUR_RENAME);
                    } finally {
                        FileUtils.deleteQuietly(fileToImport);
                    }
                    break;
                case "text/xml":
                    importExportBaseService.importXML(node.getPath(), part.getInputStream(),
                            DocumentViewImportHandler.ROOT_BEHAVIOUR_RENAME);
                    break;
                default:
                    throw new GqlJcrWrongInputException("Wrong file type");
            }
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
