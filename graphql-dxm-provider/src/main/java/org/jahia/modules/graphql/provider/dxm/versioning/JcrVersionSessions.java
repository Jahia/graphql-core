package org.jahia.modules.graphql.provider.dxm.versioning;

import graphql.GraphQLContext;
import graphql.execution.AbortExecutionException;
import graphql.language.Argument;
import graphql.language.ArrayValue;
import graphql.language.EnumValue;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.IntValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.language.VariableReference;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Opens the version sessions of a GraphQL query that reads a version with {@code jcr(versionAt:)} or
 * {@code jcr(versionOperation:)}, or the unsaved values of a form with {@code jcr(unsavedValues:)}. A version session
 * shows each node of the {@code default} workspace at the state that the content-versioning module supplies, or the node
 * of a form with its unsaved values, and it refuses every write. Jahia 8.2.4 brings these sessions.
 *
 * <p>Core sets the version of a session when the session opens, and every field of a request reads through the same
 * current user sessions. The version therefore applies to the whole operation, and it is set when the operation starts.
 * A filter of the request, such as the security filter, may have opened a {@code default} session before. Core moves
 * that session aside when the version is set, and the next lookup opens a version session. Four rules follow:
 * <ul>
 * <li>The query selects the {@code jcr} field alone at its root.</li>
 * <li>A query that runs inside a render cannot read a version. The rest of the render would mix its current nodes
 * with the nodes of the version.</li>
 * <li>No other operation follows a version in its request, because the version stays set, and its sessions stay
 * open, until the end of the request.</li>
 * <li>A field that runs on another thread, such as a field annotated {@code @GraphQLAsync}, gets the version through
 * {@link #currentVersion()} and {@link #resumeVersion(Object)}.</li>
 * </ul>
 *
 * <p>graphql-core builds against Jahia 8.2.0. This class therefore reaches the API of the version sessions by
 * reflection, and the bundle imports {@code org.jahia.api.versioning} as optional. On an older Jahia, a query that asks
 * for a version fails.
 */
public final class JcrVersionSessions {

    public static final String VERSION_AT = "versionAt";
    public static final String VERSION_OPERATION = "versionOperation";
    public static final String UNSAVED_VALUES = "unsavedValues";

    private static final Logger logger = LoggerFactory.getLogger(JcrVersionSessions.class);

    private static final String REQUEST_ATTRIBUTE = JcrVersionSessions.class.getName() + ".opened";
    private static final String OPENED_KEY = JcrVersionSessions.class.getName() + ".opened";
    private static final String ERROR_KEY = JcrVersionSessions.class.getName() + ".error";

    private static final JcrVersionSessions INSTANCE = new JcrVersionSessions(ReflectiveVersionApi.load());

    private final VersionApi api;
    private final ThreadLocal<Object> carriedVersion = new ThreadLocal<>();

    JcrVersionSessions(VersionApi api) {
        this.api = api;
    }

    /**
     * @return the instance that uses the version sessions of the running Jahia
     */
    public static JcrVersionSessions getInstance() {
        return INSTANCE;
    }

    /**
     * Opens the version sessions of an operation that asks for a version, when it starts. An operation that cannot read
     * its version gets an error from {@link #checkOperation(GraphQLContext)}.
     *
     * @param operation the operation that starts
     * @param fragments the fragments of the document, by name
     * @param variables the coerced variables of the operation
     * @param request   the HTTP request, or {@code null} when the operation runs without one
     * @param context   the GraphQL context of the operation
     */
    public void beginOperation(OperationDefinition operation, Map<String, FragmentDefinition> fragments,
                               Map<String, Object> variables, HttpServletRequest request, GraphQLContext context) {
        carriedVersion.remove();
        if (request != null && request.getAttribute(REQUEST_ATTRIBUTE) != null) {
            context.put(ERROR_KEY, "A request that read a version runs no other operation after it");
            return;
        }
        if (operation.getOperation() != OperationDefinition.Operation.QUERY) {
            return;
        }
        List<Field> rootFields = new ArrayList<>();
        collectFields(operation.getSelectionSet(), fragments, new HashSet<>(), rootFields);
        for (Field field : rootFields) {
            if ("jcr".equals(field.getName())) {
                Object versionAt = argument(field, VERSION_AT, variables);
                Object versionOperation = argument(field, VERSION_OPERATION, variables);
                Object unsavedValues = argument(field, UNSAVED_VALUES, variables);
                if (versionAt != null || versionOperation != null || unsavedValues != null) {
                    String error = openOrRefuse(rootFields.size(), argument(field, "workspace", variables),
                            new Version(versionAt, versionOperation, unsavedValues), request);
                    if (error != null) {
                        context.put(ERROR_KEY, error);
                    } else {
                        context.put(OPENED_KEY, Boolean.TRUE);
                    }
                    return;
                }
            }
        }
    }

    /**
     * Refuses an operation that cannot read the version it asks for, and an operation that follows a version in the same
     * request.
     *
     * @param context the GraphQL context of the operation
     * @throws AbortExecutionException when the operation must not run
     */
    public void checkOperation(GraphQLContext context) {
        String error = context.get(ERROR_KEY);
        if (error != null) {
            throw new AbortExecutionException(error);
        }
    }

    /**
     * @param context the GraphQL context of the operation
     * @return {@code true} when {@link #beginOperation} opened the version sessions of the operation
     */
    public boolean isOpened(GraphQLContext context) {
        return Boolean.TRUE.equals(context.get(OPENED_KEY));
    }

    /**
     * @param session a session of the current user
     * @return {@code true} when the session is a version session
     */
    public boolean isVersionSession(JCRSessionWrapper session) {
        return api != null && api.isVersionSession(session);
    }

    /**
     * Returns the version that the operation on this thread reads, so that another thread can read it too. A version
     * that the end of an earlier request cleared in core is not returned.
     *
     * @return the version, or {@code null} when the operation reads none
     */
    public Object currentVersion() {
        Object version = carriedVersion.get();
        return version != null && api != null && api.isVersionSessionRequested() ? version : null;
    }

    /**
     * Opens the version sessions of an operation on another thread, after the current user of that thread is set. The
     * thread ends with {@code resumeVersion(null)}.
     *
     * @param version the version that {@link #currentVersion()} returned on the thread of the operation, or {@code null}
     *                to clear it
     */
    public void resumeVersion(Object version) {
        if (api == null) {
            return;
        }
        api.setCurrentVersionSelector(version);
        if (version != null) {
            carriedVersion.set(version);
        } else {
            carriedVersion.remove();
        }
    }

    private String openOrRefuse(int rootFieldCount, Object workspace, Version version, HttpServletRequest request) {
        try {
            return open(rootFieldCount, workspace, version, request);
        } catch (RuntimeException e) {
            logger.warn("The version of a GraphQL query could not open", e);
            api.setCurrentVersionSelector(null);
            carriedVersion.remove();
            return "The version of this query could not open";
        }
    }

    private String open(int rootFieldCount, Object workspace, Version version, HttpServletRequest request) {
        String refusal = refusalOfRequest(rootFieldCount, workspace, version, request);
        if (refusal != null) {
            return refusal;
        }
        Object selector = null;
        if (version.at != null) {
            Long epochMillis = toLong(version.at);
            if (epochMillis == null) {
                return "versionAt is not a date in milliseconds since the epoch: " + version.at;
            }
            selector = api.atInstant(Instant.ofEpochMilli(epochMillis));
        } else if (version.operation != null) {
            try {
                selector = api.atOperation(UUID.fromString(version.operation.toString()));
            } catch (IllegalArgumentException e) {
                return "versionOperation is not a UUID: " + version.operation;
            }
        }
        if (api.isVersionSessionRequested()) {
            return "This request already shows a version";
        }
        // The render keeps its current nodes, while its later lookups would read the version.
        if (api.isRendering()) {
            return "A query that runs inside a render cannot read a version";
        }
        if (version.unsavedValues != null) {
            if (!(version.unsavedValues instanceof Map)) {
                return "unsavedValues is not an object";
            }
            try {
                // Before the version sessions open: the plain session of the user resolves the node and the references.
                selector = api.withUnsavedValues(UnsavedValuesRequest.read((Map<?, ?>) version.unsavedValues, api.getEditSession()));
            } catch (UnsavedValuesRequest.Refusal e) {
                return e.getMessage();
            } catch (RepositoryException e) {
                throw new IllegalStateException(e);
            }
        }
        api.setCurrentVersionSelector(selector);
        // After the version sessions open, so that a session that the check opens is one of them.
        // A date needs no check, because a node with no version at a date shows its current content, and core shows
        // unsaved values itself.
        if (version.operation != null && !api.canShowVersion(selector)) {
            api.setCurrentVersionSelector(null);
            return "No version that you can read carries the operation " + version.operation;
        }
        request.setAttribute(REQUEST_ATTRIBUTE, Boolean.TRUE);
        carriedVersion.set(selector);
        return null;
    }

    private String refusalOfRequest(int rootFieldCount, Object workspace, Version version, HttpServletRequest request) {
        if (rootFieldCount != 1) {
            return "A query that reads a version selects the jcr field alone at its root";
        }
        if (version.at != null && version.operation != null) {
            return "A query reads a version with versionAt or with versionOperation, not with both";
        }
        if (version.unsavedValues != null && (version.at != null || version.operation != null)) {
            return "A query reads unsavedValues without versionAt and without versionOperation";
        }
        if (workspace != null && NodeQueryExtensions.Workspace.fromName(workspace.toString()) != NodeQueryExtensions.Workspace.EDIT) {
            return "A query reads a version in the EDIT workspace only";
        }
        if (api == null) {
            return "Reading a version needs Jahia 8.2.4 or later";
        }
        if (version.unsavedValues != null && !api.supportsUnsavedValues()) {
            return "Reading unsaved values needs the UnsavedValues API of the core, which this Jahia lacks";
        }
        if (request == null) {
            return "Reading a version needs an HTTP request";
        }
        JahiaUser user = api.getCurrentUser();
        if (JahiaUserManagerService.isGuest(user)) {
            return "A guest cannot read a version";
        }
        return null;
    }

    private static Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            // The Long scalar of the schema also accepts a string literal.
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void collectFields(SelectionSet selectionSet, Map<String, FragmentDefinition> fragments,
                                      Set<String> spreadFragments, List<Field> fields) {
        for (Selection<?> selection : selectionSet.getSelections()) {
            if (selection instanceof Field) {
                fields.add((Field) selection);
            } else if (selection instanceof InlineFragment) {
                collectFields(((InlineFragment) selection).getSelectionSet(), fragments, spreadFragments, fields);
            } else if (selection instanceof FragmentSpread) {
                // Each fragment counts once, as for graphql-java, so a nest of spreads stays linear.
                String name = ((FragmentSpread) selection).getName();
                FragmentDefinition fragment = fragments.get(name);
                if (fragment != null && spreadFragments.add(name)) {
                    collectFields(fragment.getSelectionSet(), fragments, spreadFragments, fields);
                }
            }
        }
    }

    private static Object argument(Field field, String name, Map<String, Object> variables) {
        for (Argument argument : field.getArguments()) {
            if (argument.getName().equals(name)) {
                return value(argument.getValue(), variables);
            }
        }
        return null;
    }

    private static Object value(Value<?> value, Map<String, Object> variables) {
        if (value instanceof VariableReference) {
            return variables.get(((VariableReference) value).getName());
        }
        if (value instanceof IntValue) {
            return ((IntValue) value).getValue().longValue();
        }
        if (value instanceof StringValue) {
            return ((StringValue) value).getValue();
        }
        if (value instanceof EnumValue) {
            return ((EnumValue) value).getName();
        }
        if (value instanceof ObjectValue) {
            // The input object of unsavedValues, written in the query.
            Map<String, Object> fields = new LinkedHashMap<>();
            for (ObjectField objectField : ((ObjectValue) value).getObjectFields()) {
                fields.put(objectField.getName(), value(objectField.getValue(), variables));
            }
            return fields;
        }
        if (value instanceof ArrayValue) {
            List<Object> items = new ArrayList<>();
            for (Value<?> item : ((ArrayValue) value).getValues()) {
                items.add(value(item, variables));
            }
            return items;
        }
        // NullValue, and a value that no version argument takes.
        return null;
    }

    /** The arguments of the jcr field that ask for a version, of which a query sends one. */
    private static final class Version {
        private final Object at;
        private final Object operation;
        private final Object unsavedValues;

        private Version(Object at, Object operation, Object unsavedValues) {
            this.at = at;
            this.operation = operation;
            this.unsavedValues = unsavedValues;
        }
    }

    /**
     * The calls of the version sessions of Jahia 8.2.4 that this class makes.
     */
    interface VersionApi {

        Object atInstant(Instant instant);

        Object atOperation(UUID operationId);

        boolean supportsUnsavedValues();

        Object withUnsavedValues(UnsavedValuesRequest values);

        JCRSessionWrapper getEditSession() throws RepositoryException;

        void setCurrentVersionSelector(Object selector);

        boolean canShowVersion(Object selector);

        boolean isVersionSessionRequested();

        boolean isVersionSession(JCRSessionWrapper session);

        boolean isRendering();

        JahiaUser getCurrentUser();
    }

    /**
     * Reaches the version sessions of Jahia 8.2.4 by reflection.
     */
    private static final class ReflectiveVersionApi implements VersionApi {

        private static final String SELECTOR_CLASS = "org.jahia.api.versioning.SessionVersionSelector";
        private static final String UNSAVED_VALUES_CLASS = "org.jahia.api.versioning.UnsavedValues";

        private final Method atInstant;
        private final Method atOperation;
        private final Method setCurrentVersionSelector;
        private final Method canShowVersion;
        private final Method isVersionSessionRequested;
        private final Method isVersionSession;
        private final Method getCurrentRenderMode;
        private final UnsavedValuesApi unsavedValuesApi;

        private ReflectiveVersionApi(Class<?> selectorClass) throws NoSuchMethodException {
            atInstant = selectorClass.getMethod("atInstant", Instant.class);
            atOperation = selectorClass.getMethod("atOperation", UUID.class);
            setCurrentVersionSelector = JCRSessionFactory.class.getMethod("setCurrentVersionSelector", selectorClass);
            canShowVersion = JCRSessionFactory.class.getMethod("canShowVersion", selectorClass);
            isVersionSessionRequested = JCRSessionFactory.class.getMethod("isVersionSessionRequested", String.class);
            isVersionSession = JCRSessionWrapper.class.getMethod("isVersionSession");
            getCurrentRenderMode = JCRSessionFactory.class.getMethod("getCurrentRenderMode");
            unsavedValuesApi = UnsavedValuesApi.load(selectorClass);
        }

        static VersionApi load() {
            try {
                return new ReflectiveVersionApi(Class.forName(SELECTOR_CLASS, false, JcrVersionSessions.class.getClassLoader()));
            } catch (ClassNotFoundException | NoSuchMethodException | LinkageError e) {
                logger.info("This Jahia has no version sessions, so a GraphQL query cannot read a version");
                logger.debug("Version sessions not found", e);
                return null;
            }
        }

        @Override
        public Object atInstant(Instant instant) {
            return invoke(atInstant, null, instant);
        }

        @Override
        public Object atOperation(UUID operationId) {
            return invoke(atOperation, null, operationId);
        }

        @Override
        public boolean supportsUnsavedValues() {
            return unsavedValuesApi != null;
        }

        @Override
        public Object withUnsavedValues(UnsavedValuesRequest values) {
            return unsavedValuesApi.selector(values);
        }

        @Override
        public JCRSessionWrapper getEditSession() throws RepositoryException {
            return JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE);
        }

        @Override
        public void setCurrentVersionSelector(Object selector) {
            invoke(setCurrentVersionSelector, JCRSessionFactory.getInstance(), selector);
        }

        @Override
        public boolean canShowVersion(Object selector) {
            return (Boolean) invoke(canShowVersion, JCRSessionFactory.getInstance(), selector);
        }

        @Override
        public boolean isVersionSessionRequested() {
            return (Boolean) invoke(isVersionSessionRequested, JCRSessionFactory.getInstance(), (Object) null);
        }

        @Override
        public boolean isVersionSession(JCRSessionWrapper session) {
            return (Boolean) invoke(isVersionSession, session);
        }

        @Override
        public boolean isRendering() {
            // The render servlet sets the render mode of its thread, and the end of the request clears it.
            return invoke(getCurrentRenderMode, JCRSessionFactory.getInstance()) != null;
        }

        @Override
        public JahiaUser getCurrentUser() {
            return JCRSessionFactory.getInstance().getCurrentUser();
        }

        private static Object invoke(Method method, Object target, Object... arguments) {
            try {
                return method.invoke(target, arguments);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new IllegalStateException(cause);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        /**
         * Builds a selector of unsaved values by reflection. A Jahia 8.2.4 that predates the unsaved values has none, and
         * still reads a version.
         */
        private static final class UnsavedValuesApi {

            private final Method builder;
            private final Method setProperty;
            private final Method removeProperty;
            private final Method addMixin;
            private final Method removeMixin;
            private final Method build;
            private final Method withUnsavedValues;

            private UnsavedValuesApi(Class<?> selectorClass, Class<?> valuesClass) throws NoSuchMethodException {
                builder = valuesClass.getMethod("builder", String.class);
                Class<?> builderClass = builder.getReturnType();
                setProperty = builderClass.getMethod("setProperty", String.class, Locale.class, int.class, boolean.class, List.class);
                removeProperty = builderClass.getMethod("removeProperty", String.class, Locale.class);
                addMixin = builderClass.getMethod("addMixin", String.class);
                removeMixin = builderClass.getMethod("removeMixin", String.class);
                build = builderClass.getMethod("build");
                withUnsavedValues = selectorClass.getMethod("withUnsavedValues", valuesClass);
            }

            static UnsavedValuesApi load(Class<?> selectorClass) {
                try {
                    return new UnsavedValuesApi(selectorClass,
                            Class.forName(UNSAVED_VALUES_CLASS, false, JcrVersionSessions.class.getClassLoader()));
                } catch (ClassNotFoundException | NoSuchMethodException | LinkageError e) {
                    logger.info("This Jahia has no unsaved values, so a GraphQL query cannot read them");
                    logger.debug("Unsaved values not found", e);
                    return null;
                }
            }

            /** The mixins it removes come before the mixins it adds, as the save of a form switches them. */
            Object selector(UnsavedValuesRequest values) {
                Object valuesBuilder = invoke(builder, null, values.nodeIdentifier);
                for (UnsavedValuesRequest.PropertyChange change : values.changes) {
                    if (change.values != null) {
                        invoke(setProperty, valuesBuilder, change.name, change.language, change.type, change.multiple, change.values);
                    } else {
                        invoke(removeProperty, valuesBuilder, change.name, change.language);
                    }
                }
                for (String mixin : values.mixinsToDelete) {
                    invoke(removeMixin, valuesBuilder, mixin);
                }
                for (String mixin : values.mixinsToAdd) {
                    invoke(addMixin, valuesBuilder, mixin);
                }
                return invoke(withUnsavedValues, null, invoke(build, valuesBuilder));
            }
        }
    }
}
