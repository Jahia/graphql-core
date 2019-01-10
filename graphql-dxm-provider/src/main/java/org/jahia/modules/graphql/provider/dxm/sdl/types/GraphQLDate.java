package org.jahia.modules.graphql.provider.dxm.sdl.types;


import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import org.jahia.modules.graphql.provider.dxm.util.DateTimeUtils;

import java.time.LocalDateTime;
import java.util.Date;

public class GraphQLDate extends GraphQLScalarType {

    private static final String DEFAULT_NAME = "Date";

    public GraphQLDate() {
        this(DEFAULT_NAME);
    }

    public GraphQLDate(final String name) {
        super(name, "Date type", new Coercing<Date, String>() {
            private Date convertImpl(Object input) {
                if(input instanceof Long){
                    return new Date((Long)input);
                }else if (input instanceof String) {
                    LocalDateTime localDateTime = DateTimeUtils.parseDate((String) input);

                    if (localDateTime != null) {
                        return DateTimeUtils.toDate(localDateTime);
                    }
                }
                return null;
            }

            @Override
            public String serialize(Object input) {
                if (input instanceof Date) {
                    return DateTimeUtils.toISOString((Date) input);
                } else {
                    Date result = convertImpl(input);
                    if (result == null) {
                        throw new CoercingSerializeException("Invalid value '" + input + "' for Date");
                    }
                    return DateTimeUtils.toISOString(result);
                }
            }

            @Override
            public Date parseValue(Object input) {
                Date result = convertImpl(input);
                if (result == null) {
                    throw new CoercingParseValueException("Invalid value '" + input + "' for Date");
                }
                return result;
            }

            @Override
            public Date parseLiteral(Object input) {
                if (!(input instanceof StringValue)) return null;
                String value = ((StringValue) input).getValue();
                Date result = convertImpl(value);
                return result;
            }
        });
    }

}
