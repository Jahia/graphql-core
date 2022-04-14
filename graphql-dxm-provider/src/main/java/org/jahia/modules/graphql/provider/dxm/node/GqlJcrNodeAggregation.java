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
import graphql.relay.Edge;
import org.apache.jackrabbit.util.ISO8601;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import pl.touk.throwing.ThrowingSupplier;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

@GraphQLName("JCRNodeAggregation")
@GraphQLDescription("Aggregations on JCR Nodes")
public class GqlJcrNodeAggregation {

    private List<Edge<GqlJcrNode>> edges;

    public GqlJcrNodeAggregation(List<Edge<GqlJcrNode>> edges) {
        this.edges = edges;
    }


    @GraphQLField
    @GraphQLName("count")
    @GraphQLDescription("Count aggregation")
    public CountAggregation getCount() {
        return new CountAggregation();
    }

    @GraphQLField
    @GraphQLName("sum")
    @GraphQLDescription("Sum aggregation")
    public StatAggregation getSum() {
        return new StatAggregation(LongStream::sum, DoubleStream::sum);
    }

    @GraphQLField
    @GraphQLName("max")
    @GraphQLDescription("Max aggregation")
    public StatAggregation getMax() {
        return new StatAggregation((l) -> l.max().orElseThrow(() -> new DataFetchingException("No value")),
                (d) -> d.max().orElseThrow(() -> new DataFetchingException("No value")));
    }

    @GraphQLField
    @GraphQLName("min")
    @GraphQLDescription("Min aggregation")
    public StatAggregation getMin() {
        return new StatAggregation((l) -> l.min().orElseThrow(() -> new DataFetchingException("No value")),
                (d) -> d.min().orElseThrow(() -> new DataFetchingException("No value")));
    }

    @GraphQLField
    @GraphQLName("avg")
    @GraphQLDescription("Average aggregation")
    public StatAggregation getAvg() {
        return new StatAggregation((l) -> (long) l.average().orElseThrow(() -> new DataFetchingException("No value")),
                (d) -> d.average().orElseThrow(() -> new DataFetchingException("No value")));
    }


    @GraphQLDescription("Simple node count aggregation")
    public class CountAggregation {
        @GraphQLField
        @GraphQLDescription("Count all values")
        public Integer values(@GraphQLName("name") @GraphQLDescription("The name of the JCR property") @GraphQLNonNull String name,
                              @GraphQLName("language") @GraphQLDescription("The language to obtain the property in; must be a valid language code for internationalized properties, does not matter for non-internationalized ones") String language) {
            return (int) getAllValues(name, language).count();
        }
    }

    @GraphQLDescription("Simple numeric aggregation on properties values")
    public class StatAggregation {
        private Function<LongStream, Long> longFunction;
        private Function<DoubleStream, Double> doubleFunction;

        public StatAggregation(Function<LongStream, Long> longFunction, Function<DoubleStream, Double> doubleFunction) {
            this.longFunction = longFunction;
            this.doubleFunction = doubleFunction;
        }

        @GraphQLField
        @GraphQLDescription("The long representation of a JCR node property")
        public Long longPropertyValue(@GraphQLName("name") @GraphQLDescription("The name of the JCR property") @GraphQLNonNull String name,
                                      @GraphQLName("language") @GraphQLDescription("The language to obtain the property in; must be a valid language code for internationalized properties, does not matter for non-internationalized ones") String language) {
            return longFunction.apply(getAllValues(name, language).mapToLong(v -> ThrowingSupplier.unchecked(v::getLong).get()));
        }

        @GraphQLField
        @GraphQLDescription("The float representation of a JCR node property")
        public Double floatPropertyValue(@GraphQLName("name") @GraphQLDescription("The name of the JCR property") @GraphQLNonNull String name,
                                         @GraphQLName("language") @GraphQLDescription("The language to obtain the property in; must be a valid language code for internationalized properties, does not matter for non-internationalized ones") String language) {
            return doubleFunction.apply(getAllValues(name, language).mapToDouble(v -> ThrowingSupplier.unchecked(v::getDouble).get()));
        }

        @GraphQLField
        @GraphQLDescription("The date representation of a JCR node property")
        public String datePropertyValue(@GraphQLName("name") @GraphQLDescription("The name of the JCR property") @GraphQLNonNull String name,
                                        @GraphQLName("language") @GraphQLDescription("The language to obtain the property in; must be a valid language code for internationalized properties, does not matter for non-internationalized ones") String language) {
            GregorianCalendar c = new GregorianCalendar();
            c.setTime(new Date(longPropertyValue(name, language)));
            return ISO8601.format(c);
        }
    }

    private Stream<Value> getAllValues(@GraphQLNonNull @GraphQLName("name") @GraphQLDescription("The name of the JCR property") String name, @GraphQLName("language") @GraphQLDescription("The language to obtain the property in; must be a valid language code for internationalized properties, does not matter for non-internationalized ones") String language) {
        return edges.stream().flatMap(edge -> {
            try {
                JCRNodeWrapper node = NodeHelper.getNodeInLanguage(edge.getNode().getNode(), language);
                if (!node.hasProperty(name)) {
                    return null;
                }

                JCRPropertyWrapper property = node.getProperty(name);
                if (property.isMultiple()) {
                    return Arrays.stream(property.getValues());
                } else {
                    return Stream.of(property.getValue());
                }
            } catch (RepositoryException e) {
                throw new DataFetchingException(e);
            }
        });
    }

}
