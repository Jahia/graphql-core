//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.jahia.modules.graphql.provider.dxm.relay;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.relay.Edge;
import org.apache.jackrabbit.util.ISO8601;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldEvaluator;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@GraphQLName("DXFieldAggregation")
@GraphQLDescription("Aggregations on nodes fields")
public class DXFieldAggregation<T> {
    private List<Edge<T>> edges;
    private FieldEvaluator evaluator;

    public DXFieldAggregation(List<Edge<T>> edges, FieldEvaluator evaluator) {
        this.edges = edges;
        this.evaluator = evaluator;
    }

    @GraphQLField
    @GraphQLName("sum")
    @GraphQLDescription("Sum aggregation")
    public Double getSum(@GraphQLName("name") @GraphQLDescription("The name of the field") @GraphQLNonNull String name) {
        return getAllValues(name).mapToDouble(v -> Double.parseDouble(v.toString())).sum();
    }

    @GraphQLField
    @GraphQLName("max")
    @GraphQLDescription("Max aggregation")
    public Double getMax(@GraphQLName("name") @GraphQLDescription("The name of the field") @GraphQLNonNull String name) {
        return getAllValues(name).mapToDouble(v -> Double.parseDouble(v.toString())).max().orElseThrow(() -> new DataFetchingException("No value"));
    }

    @GraphQLField
    @GraphQLName("min")
    @GraphQLDescription("Min aggregation")
    public Double getMin(@GraphQLName("name") @GraphQLDescription("The name of the field") @GraphQLNonNull String name) {
        return getAllValues(name).mapToDouble(v -> Double.parseDouble(v.toString())).min().orElseThrow(() -> new DataFetchingException("No value"));
    }

    @GraphQLField
    @GraphQLName("avg")
    @GraphQLDescription("Average aggregation")
    public Double getAvg(@GraphQLName("name") @GraphQLDescription("The name of the field") @GraphQLNonNull String name) {
        return getAllValues(name).mapToDouble(v -> Double.parseDouble(v.toString())).average().orElseThrow(() -> new DataFetchingException("No value"));
    }

    public String getFirstDate(@GraphQLName("name") @GraphQLDescription("The name of the field") @GraphQLNonNull String name) {
        Calendar c = new GregorianCalendar();
        c.setTimeInMillis(getAllValues(name).mapToLong(v -> ISO8601.parse(v.toString()).getTimeInMillis()).min().orElseThrow(() -> new DataFetchingException("No value")));
        return ISO8601.format(c);
    }

    public String getLastDate(@GraphQLName("name") @GraphQLDescription("The name of the field") @GraphQLNonNull String name) {
        Calendar c = new GregorianCalendar();
        c.setTimeInMillis(getAllValues(name).mapToLong(v -> ISO8601.parse(v.toString()).getTimeInMillis()).max().orElseThrow(() -> new DataFetchingException("No value")));
        return ISO8601.format(c);
    }

    private Stream<Object> getAllValues(@GraphQLNonNull @GraphQLName("fieldName") @GraphQLDescription("The name of the field") String fieldName) {
        return this.edges.stream().map(edge -> evaluator.getFieldValue(edge.getNode(), fieldName)).filter(Objects::nonNull);
    }

}
