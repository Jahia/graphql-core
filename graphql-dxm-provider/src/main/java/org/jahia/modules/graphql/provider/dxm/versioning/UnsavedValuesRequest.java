package org.jahia.modules.graphql.provider.dxm.versioning;

import org.apache.commons.fileupload.FileUploadBase;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrMutationSupport;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrPropertyOption;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrPropertyType;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.utils.LanguageCodeConverters;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The unsaved values of a {@code jcr(unsavedValues:)} argument, read into the strings of their JCR values. The read
 * follows the save of a form: a value takes its type and its option as {@code setPropertiesBatch} takes them, and a
 * reference takes the identifier of its target. A binary value is refused.
 */
final class UnsavedValuesRequest {

    /** A property that the form sets, or removes when its values are {@code null}. */
    static final class PropertyChange {
        final String name;
        final Locale language;
        final int type;
        final boolean multiple;
        final List<String> values;

        PropertyChange(String name, Locale language, int type, boolean multiple, List<String> values) {
            this.name = name;
            this.language = language;
            this.type = type;
            this.multiple = multiple;
            this.values = values;
        }
    }

    final String nodeIdentifier;
    final List<PropertyChange> changes;
    final List<String> mixinsToAdd;
    final List<String> mixinsToDelete;

    private UnsavedValuesRequest(String nodeIdentifier, List<PropertyChange> changes, List<String> mixinsToAdd,
                                 List<String> mixinsToDelete) {
        this.nodeIdentifier = nodeIdentifier;
        this.changes = changes;
        this.mixinsToAdd = mixinsToAdd;
        this.mixinsToDelete = mixinsToDelete;
    }

    /**
     * Reads the argument. The sets come before the removals, as the save of a form applies them.
     *
     * @param argument the value of the argument: a map of its fields, as GraphQL coerces an input object
     * @param session  a plain session of the {@code default} workspace, which resolves the node and the references
     * @return the request
     * @throws IllegalArgumentException when the argument names no readable node, or carries a value that its type refuses
     * @throws RepositoryException      when the repository fails
     */
    static UnsavedValuesRequest read(Map<?, ?> argument, JCRSessionWrapper session) throws RepositoryException {
        Object node = argument.get("node");
        if (node == null) {
            throw new IllegalArgumentException("unsavedValues names no node");
        }
        String nodeIdentifier = node(session, node.toString()).getIdentifier();
        List<PropertyChange> changes = new ArrayList<>();
        for (Map<?, ?> property : maps(argument.get("properties"))) {
            PropertyChange change = set(property, session);
            if (change != null) {
                changes.add(change);
            }
        }
        for (Map<?, ?> property : maps(argument.get("propertiesToDelete"))) {
            changes.add(new PropertyChange(name(property), language(property), PropertyType.UNDEFINED, false, null));
        }
        return new UnsavedValuesRequest(nodeIdentifier, changes, strings(argument.get("mixinsToAdd")),
                strings(argument.get("mixinsToDelete")));
    }

    private static JCRNodeWrapper node(JCRSessionWrapper session, String pathOrId) throws RepositoryException {
        try {
            return pathOrId.startsWith("/") ? session.getNode(JCRContentUtils.escapeNodePath(pathOrId))
                    : session.getNodeByIdentifier(pathOrId);
        } catch (PathNotFoundException | ItemNotFoundException e) {
            throw new IllegalArgumentException("The node of unsavedValues does not exist: " + pathOrId);
        }
    }

    /** A property with neither a value nor values changes nothing, as for {@code setPropertiesBatch}. */
    private static PropertyChange set(Map<?, ?> property, JCRSessionWrapper session) throws RepositoryException {
        String name = name(property);
        GqlJcrPropertyType type = enumValue(GqlJcrPropertyType.class, property.get("type"));
        int jcrType = type != null ? type.getValue() : PropertyType.STRING;
        if (jcrType == PropertyType.BINARY) {
            throw new IllegalArgumentException("unsavedValues takes no binary value: " + name);
        }
        GqlJcrPropertyOption option = enumValue(GqlJcrPropertyOption.class, property.get("option"));
        Object value = property.get("value");
        Object values = property.get("values");
        if (value != null) {
            return new PropertyChange(name, language(property), jcrType, false,
                    Collections.singletonList(convert(name, jcrType, option, value.toString(), session)));
        }
        if (values == null) {
            return null;
        }
        List<String> converted = new ArrayList<>();
        for (String one : strings(values)) {
            converted.add(convert(name, jcrType, option, one, session));
        }
        return new PropertyChange(name, language(property), jcrType, true, converted);
    }

    private static String convert(String name, int type, GqlJcrPropertyOption option, String value, JCRSessionWrapper session)
            throws RepositoryException {
        try {
            return GqlJcrMutationSupport.getValue(type, option, value, session, null).getString();
        } catch (ValueFormatException | GqlJcrWrongInputException e) {
            throw new IllegalArgumentException("unsavedValues has a value that is not a " + PropertyType.nameFromValue(type)
                    + ": " + name);
        } catch (FileUploadBase.FileSizeLimitExceededException | IOException e) {
            // Only a binary value reads a stream, and the read refuses a binary before it converts.
            throw new IllegalStateException(e);
        } catch (DataFetchingException e) {
            // The target of a reference, which the session cannot read.
            throw new IllegalArgumentException("unsavedValues has a reference to a node that does not exist: " + name);
        }
    }

    private static String name(Map<?, ?> property) {
        Object name = property.get("name");
        if (name == null) {
            throw new IllegalArgumentException("unsavedValues has a property with no name");
        }
        return name.toString();
    }

    private static Locale language(Map<?, ?> property) {
        Object language = property.get("language");
        return language != null ? LanguageCodeConverters.languageCodeToLocale(language.toString()) : null;
    }

    /** An enum of an input reaches the coerced variables as the Java constant, and a literal as its name. */
    private static <E extends Enum<E>> E enumValue(Class<E> type, Object value) {
        if (value == null) {
            return null;
        }
        String name = value instanceof Enum ? ((Enum<?>) value).name() : value.toString();
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unsavedValues has an unknown " + type.getSimpleName() + ": " + name);
        }
    }

    private static List<Map<?, ?>> maps(Object list) {
        List<Map<?, ?>> maps = new ArrayList<>();
        if (list instanceof Collection) {
            for (Object item : (Collection<?>) list) {
                if (item instanceof Map) {
                    maps.add((Map<?, ?>) item);
                }
            }
        }
        return maps;
    }

    private static List<String> strings(Object list) {
        List<String> strings = new ArrayList<>();
        if (list instanceof Collection) {
            for (Object item : (Collection<?>) list) {
                if (item != null) {
                    strings.add(item.toString());
                }
            }
        }
        return strings;
    }
}
