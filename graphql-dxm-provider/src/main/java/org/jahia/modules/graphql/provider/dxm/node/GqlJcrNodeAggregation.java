/*
 *  ==========================================================================================
 *  =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 *  ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 *      Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *      THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *      1/GPL OR 2/JSEL
 *
 *      1/ GPL
 *      ==================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *      2/ JSEL - Commercial and Supported Versions of the program
 *      ===================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      Alternatively, commercial and supported versions of the program - also known as
 *      Enterprise Distributions - must be used in accordance with the terms and conditions
 *      contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *      If you are unsure which license is appropriate for your use,
 *      please contact the sales department at sales@jahia.com.
 *
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
    @GraphQLDescription("Count aggregation")
    public CountAggregation getCount() {
        return new CountAggregation();
    }

    @GraphQLField
    @GraphQLDescription("Sum aggregation")
    public StatAggregation getSum() {
        return new StatAggregation(LongStream::sum, DoubleStream::sum);
    }

    @GraphQLField
    @GraphQLDescription("Max aggregation")
    public StatAggregation getMax() {
        return new StatAggregation((l) -> l.max().orElseThrow(() -> new DataFetchingException("No value")),
                (d) -> d.max().orElseThrow(() -> new DataFetchingException("No value")));
    }

    @GraphQLField
    @GraphQLDescription("Min aggregation")
    public StatAggregation getMin() {
        return new StatAggregation((l) -> l.min().orElseThrow(() -> new DataFetchingException("No value")),
                (d) -> d.min().orElseThrow(() -> new DataFetchingException("No value")));
    }

    @GraphQLField
    @GraphQLDescription("Average aggregation")
    public StatAggregation getAvg() {
        return new StatAggregation((l) -> (long) l.average().orElseThrow(() -> new DataFetchingException("No value")),
                (d) -> d.average().orElseThrow(() -> new DataFetchingException("No value")));
    }


    public class CountAggregation {
        @GraphQLField
        @GraphQLDescription("Count all values")
        public Integer values(@GraphQLName("name") @GraphQLDescription("The name of the JCR property") @GraphQLNonNull String name,
                              @GraphQLName("language") @GraphQLDescription("The language to obtain the property in; must be a valid language code for internationalized properties, does not matter for non-internationalized ones") String language) {
            return (int) getAllValues(name, language).count();
        }
    }

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
            return longFunction.apply(mapLong(getAllValues(name, language)));
        }

        @GraphQLField
        @GraphQLDescription("The double representation of a JCR node property")
        public Double doublePropertyValue(@GraphQLName("name") @GraphQLDescription("The name of the JCR property") @GraphQLNonNull String name,
                                          @GraphQLName("language") @GraphQLDescription("The language to obtain the property in; must be a valid language code for internationalized properties, does not matter for non-internationalized ones") String language) {
            return doubleFunction.apply(mapDouble(getAllValues(name, language)));
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

    private LongStream mapLong(Stream<Value> values) {
        return values.mapToLong(v -> {
            try {
                return v.getLong();
            } catch (RepositoryException e) {
                throw new DataFetchingException(e);
            }
        });
    }

    private DoubleStream mapDouble(Stream<Value> values) {
        return values.mapToDouble(v -> {
            try {
                return v.getDouble();
            } catch (RepositoryException e) {
                throw new DataFetchingException(e);
            }
        });
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
