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
package org.jahia.modules.graphql.provider.dxm.instrumentation;

import graphql.analysis.QueryComplexityCalculator;
import graphql.analysis.QueryTraverser;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.AbortExecutionException;
import graphql.execution.CoercedVariables;
import graphql.language.Document;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link QueryCostCalculator}, the static analysis behind the query-cost guards.
 *
 * <p>The schema below mirrors the shape of the JCR schema that matters here: a node type that is recursive both
 * directly and through a Relay-style connection, so that a small document can be made arbitrarily deep.
 */
public class QueryCostCalculatorTest {

    private static final String SDL = "type Query { jcr: JCRQuery currentUser: User } " +
            "type User { name: String displayName: String } " +
            "type JCRQuery { nodeByPath(path: String): JCRNode } " +
            "type JCRNode { name: String uuid: String parent: JCRNode descendants: JCRNodeConnection } " +
            "type JCRNodeConnection { nodes: [JCRNode] } " +
            "type Mutation { jcr: JCRMutation } " +
            "input InputJCRNode { name: String children: [InputJCRNode] mixins: [String] } " +
            "type JCRMutation { addNodesBatch(nodes: [InputJCRNode]): [JCRNodeMutation] " +
            "  mutateNodes(pathsOrIds: [String]): [JCRNodeMutation] " +
            "  mutateVanityUrls(pathsOrIds: [String]!): [VanityUrlMutation]! " +
            "  mutateNode(pathOrId: String): JCRNodeMutation } "
            + "type JCRNodeMutation2 { x: String } " +
            "type VanityUrlMutation { uuid: String } " +
            "type ZipFileMutation { addToZip(pathsOrIds: [String]!): Boolean } " +
            "type JCRNodeMutation { uuid: String zip: ZipFileMutation " +
            "  addMixins(mixins: [String]): Boolean setValues(values: [String]): Boolean }";

    /** High enough that no test is cut short by the early exit. */
    private static final int NO_CEILING = Integer.MAX_VALUE;

    private static GraphQLSchema schema;

    @BeforeClass
    public static void setUpSchema() {
        schema = new SchemaGenerator().makeExecutableSchema(
                new SchemaParser().parse(SDL), RuntimeWiring.newRuntimeWiring().build());
    }

    private static QueryTraverser traverser(String query, CoercedVariables variables) {
        Document document = Parser.parse(query);
        return QueryTraverser.newQueryTraverser()
                .schema(schema)
                .document(document)
                .coercedVariables(variables)
                .build();
    }

    private static QueryCostCalculator.QueryCost cost(String query) {
        return QueryCostCalculator.calculate(traverser(query, CoercedVariables.emptyVariables()), NO_CEILING);
    }

    private static int complexity(String query) {
        return cost(query).getComplexity();
    }

    private static int depth(String query) {
        return cost(query).getDepth();
    }

    private static int batchSize(String query) {
        return cost(query).getBatchSize();
    }

    private static int batchSize(String query, CoercedVariables variables) {
        return QueryCostCalculator.calculate(traverser(query, variables), NO_CEILING).getBatchSize();
    }

    private static int expandedFields(String query, int ceiling) {
        return QueryCostCalculator.expandedFieldCount(schema, Parser.parse(query), null,
                CoercedVariables.emptyVariables(), ceiling);
    }

    /**
     * Builds an operation over {@code levels} fragments, each spreading the one below it twice under distinct aliases:
     * the document grows by one fragment per level while what it executes doubles, to 3 * 2^levels fields.
     */
    private static String twiceSpreadFragments(int levels) {
        StringBuilder query = new StringBuilder("fragment f0 on JCRNode { name } ");
        for (int i = 1; i <= levels; i++) {
            query.append("fragment f").append(i).append(" on JCRNode { x: parent { ...f").append(i - 1)
                    .append(" } y: parent { ...f").append(i - 1).append(" } } ");
        }
        return query.append("{ jcr { nodeByPath(path: \"/\") { ...f").append(levels).append(" } } }").toString();
    }

    /** Builds a mutation selecting {@code mutateNodes} under {@code aliasCount} aliases, each given {@code items} paths. */
    private static String aliasedMutateNodes(int aliasCount, int items) {
        String paths = IntStream.range(0, items)
                .mapToObj(i -> "\"/p" + i + "\"")
                .collect(Collectors.joining(", "));
        String selections = IntStream.range(0, aliasCount)
                .mapToObj(i -> "a" + i + ": mutateNodes(pathsOrIds: [" + paths + "]) { uuid }")
                .collect(Collectors.joining(" "));
        return "mutation { jcr { " + selections + " } }";
    }

    /** Builds {@code { a0:__typename a1:__typename ... }}, the shape of an alias amplification payload. */
    private static String aliasedTypenameQuery(int aliasCount) {
        return "{" + IntStream.range(0, aliasCount)
                .mapToObj(i -> "a" + i + ":__typename")
                .collect(Collectors.joining(" ")) + "}";
    }

    /** Wraps {@code inner} in the given number of nested {@code descendants { nodes { ... } }}, none paginated. */
    private static String nestedDescendants(int levels, String inner) {
        StringBuilder query = new StringBuilder(inner);
        for (int i = 0; i < levels; i++) {
            query.insert(0, "descendants { nodes { ").append(" } }");
        }
        return "{ jcr { nodeByPath(path: \"/\") { " + query + " } } }";
    }

    // --- complexity ---

    @Test
    public void shouldCountEveryAliasedTypenameField() {
        // Each alias is a separate selection that costs a permission check and, once denied, an error object in the
        // response, so each has to be charged.
        assertEquals(500, complexity(aliasedTypenameQuery(500)));
    }

    @Test
    public void graphqlJavaCalculatorScoresAliasedTypenameAsZero() {
        // Documents WHY this class exists rather than reusing graphql-java's calculator: that one exempts __typename
        // unconditionally (the check sits in a private method, so a custom FieldComplexityCalculator cannot restore
        // it), scoring the payload above as 0 and letting it pass any budget. Should graphql-java ever charge for
        // __typename, this test fails and the complexity check here could be dropped for the built-in one.
        int graphqlJavaComplexity = QueryComplexityCalculator.newCalculator()
                .fieldComplexityCalculator((env, childComplexity) -> 1 + childComplexity)
                .schema(schema)
                .document(Parser.parse(aliasedTypenameQuery(500)))
                .variables(CoercedVariables.emptyVariables())
                .build()
                .calculate();
        assertEquals(0, graphqlJavaComplexity);
    }

    @Test
    public void shouldCountNestedFieldsOnce() {
        assertEquals(4, complexity("{ jcr { nodeByPath(path: \"/\") { name uuid } } }"));
    }

    @Test
    public void shouldCountAliasesOfOrdinaryFieldsIndividually() {
        assertEquals(3, complexity("{ currentUser { a: name b: name } }"));
    }

    @Test
    public void shouldCountTypenameAlongsideOrdinaryFields() {
        // jcr + nodeByPath + name + __typename
        assertEquals(4, complexity("{ jcr { nodeByPath(path: \"/\") { name __typename } } }"));
    }

    @Test
    public void shouldCountFieldsOfEveryOperationRoot() {
        assertEquals(5, complexity("{ currentUser { name } jcr { nodeByPath(path: \"/\") { name } } }"));
    }

    @Test
    public void shouldCountFieldsReachedThroughAFragment() {
        assertEquals(4, complexity("{ jcr { nodeByPath(path: \"/\") { ...f } } } fragment f on JCRNode { name uuid }"));
    }

    @Test
    public void shouldNotCountFieldsExcludedBySkip() {
        // A skipped field never executes, so it costs nothing.
        assertEquals(1, complexity("{ currentUser { name @skip(if: true) } }"));
        assertEquals(2, complexity("{ currentUser { name @skip(if: false) } }"));
    }

    // --- depth ---

    @Test
    public void shouldReportTheLongestFieldPathAsDepth() {
        assertEquals(2, depth("{ currentUser { name } }"));
        assertEquals(3, depth("{ jcr { nodeByPath(path: \"/\") { name } } }"));
        // The deepest path wins over a shallower sibling.
        assertEquals(5, depth("{ jcr { nodeByPath(path: \"/\") { name parent { parent { name } } } } }"));
    }

    @Test
    public void shouldReportTheSameDepthAsGraphqlJava() {
        // The depth guard used to be graphql-java's MaxQueryDepthInstrumentation; a configured maximum only keeps its
        // meaning if this measures depth the same way it did.
        for (String query : new String[]{
                "{ currentUser { name } }",
                "{ jcr { nodeByPath(path: \"/\") { name parent { parent { name } } } } }",
                "{ jcr { nodeByPath(path: \"/\") { ...f } } } fragment f on JCRNode { parent { name } }",
                nestedDescendants(3, "name"),
                aliasedTypenameQuery(5)}) {
            assertEquals("depth of " + query, graphqlJavaDepth(query), depth(query));
        }
    }

    /** How {@link graphql.analysis.MaxQueryDepthInstrumentation} measures depth, reproduced to compare against. */
    private static int graphqlJavaDepth(String query) {
        return traverser(query, CoercedVariables.emptyVariables())
                .reducePreOrder((env, acc) -> Math.max(pathLength(env.getParentEnvironment()), acc), 0);
    }

    private static int pathLength(QueryVisitorFieldEnvironment path) {
        int length = 1;
        while (path != null) {
            path = path.getParentEnvironment();
            length++;
        }
        return length;
    }

    // --- expanded fields ---

    @Test
    public void shouldCountAFragmentAtEveryPlaceItIsSpread() {
        String query = twiceSpreadFragments(8);
        // jcr and nodeByPath, then two parents per node over eight levels, and a name under each leaf: 3 * 2^8.
        assertEquals(768, expandedFields(query, NO_CEILING));
        // The document as written is small - jcr, nodeByPath, two spreads per level, the leaf's name - because a
        // traversal reads each fragment definition once. That is the gap between the two measures.
        assertEquals(19, complexity(query));
        assertEquals(11, depth(query));
    }

    @Test
    public void shouldCountMergedSpreadsOnce() {
        // Two spreads of one fragment under one response key execute as one field, so they count once.
        assertEquals(2, expandedFields("{ currentUser { ...f ...f } } fragment f on User { name }", NO_CEILING));
        // Under distinct aliases they are distinct response keys, and execute separately.
        assertEquals(3, expandedFields("{ currentUser { a: name b: name } }", NO_CEILING));
    }

    @Test
    public void shouldCountAliasedTypenameFieldsAsExpandedFields() {
        assertEquals(500, expandedFields(aliasedTypenameQuery(500), NO_CEILING));
    }

    @Test
    public void shouldNotCountFieldsExcludedBySkipAsExpandedFields() {
        assertEquals(1, expandedFields("{ currentUser { name @skip(if: true) } }", NO_CEILING));
    }

    @Test
    public void shouldAcceptAnOperationAtTheCeiling() {
        assertEquals(768, expandedFields(twiceSpreadFragments(8), 768));
    }

    @Test
    public void shouldRefuseAnOperationExpandingPastTheCeiling() {
        String query = twiceSpreadFragments(8);
        AbortExecutionException refusal = assertThrows(AbortExecutionException.class, () -> expandedFields(query, 100));
        // Refused as soon as the count passes the ceiling, so the message names the ceiling plus one rather than what
        // the operation would have expanded to: the measure costs at most the ceiling.
        assertEquals("Maximum field count exceeded. 101 > 100", refusal.getMessage());
    }

    // --- batch size ---

    @Test
    public void shouldCountTheItemsAnEnumeratedArgumentCarries() {
        assertEquals(3, batchSize("mutation { jcr { mutateNodes(pathsOrIds: [\"/a\", \"/b\", \"/c\"]) { uuid } } }"));
    }

    @Test
    public void shouldSumEnumeratedItemsAcrossAliasesSoAliasingCannotMultiplyTheBound() {
        // Why the document is measured rather than each call: every alias below is small, the request is not.
        assertEquals(30, batchSize(aliasedMutateNodes(10, 3)));
        // Batch size is its own metric: the same document's complexity reflects only its shape.
        assertEquals(21, complexity(aliasedMutateNodes(10, 3)));
    }

    @Test
    public void shouldCountItemsSuppliedThroughAVariableLikeInlineOnes() {
        // Arguments reach the traverser coerced, so the variable form is measured exactly like an inline one.
        String query = "mutation ($paths: [String]) { jcr { mutateNodes(pathsOrIds: $paths) { uuid } } }";
        CoercedVariables variables = CoercedVariables.of(
                Collections.singletonMap("paths", Arrays.asList("/a", "/b", "/c", "/d")));
        assertEquals(4, batchSize(query, variables));
    }

    @Test
    public void shouldNotCountScalarArgumentsOrArgumentlessFields() {
        assertEquals(0, batchSize("mutation { jcr { mutateNode(pathOrId: \"/a\") { uuid } } }"));
        assertEquals(0, batchSize("{ jcr { nodeByPath(path: \"/\") { name } } }"));
        assertEquals(0, batchSize("{ currentUser { name } }"));
    }

    @Test
    public void shouldCountItemsBuriedInsideNestedInputObjects() {
        // InputJCRNode holds a list of itself, so one outer item can describe a whole tree of nodes to create. Counting
        // only the outer list would score this as 1 while it asks for six nodes.
        String query = "mutation { jcr { addNodesBatch(nodes: [" +
                "{name: \"a\", children: [{name: \"a1\"}, {name: \"a2\"}]}, " +
                "{name: \"b\", children: [{name: \"b1\", children: [{name: \"b1x\"}]}]}" +
                "]) { uuid } } }";
        // 2 outer + 2 children of a + 1 child of b + 1 grandchild = 6
        assertEquals(6, batchSize(query));
    }

    @Test
    public void shouldCountNestedItemsSuppliedThroughAVariable() {
        String query = "mutation ($nodes: [InputJCRNode]) { jcr { addNodesBatch(nodes: $nodes) { uuid } } }";
        Map<String, Object> child = new HashMap<>();
        child.put("name", "c1");
        Map<String, Object> parent = new HashMap<>();
        parent.put("name", "p");
        parent.put("children", Arrays.asList(child, child, child));
        CoercedVariables variables = CoercedVariables.of(
                Collections.singletonMap("nodes", Collections.singletonList(parent)));
        // 1 outer + 3 children
        assertEquals(4, batchSize(query, variables));
    }

    @Test
    public void shouldNotCountListArgumentsThatDoNotDenoteNodes() {
        // Property values, mixin names and the like are cardinality of a different kind: they must not consume a node
        // allowance, or setting one multivalued property to many values would be refused as an oversized node batch.
        assertEquals(0, batchSize("mutation { jcr { mutateNodes(pathsOrIds: []) { setValues(values: " +
                stringList(4000) + ") } } }"));
        assertEquals(0, batchSize("mutation { jcr { mutateNodes(pathsOrIds: []) { addMixins(mixins: " +
                stringList(50) + ") } } }"));
    }

    @Test
    public void shouldCountAPathListOnEveryFieldThatBatchesOverIt() {
        // mutateNodes is not the only field handed a list of nodes to act on one by one: mutateVanityUrls is another,
        // extension providers contribute more, and each yields one mutation object per path. What they have in common is
        // the pair - a pathsOrIds list, and a list of mutations as their own type - so that is what is matched, rather
        // than a set of field names this class would have to be told about.
        assertEquals(3, batchSize("mutation { jcr { mutateVanityUrls(pathsOrIds: [\"/a\", \"/b\", \"/c\"]) { uuid } } }"));
        // The non-null wrappers on that field are part of the point: a batch is a batch however its type is qualified.
    }

    @Test
    public void shouldShareOneAllowanceAcrossDifferentBatchFields() {
        assertEquals(5, batchSize("mutation { jcr { " +
                "mutateNodes(pathsOrIds: [\"/a\", \"/b\"]) { uuid } " +
                "mutateVanityUrls(pathsOrIds: [\"/c\", \"/d\", \"/e\"]) { uuid } } }"));
    }

    @Test
    public void shouldNotCountAPathListOnAFieldThatProducesOneResultFromTheWholeSet() {
        // addToZip takes the same argument name to write a single archive: its cardinality is one whatever it is handed,
        // so it has no node batch to size and must not consume a node allowance. Its own type is what says so - a scalar
        // rather than a list of mutations - and a request adding many files to an archive stays within the bound.
        assertEquals(0, batchSize("mutation { jcr { mutateNode(pathOrId: \"/a\") { zip { addToZip(pathsOrIds: " +
                stringList(4000) + ") } } } }"));
        // The node the archive itself hangs off is still counted where it is named as a batch.
        assertEquals(2, batchSize("mutation { jcr { mutateNodes(pathsOrIds: [\"/a\", \"/b\"]) " +
                "{ zip { addToZip(pathsOrIds: " + stringList(4000) + ") } } } }"));
    }

    @Test
    public void shouldStopCountingOnceTheCeilingIsPassed() {
        String query = "mutation { jcr { mutateNodes(pathsOrIds: " + stringList(100) + ") { uuid } } }";
        QueryCostCalculator.QueryCost cost =
                QueryCostCalculator.calculate(traverser(query, CoercedVariables.emptyVariables()), 10);
        // Enough to decide the request is over, without having counted all of it.
        assertTrue("expected the count to exceed the ceiling, got " + cost.getBatchSize(), cost.getBatchSize() > 10);
    }

    @Test
    public void shouldNotMeasureBatchSizeWhenNoCeilingIsGiven() {
        // What a query operation asks for: the count is skipped rather than computed and discarded.
        String query = "mutation { jcr { mutateNodes(pathsOrIds: " + stringList(20) + ") { uuid } } }";
        assertEquals(0, QueryCostCalculator.calculate(
                traverser(query, CoercedVariables.emptyVariables()), 0).getBatchSize());
    }

    @Test
    public void shouldCountDeeplyNestedNodesWithoutRecursing() {
        // A node input holds a list of itself, so nesting depth is caller-controlled. Depth like this only arrives in a
        // variable - the parser rejects it inline - and counting it must not use the stack.
        Map<String, Object> node = new HashMap<>();
        node.put("name", "leaf");
        for (int i = 0; i < 20000; i++) {
            Map<String, Object> parent = new HashMap<>();
            parent.put("name", "n");
            parent.put("children", Collections.singletonList(node));
            node = parent;
        }
        CoercedVariables variables = CoercedVariables.of(
                Collections.singletonMap("nodes", Collections.singletonList(node)));
        String query = "mutation ($nodes: [InputJCRNode]) { jcr { addNodesBatch(nodes: $nodes) { uuid } } }";
        assertEquals(20001, batchSize(query, variables));
    }

    /** A GraphQL list literal of {@code size} distinct strings. */
    private static String stringList(int size) {
        return "[" + IntStream.range(0, size).mapToObj(i -> "\"v" + i + "\"").collect(Collectors.joining(", ")) + "]";
    }

}
