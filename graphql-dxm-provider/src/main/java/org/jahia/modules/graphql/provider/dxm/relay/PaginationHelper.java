package org.jahia.modules.graphql.provider.dxm.relay;

import graphql.schema.DataFetchingEnvironment;

import java.util.List;
import java.util.Optional;

public class PaginationHelper {

    public static <T> DXPaginatedData<T> paginate(List<T> source, CursorSupport<T> cursorSupport, DataFetchingEnvironment environment) {
        Arguments args = parseArguments(environment);

        List<T> filtered = source;
        if (args.after != null) {
            Optional<T> after = filtered.stream().filter(s -> args.after.equals(cursorSupport.getCursor(s))).findFirst();
            if (after.isPresent()) {
                filtered = filtered.subList(filtered.indexOf(after.get()) + 1, filtered.size());
            }
        }

        if (args.before != null) {
            Optional<T> before = filtered.stream().filter(s -> args.before.equals(cursorSupport.getCursor(s))).findFirst();
            if (before.isPresent()) {
                filtered = filtered.subList(0, filtered.indexOf(before.get()));
            }
        }

        if (args.first != null) {
            filtered = filtered.subList(0, filtered.size() > args.first ? args.first : filtered.size());
        }
        if (args.last != null) {
            filtered = filtered.subList(filtered.size() > args.last ? filtered.size()-args.last : 0, filtered.size());
        }

        // todo : handle limit/offset parameters

        boolean hasPrevious = filtered.size() > 0 && filtered.get(0) != source.get(0);
        boolean hasNext = filtered.size() > 0 && filtered.get(filtered.size()-1) != source.get(source.size()-1);

        return new AbstractDXPaginatedData<T>(filtered, hasPrevious, hasNext, filtered.size(), source.size()) {
            @Override
            public String getCursor(T entity) {
                return cursorSupport.getCursor(entity);
            }
        };
    }

    public static Arguments parseArguments(DataFetchingEnvironment environment) {
        return new Arguments(environment.getArgument("before"),
                environment.getArgument("after"),
                environment.getArgument("first"),
                environment.getArgument("last"),
                environment.getArgument("offset"),
                environment.getArgument("limit"));
    }

    public static class Arguments {
        String before;
        String after;
        Integer first;
        Integer last;
        Integer offset;
        Integer limit;

        public Arguments(String before, String after, Integer first, Integer last, Integer offset, Integer limit) {
            this.before = before;
            this.after = after;
            this.first = first;
            this.last = last;
            this.offset = offset;
            this.limit = limit;
        }
    }

}
