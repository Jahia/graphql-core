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
 * <p>The schema below mirrors the shape of the JCR schema that matters here: a recursive node type reachable through
 * Relay-style connections, plus a list of scalars.
 */
public class QueryCostCalculatorTest {

    private static final String SDL = "type Query { jcr: JCRQuery currentUser: User } " +
            "type User { name: String displayName: String } " +
            "type JCRQuery { nodeByPath(path: String): JCRNode } " +
            "type JCRNode { name: String uuid: String parent: JCRNode ancestors: [JCRNode]! " +
            "               properties: [JCRProperty] children: JCRNodeConnection descendants: JCRNodeConnection } " +
            "type JCRNodeConnection { nodes: [JCRNode] edges: [JCRNodeEdge] } " +
            "type JCRNodeEdge { node: JCRNode cursor: String } " +
            "type JCRProperty { name: String values: [String] }";

    private static GraphQLSchema schema;

    @BeforeClass
    public static void setUpSchema() {
        schema = new SchemaGenerator().makeExecutableSchema(
                new SchemaParser().parse(SDL), RuntimeWiring.newRuntimeWiring().build());
    }

    private static QueryTraverser traverser(String query) {
        Document document = Parser.parse(query);
        return QueryTraverser.newQueryTraverser()
                .schema(schema)
                .document(document)
                .coercedVariables(CoercedVariables.emptyVariables())
                .build();
    }

    private static int complexity(String query) {
        return QueryCostCalculator.calculateComplexity(traverser(query));
    }

    private static int listNesting(String query) {
        return QueryCostCalculator.calculateMaxListNesting(traverser(query));
    }

    /** Builds {@code { a0:__typename a1:__typename ... }}, the shape of the reported amplification payload. */
    private static String aliasedTypenameQuery(int aliasCount) {
        return "{" + IntStream.range(0, aliasCount)
                .mapToObj(i -> "a" + i + ":__typename")
                .collect(Collectors.joining(" ")) + "}";
    }

    // --- complexity ---

    @Test
    public void shouldCountEveryAliasedTypenameField() {
        // The reported bypass: each alias is a separate selection that costs a permission check and, once denied, an
        // error object in the response, so each must be charged.
        assertEquals(500, complexity(aliasedTypenameQuery(500)));
    }

    @Test
    public void graphqlJavaCalculatorScoresAliasedTypenameAsZero() {
        // Documents WHY this class exists rather than reusing graphql-java's calculator: that one exempts __typename
        // unconditionally (the check sits in a private method, so a custom FieldComplexityCalculator cannot restore
        // it), scoring the payload above as 0 and letting it pass any budget. Should graphql-java ever charge for
        // __typename, this test fails and JahiaMaxQueryComplexityInstrumentation can be dropped for the built-in one.
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

    // --- list nesting ---

    @Test
    public void shouldReportZeroListNestingWhenNoListIsSelected() {
        assertEquals(0, listNesting("{ jcr { nodeByPath(path: \"/\") { parent { parent { name } } } } }"));
    }

    @Test
    public void shouldCountNestedConnections() {
        // nodes -> nodes -> nodes; the connection wrappers themselves are single objects and do not count.
        assertEquals(3, listNesting("{ jcr { nodeByPath(path: \"/\") { descendants { nodes { " +
                "descendants { nodes { descendants { nodes { name } } } } } } } } }"));
    }

    @Test
    public void shouldCountEdgesLikeNodes() {
        assertEquals(2, listNesting("{ jcr { nodeByPath(path: \"/\") { children { edges { node { " +
                "children { edges { node { name } } } } } } } } }"));
    }

    @Test
    public void shouldCountNonNullWrappedLists() {
        // ancestors is [JCRNode]!, i.e. NonNull(List(JCRNode)): the non-null wrapper must not hide the list.
        assertEquals(1, listNesting("{ jcr { nodeByPath(path: \"/\") { ancestors { name } } } }"));
    }

    @Test
    public void shouldIgnoreListsOfScalars() {
        // properties is a list of objects and counts; values is a list of scalars, a leaf that cannot recurse.
        assertEquals(1, listNesting("{ jcr { nodeByPath(path: \"/\") { properties { name values } } } }"));
    }

    @Test
    public void shouldReportTheDeepestPathNotTheTotal() {
        // Two sibling branches: one nesting 1 list, one nesting 3. The guard bounds the deepest path.
        assertEquals(3, listNesting("{ jcr { nodeByPath(path: \"/\") { " +
                "ancestors { name } " +
                "descendants { nodes { descendants { nodes { descendants { nodes { name } } } } } } } } }"));
    }
}
