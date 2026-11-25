package org.jahia.modules.graphql.provider.dxm.versioning;

import org.apache.commons.fileupload.FileUploadBase;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrMutationSupport;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrPropertyOption;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrPropertyType;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.utils.LanguageCodeConverters;

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

    /**
     * The most values that one argument carries: the values of the properties that it sets, the properties that it
     * removes and the mixins that it changes. A value can cost an encryption or a lookup before the query starts.
     */
    static final int MAX_VALUES = 1000;

    /** A refusal of the argument, whose message the query returns. */
    static final class Refusal extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Refusal(String message) {
            super(message);
        }
    }

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
     * @param argument the value of the argument, a map of its fields. A variable gives the map that GraphQL coerces, with
     *                 each enum as its Java constant. A literal of the query gives the map that {@link JcrVersionSessions}
     *                 builds, with each enum as its name and a single value where the schema declares a list.
     * @param session  a plain session of the {@code default} workspace, which resolves the node and the references
     * @return the request
     * @throws Refusal             when the argument names no readable node, carries too many values, or carries a value
     *                             that its type refuses
     * @throws RepositoryException when the repository fails
     */
    static UnsavedValuesRequest read(Map<?, ?> argument, JCRSessionWrapper session) throws RepositoryException {
        Object node = argument.get("node");
        if (node.toString().trim().isEmpty()) {
            throw new Refusal("unsavedValues names no node");
        }
        List<Map<?, ?>> properties = maps(argument.get("properties"));
        List<Map<?, ?>> propertiesToDelete = maps(argument.get("propertiesToDelete"));
        List<String> mixinsToAdd = strings(argument.get("mixinsToAdd"));
        List<String> mixinsToDelete = strings(argument.get("mixinsToDelete"));
        // Counted before any conversion, so that a large argument costs no encryption and no lookup.
        int count = propertiesToDelete.size() + mixinsToAdd.size() + mixinsToDelete.size();
        for (Map<?, ?> property : properties) {
            count += Math.max(1, strings(property.get("values")).size());
        }
        if (count > MAX_VALUES) {
            throw new Refusal("unsavedValues carries more than " + MAX_VALUES + " values");
        }
        JCRNodeWrapper formNode = node(session, node.toString());
        String path = formNode.getPath();
        // Core shows these nodes as they are stored: the templates under /modules feed caches that every user shares.
        if (path.startsWith("/jcr:system") || path.equals("/modules") || path.startsWith("/modules/")) {
            throw new Refusal("unsavedValues shows no node under /modules or /jcr:system: " + path);
        }
        String nodeIdentifier = formNode.getIdentifier();
        List<PropertyChange> changes = new ArrayList<>();
        for (Map<?, ?> property : properties) {
            PropertyChange change = set(property, session);
            if (change != null) {
                changes.add(change);
            }
        }
        for (Map<?, ?> property : propertiesToDelete) {
            changes.add(new PropertyChange(name(property), language(property), PropertyType.UNDEFINED, false, null));
        }
        return new UnsavedValuesRequest(nodeIdentifier, changes, mixinsToAdd, mixinsToDelete);
    }

    /** A node that the lookup cannot read, for any reason of the repository, does not exist for the user. */
    private static JCRNodeWrapper node(JCRSessionWrapper session, String pathOrId) {
        try {
            return GqlJcrMutationSupport.getNodeFromPathOrId(session, pathOrId);
        } catch (DataFetchingException e) {
            throw new Refusal("The node of unsavedValues does not exist: " + pathOrId);
        }
    }

    /** A property with neither a value nor values changes nothing, as for {@code setPropertiesBatch}. */
    private static PropertyChange set(Map<?, ?> property, JCRSessionWrapper session) throws RepositoryException {
        String name = name(property);
        Object value = property.get("value");
        Object values = property.get("values");
        if (value == null && values == null) {
            return null;
        }
        GqlJcrPropertyType type = enumValue(GqlJcrPropertyType.class, property.get("type"));
        int jcrType = type != null ? type.getValue() : PropertyType.STRING;
        if (jcrType == PropertyType.BINARY) {
            throw new Refusal("unsavedValues takes no binary value: " + name);
        }
        GqlJcrPropertyOption option = enumValue(GqlJcrPropertyOption.class, property.get("option"));
        if (value != null) {
            return new PropertyChange(name, language(property), jcrType, false,
                    Collections.singletonList(convert(name, jcrType, option, value.toString(), session)));
        }
        List<String> converted = new ArrayList<>();
        for (String one : strings(values)) {
            converted.add(convert(name, jcrType, option, one, session));
        }
        return new PropertyChange(name, language(property), jcrType, true, converted);
    }

    private static String convert(String name, int type, GqlJcrPropertyOption option, String value, JCRSessionWrapper session)
            throws RepositoryException {
        if ((type == PropertyType.REFERENCE || type == PropertyType.WEAKREFERENCE) && value.isEmpty()) {
            throw new Refusal("unsavedValues has an empty reference: " + name);
        }
        try {
            return GqlJcrMutationSupport.getValue(type, option, value, session, null).getString();
        } catch (ValueFormatException | GqlJcrWrongInputException e) {
            throw new Refusal("unsavedValues has a value that is not a " + PropertyType.nameFromValue(type) + ": " + name);
        } catch (FileUploadBase.FileSizeLimitExceededException | IOException e) {
            // Only a binary value reads a stream, and the read refuses a binary before it converts.
            throw new IllegalStateException(e);
        } catch (DataFetchingException e) {
            // The target of a reference, which the session cannot read.
            throw new Refusal("unsavedValues has a reference to a node that does not exist: " + name);
        }
    }

    private static String name(Map<?, ?> property) {
        return property.get("name").toString();
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
        return Enum.valueOf(type, value instanceof Enum ? ((Enum<?>) value).name() : value.toString());
    }

    /** A single value where the schema declares a list counts as a list of one, as GraphQL coerces it. */
    private static List<?> list(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        return value instanceof Collection ? new ArrayList<>((Collection<?>) value) : Collections.singletonList(value);
    }

    private static List<Map<?, ?>> maps(Object value) {
        List<Map<?, ?>> maps = new ArrayList<>();
        for (Object item : list(value)) {
            if (item instanceof Map) {
                maps.add((Map<?, ?>) item);
            }
        }
        return maps;
    }

    private static List<String> strings(Object value) {
        List<String> strings = new ArrayList<>();
        for (Object item : list(value)) {
            if (item != null) {
                strings.add(item.toString());
            }
        }
        return strings;
    }
}
