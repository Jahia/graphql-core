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
import graphql.execution.CoercedVariables;
import graphql.language.Document;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

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
            "type JCRNodeConnection { nodes: [JCRNode] }";

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
        return QueryCostCalculator.calculate(traverser(query, CoercedVariables.emptyVariables()));
    }

    private static int complexity(String query) {
        return cost(query).getComplexity();
    }

    private static int depth(String query) {
        return cost(query).getDepth();
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
}
