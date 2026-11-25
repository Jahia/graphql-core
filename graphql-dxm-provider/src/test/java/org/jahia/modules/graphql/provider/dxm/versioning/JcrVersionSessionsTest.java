package org.jahia.modules.graphql.provider.dxm.versioning;

import graphql.GraphQLContext;
import graphql.execution.AbortExecutionException;
import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.FragmentDefinition;
import graphql.language.OperationDefinition;
import graphql.parser.Parser;
import graphql.parser.ParserEnvironment;
import graphql.parser.ParserOptions;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrPropertyType;
import org.jahia.modules.graphql.provider.dxm.node.NodeQueryExtensions;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.usermanager.JahiaUser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the decision that {@link JcrVersionSessions} takes when an operation starts, with a fake of the version
 * sessions of core.
 */
public class JcrVersionSessionsTest {

    private static final String OPERATION_ID = "0b1f7d6c-2f7e-4b8e-9a3c-5d2e1f0a9b8c";
    private static final String NODE_ID = "5a1c3f4e-8d2b-4c6a-9e7f-1b2d3c4e5f6a";

    private FakeVersionApi api;
    private JcrVersionSessions versionSessions;
    private HttpServletRequest request;
    private GraphQLContext context;

    @Before
    public void setUp() throws RepositoryException {
        api = new FakeVersionApi();
        api.user = user("editor");
        api.editSession = editSession();
        versionSessions = new JcrVersionSessions(api);
        request = request();
        context = GraphQLContext.newContext().build();
    }

    @After
    public void tearDown() {
        versionSessions.resumeVersion(null);
    }

    @Test
    public void plainQueryOpensNoVersion() {
        begin("{ jcr { nodeByPath(path: \"/sites\") { name } } }", Collections.emptyMap());

        assertFalse(versionSessions.isOpened(context));
        assertNull(api.selector);
        assertNull(versionSessions.currentVersion());
        versionSessions.checkOperation(context);
    }

    @Test
    public void versionAtOpensTheVersionOfTheOperation() {
        begin("{ jcr(versionAt: 1700000000000) { nodeByPath(path: \"/sites\") { name } } }", Collections.emptyMap());

        assertTrue(versionSessions.isOpened(context));
        assertEquals("at:" + Instant.ofEpochMilli(1700000000000L), api.selector);
        assertEquals(api.selector, versionSessions.currentVersion());
        versionSessions.checkOperation(context);
    }

    @Test
    public void versionAtReadsAVariable() {
        begin("query Q($at: Long) { jcr(versionAt: $at) { workspace } }", Collections.singletonMap("at", 1000L));

        assertTrue(versionSessions.isOpened(context));
        assertEquals("at:" + Instant.ofEpochMilli(1000L), api.selector);
    }

    @Test
    public void versionAtReadsAStringLiteral() {
        // The Long scalar of the schema accepts a string literal.
        begin("{ jcr(versionAt: \"1000\") { workspace } }", Collections.emptyMap());

        assertTrue(versionSessions.isOpened(context));
        assertEquals("at:" + Instant.ofEpochMilli(1000L), api.selector);
    }

    @Test
    public void versionAtThatIsNotANumberFails() {
        begin("{ jcr(versionAt: \"yesterday\") { workspace } }", Collections.emptyMap());

        assertRefused("versionAt is not a date in milliseconds since the epoch: yesterday");
        assertNull(api.selector);
    }

    @Test
    public void nullVersionAtOpensNoVersion() {
        begin("query Q($at: Long) { jcr(versionAt: $at) { workspace } other }", Collections.singletonMap("at", null));

        assertFalse(versionSessions.isOpened(context));
        assertNull(api.selector);
        versionSessions.checkOperation(context);
    }

    @Test
    public void versionOperationOpensAnOperationThatTheUserCanSee() {
        begin("{ jcr(versionOperation: \"" + OPERATION_ID + "\") { workspace } }", Collections.emptyMap());

        assertTrue(versionSessions.isOpened(context));
        assertEquals("op:" + OPERATION_ID, api.selector);
        assertEquals(1, api.checks);
    }

    @Test
    public void unknownOperationFailsClearsTheVersionAndLetsTheNextOperationRun() {
        api.canShow = false;

        begin("{ jcr(versionOperation: \"" + OPERATION_ID + "\") { workspace } }", Collections.emptyMap());

        assertRefused("No version that you can read carries the operation " + OPERATION_ID);
        assertNull(api.selector);
        assertNull(versionSessions.currentVersion());

        context = GraphQLContext.newContext().build();
        begin("{ jcr { workspace } }", Collections.emptyMap());

        versionSessions.checkOperation(context);
    }

    @Test
    public void operationThatIsNotAUuidFails() {
        begin("{ jcr(versionOperation: \"not-a-uuid\") { workspace } }", Collections.emptyMap());

        assertRefused("versionOperation is not a UUID: not-a-uuid");
    }

    @Test
    public void bothArgumentsFail() {
        begin("{ jcr(versionAt: 1, versionOperation: \"" + OPERATION_ID + "\") { workspace } }", Collections.emptyMap());

        assertRefused("A query reads a version with versionAt or with versionOperation, not with both");
    }

    @Test
    public void liveWorkspaceFails() {
        begin("{ jcr(workspace: LIVE, versionAt: 1) { workspace } }", Collections.emptyMap());

        assertRefused("A query reads a version in the EDIT workspace only");
    }

    @Test
    public void liveWorkspaceInAVariableFails() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("ws", NodeQueryExtensions.Workspace.LIVE);
        begin("query Q($ws: Workspace) { jcr(workspace: $ws, versionAt: 1) { workspace } }", variables);

        assertRefused("A query reads a version in the EDIT workspace only");
    }

    @Test
    public void anotherRootFieldFails() {
        begin("{ jcr(versionAt: 1) { workspace } currentUser { name } }", Collections.emptyMap());

        assertRefused("A query that reads a version selects the jcr field alone at its root");
    }

    @Test
    public void rootFieldsOfAFragmentCount() {
        begin("query { ...Root } fragment Root on Query { jcr(versionAt: 1) { workspace } currentUser { name } }",
                Collections.emptyMap());

        assertRefused("A query that reads a version selects the jcr field alone at its root");
    }

    @Test
    public void versionInAFragmentOpensTheVersion() {
        begin("query { ... on Query { ...Root } } fragment Root on Query { jcr(versionAt: 1) { workspace } }",
                Collections.emptyMap());

        assertTrue(versionSessions.isOpened(context));
    }

    @Test(timeout = 5000)
    public void aNestOfFragmentSpreadsIsReadOnce() {
        // Each fragment spreads the next one twice, so an expansion of every spread would make 2^40 calls.
        StringBuilder document = new StringBuilder("query { jcr(versionAt: 1) { workspace } ...F0 }");
        for (int i = 0; i < 40; i++) {
            document.append(" fragment F").append(i).append(" on Query { ...F").append(i + 1).append(" ...F").append(i + 1).append(" }");
        }
        document.append(" fragment F40 on Query { __typename }");

        begin(document.toString(), Collections.emptyMap());

        assertRefused("A query that reads a version selects the jcr field alone at its root");
    }

    @Test
    public void jahiaWithoutVersionSessionsFails() {
        versionSessions = new JcrVersionSessions(null);

        begin("{ jcr(versionAt: 1) { workspace } }", Collections.emptyMap());

        assertRefused("Reading a version needs Jahia 8.2.4 or later");
    }

    @Test
    public void operationWithoutARequestFails() {
        request = null;

        begin("{ jcr(versionAt: 1) { workspace } }", Collections.emptyMap());

        assertRefused("Reading a version needs an HTTP request");
    }

    @Test
    public void guestFails() {
        api.user = user("guest");

        begin("{ jcr(versionAt: 1) { workspace } }", Collections.emptyMap());

        assertRefused("A guest cannot read a version");
        assertNull(api.selector);
    }

    @Test
    public void versionOfAnotherCallerFails() {
        api.requested = true;

        begin("{ jcr(versionAt: 1) { workspace } }", Collections.emptyMap());

        assertRefused("This request already shows a version");
        assertNull(api.selector);
    }

    @Test
    public void queryInsideARenderFails() {
        api.rendering = true;

        begin("{ jcr(versionAt: 1) { workspace } }", Collections.emptyMap());

        assertRefused("A query that runs inside a render cannot read a version");
        assertNull(api.selector);
    }

    @Test
    public void aFailureOfCoreRefusesTheVersionAndClearsIt() {
        api.failOnCheck = true;

        begin("{ jcr(versionOperation: \"" + OPERATION_ID + "\") { workspace } }", Collections.emptyMap());

        assertRefused("The version of this query could not open");
        assertNull(api.selector);
        assertNull(versionSessions.currentVersion());
    }

    @Test
    public void operationAfterAVersionInTheSameRequestFails() {
        begin("{ jcr(versionAt: 1) { workspace } }", Collections.emptyMap());
        assertTrue(versionSessions.isOpened(context));

        context = GraphQLContext.newContext().build();
        begin("mutation { jcr { mutateNode(pathOrId: \"/sites\") { delete } } }", Collections.emptyMap());

        assertRefused("A request that read a version runs no other operation after it");
        assertNull(versionSessions.currentVersion());
    }

    @Test
    public void operationOfAnotherRequestCarriesNoVersion() {
        begin("{ jcr(versionAt: 1) { workspace } }", Collections.emptyMap());
        assertEquals(api.selector, versionSessions.currentVersion());

        request = request();
        context = GraphQLContext.newContext().build();
        begin("{ jcr { workspace } }", Collections.emptyMap());

        assertNull(versionSessions.currentVersion());
        versionSessions.checkOperation(context);
    }

    @Test
    public void aVersionThatTheEndOfTheRequestClearedIsNotCarried() {
        begin("{ jcr(versionAt: 1) { workspace } }", Collections.emptyMap());
        assertEquals(api.selector, versionSessions.currentVersion());

        // The end of the request clears the version in core, and no operation starts on the thread after it.
        api.selector = null;

        assertNull(versionSessions.currentVersion());
    }

    @Test
    public void mutationOpensNoVersion() {
        begin("mutation { jcr(versionAt: 1) { workspace } }", Collections.emptyMap());

        assertFalse(versionSessions.isOpened(context));
        assertNull(api.selector);
    }

    @Test
    public void resumeVersionOpensAndClearsTheVersionOfAThread() {
        versionSessions.resumeVersion("at:carried");

        assertEquals("at:carried", api.selector);
        assertEquals("at:carried", versionSessions.currentVersion());

        versionSessions.resumeVersion(null);

        assertNull(api.selector);
        assertNull(versionSessions.currentVersion());
    }

    @Test
    public void isVersionSessionAsksCore() {
        JCRSessionWrapper versionSession = mock(JCRSessionWrapper.class);
        JCRSessionWrapper plainSession = mock(JCRSessionWrapper.class);
        api.versionSessions.add(versionSession);

        assertTrue(versionSessions.isVersionSession(versionSession));
        assertFalse(versionSessions.isVersionSession(plainSession));
        assertFalse(new JcrVersionSessions(null).isVersionSession(versionSession));
    }

    @Test
    public void unsavedValuesOpenTheValuesOfTheFormFromAVariable() throws RepositoryException {
        Map<String, Object> values = new HashMap<>();
        values.put("node", NODE_ID);
        values.put("properties", Arrays.asList(
                property("text", "en", null, "typed", null),
                property("j:keywords", "en", GqlJcrPropertyType.STRING, null, Arrays.asList("one", "two"))));
        values.put("propertiesToDelete", Collections.singletonList(deleted("jcr:title", "fr")));
        values.put("mixinsToAdd", Collections.singletonList("jmix:keywords"));
        values.put("mixinsToDelete", Collections.singletonList("jmix:description"));

        begin("query Q($values: InputJCRUnsavedValues) { jcr(unsavedValues: $values) { workspace } }",
                Collections.singletonMap("values", values));

        assertTrue(versionSessions.isOpened(context));
        assertEquals("unsaved:" + NODE_ID, api.selector);
        assertEquals(api.selector, versionSessions.currentVersion());
        assertEquals("core shows unsaved values without a check", 0, api.checks);
        UnsavedValuesRequest request = api.unsavedValues;
        assertEquals(3, request.changes.size());
        assertChange(request.changes.get(0), "text", Locale.ENGLISH, PropertyType.STRING, false, Collections.singletonList("typed"));
        assertChange(request.changes.get(1), "j:keywords", Locale.ENGLISH, PropertyType.STRING, true, Arrays.asList("one", "two"));
        assertChange(request.changes.get(2), "jcr:title", Locale.FRENCH, PropertyType.UNDEFINED, false, null);
        assertEquals(Collections.singletonList("jmix:keywords"), request.mixinsToAdd);
        assertEquals(Collections.singletonList("jmix:description"), request.mixinsToDelete);
        versionSessions.checkOperation(context);
    }

    @Test
    public void unsavedValuesReadALiteralThatNamesTheNodeByItsPath() throws RepositoryException {
        begin("{ jcr(unsavedValues: {node: \"/sites/a/text\", properties: [{name: \"count\", type: LONG, value: \"3\"}]}) { workspace } }",
                Collections.emptyMap());

        assertTrue(versionSessions.isOpened(context));
        assertEquals("unsaved:" + NODE_ID, api.selector);
        assertChange(api.unsavedValues.changes.get(0), "count", null, PropertyType.LONG, false, Collections.singletonList("3"));
    }

    @Test
    public void unsavedValuesWithAVersionFail() {
        begin("{ jcr(versionAt: 1000, unsavedValues: {node: \"" + NODE_ID + "\"}) { workspace } }", Collections.emptyMap());

        assertRefused("A query reads unsavedValues without versionAt and without versionOperation");
        assertNull(api.selector);
    }

    @Test
    public void unsavedValuesOfANodeThatDoesNotExistFail() {
        begin("{ jcr(unsavedValues: {node: \"missing\"}) { workspace } }", Collections.emptyMap());

        assertRefused("The node of unsavedValues does not exist: missing");
        assertNull(api.selector);
    }

    @Test
    public void aBinaryUnsavedValueFails() {
        begin("{ jcr(unsavedValues: {node: \"" + NODE_ID + "\", properties: [{name: \"data\", type: BINARY, value: \"x\"}]}) { workspace } }",
                Collections.emptyMap());

        assertRefused("unsavedValues takes no binary value: data");
        assertNull(api.selector);
    }

    @Test
    public void anUnsavedValueThatItsTypeRefusesFails() {
        begin("{ jcr(unsavedValues: {node: \"" + NODE_ID + "\", properties: [{name: \"count\", type: LONG, value: \"three\"}]}) { workspace } }",
                Collections.emptyMap());

        assertRefused("unsavedValues has a value that is not a Long: count");
        assertNull(api.selector);
    }

    @Test
    public void unsavedValuesInTheLiveWorkspaceFail() {
        begin("{ jcr(workspace: LIVE, unsavedValues: {node: \"" + NODE_ID + "\"}) { workspace } }", Collections.emptyMap());

        assertRefused("A query reads a version in the EDIT workspace only");
    }

    @Test
    public void unsavedValuesOnAJahiaWithoutThemFailAndAVersionStillOpens() {
        api.unsavedValuesSupported = false;

        begin("{ jcr(unsavedValues: {node: \"" + NODE_ID + "\"}) { workspace } }", Collections.emptyMap());

        assertRefused("Reading unsaved values needs a Jahia that supports them");
        assertNull(api.selector);
        context = GraphQLContext.newContext().build();
        request = request();
        begin("{ jcr(versionAt: 1000) { workspace } }", Collections.emptyMap());
        assertTrue(versionSessions.isOpened(context));
    }

    private static Map<String, Object> property(String name, String language, Object type, String value, List<String> values) {
        Map<String, Object> property = new HashMap<>();
        property.put("name", name);
        property.put("language", language);
        property.put("type", type);
        property.put("value", value);
        property.put("values", values);
        return property;
    }

    private static Map<String, Object> deleted(String name, String language) {
        Map<String, Object> property = new HashMap<>();
        property.put("name", name);
        property.put("language", language);
        return property;
    }

    private static void assertChange(UnsavedValuesRequest.PropertyChange change, String name, Locale language, int type,
                                     boolean multiple, List<String> values) {
        assertEquals(name, change.name);
        assertEquals(language, change.language);
        assertEquals(type, change.type);
        assertEquals(multiple, change.multiple);
        assertEquals(values, change.values);
    }

    /** A plain session that knows one node, by its identifier and by its path, and whose values take their string. */
    private static JCRSessionWrapper editSession() throws RepositoryException {
        JCRSessionWrapper session = mock(JCRSessionWrapper.class);
        JCRNodeWrapper node = mock(JCRNodeWrapper.class);
        when(node.getIdentifier()).thenReturn(NODE_ID);
        when(session.getNodeByIdentifier(anyString())).thenThrow(new ItemNotFoundException());
        doReturn(node).when(session).getNodeByIdentifier(NODE_ID);
        doReturn(node).when(session).getNode("/sites/a/text");
        ValueFactory valueFactory = mock(ValueFactory.class);
        when(valueFactory.createValue(anyString(), anyInt())).thenAnswer(invocation -> {
            String string = invocation.getArgument(0);
            if (invocation.<Integer>getArgument(1) == PropertyType.LONG && !string.matches("\\d+")) {
                throw new ValueFormatException("not a long");
            }
            Value value = mock(Value.class);
            when(value.getString()).thenReturn(string);
            return value;
        });
        when(session.getValueFactory()).thenReturn(valueFactory);
        return session;
    }

    private void begin(String query, Map<String, Object> variables) {
        Document document = Parser.parse(ParserEnvironment.newParserEnvironment().document(query)
                .parserOptions(ParserOptions.newParserOptions().maxTokens(100000).build()).build());
        OperationDefinition operation = null;
        Map<String, FragmentDefinition> fragments = new HashMap<>();
        for (Definition<?> definition : document.getDefinitions()) {
            if (definition instanceof OperationDefinition) {
                operation = (OperationDefinition) definition;
            } else if (definition instanceof FragmentDefinition) {
                fragments.put(((FragmentDefinition) definition).getName(), (FragmentDefinition) definition);
            }
        }
        versionSessions.beginOperation(operation, fragments, variables, request, context);
    }

    private void assertRefused(String message) {
        assertFalse(versionSessions.isOpened(context));
        try {
            versionSessions.checkOperation(context);
            fail("The operation should be refused");
        } catch (AbortExecutionException e) {
            assertEquals(message, e.getMessage());
        }
    }

    private static HttpServletRequest request() {
        Map<String, Object> attributes = new HashMap<>();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        return request;
    }

    private static JahiaUser user(String name) {
        JahiaUser user = mock(JahiaUser.class);
        when(user.getName()).thenReturn(name);
        when(user.getUsername()).thenReturn(name);
        return user;
    }

    /** A fake of the version sessions of core, which records the version of the thread. */
    private static final class FakeVersionApi implements JcrVersionSessions.VersionApi {

        private final Set<JCRSessionWrapper> versionSessions = new HashSet<>();
        private Object selector;
        private boolean canShow = true;
        private boolean failOnCheck;
        private boolean requested;
        private boolean rendering;
        private int checks;
        private JahiaUser user;
        private boolean unsavedValuesSupported = true;
        private UnsavedValuesRequest unsavedValues;
        private JCRSessionWrapper editSession;

        @Override
        public Object atInstant(Instant instant) {
            return "at:" + instant;
        }

        @Override
        public Object atOperation(UUID operationId) {
            return "op:" + operationId;
        }

        @Override
        public boolean supportsUnsavedValues() {
            return unsavedValuesSupported;
        }

        @Override
        public Object withUnsavedValues(UnsavedValuesRequest values) {
            unsavedValues = values;
            return "unsaved:" + values.nodeIdentifier;
        }

        @Override
        public JCRSessionWrapper getEditSession() {
            return editSession;
        }

        @Override
        public void setCurrentVersionSelector(Object selector) {
            this.selector = selector;
        }

        @Override
        public boolean canShowVersion(Object selector) {
            checks++;
            if (failOnCheck) {
                throw new IllegalStateException("core failed");
            }
            return canShow;
        }

        @Override
        public boolean isVersionSessionRequested() {
            // Another caller set a version, or this class did.
            return requested || selector != null;
        }

        @Override
        public boolean isVersionSession(JCRSessionWrapper session) {
            return versionSessions.contains(session);
        }

        @Override
        public boolean isRendering() {
            return rendering;
        }

        @Override
        public JahiaUser getCurrentUser() {
            return user;
        }
    }
}
