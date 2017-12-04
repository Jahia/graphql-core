package org.jahia.modules.graphql.provider.dxm.relay;

import graphql.schema.DataFetchingEnvironment;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static java.util.Base64.getEncoder;

public class PaginationHelper {

    public static <T> DXPaginatedData<T> paginate(List<T> source, DataFetchingEnvironment environment) {
        return paginate(source, obj -> getEncoder().encodeToString(("index:" + source.indexOf(obj)).getBytes(StandardCharsets.UTF_8)), environment);
    }

    public static <T> DXPaginatedData<T> paginate(List<T> source, CursorSupport<T> cursorSupport, DataFetchingEnvironment environment) {
        Arguments args = parseArguments(environment);

        List<T> filtered = applyCursorsToEdge(args, source, cursorSupport);
        filtered = applyFirstLast(filtered, args);
        filtered = applyLimitOffset(filtered, args);

        boolean hasPrevious = filtered.size() > 0 && filtered.get(0) != source.get(0);
        boolean hasNext = filtered.size() > 0 && filtered.get(filtered.size()-1) != source.get(source.size()-1);

        return new SimpleDXPaginatedData<>(source, filtered, cursorSupport, hasPrevious, hasNext);
    }

    public static <T> List<T> applyLimitOffset(List<T> filtered, Arguments args) {
        if (args.offset != null) {
            filtered = filtered.subList(Math.min(args.offset, filtered.size()), filtered.size());
        }
        if (args.limit != null) {
            filtered = filtered.subList(0, filtered.size() > args.limit ? args.limit : filtered.size());
        }
        return filtered;
    }

    public static <T> List<T> applyFirstLast(List<T> filtered, Arguments args) {
        if (args.first != null) {
            filtered = filtered.subList(0, filtered.size() > args.first ? args.first : filtered.size());
        }
        if (args.last != null) {
            filtered = filtered.subList(filtered.size() > args.last ? filtered.size()-args.last : 0, filtered.size());
        }
        return filtered;
    }

    public static <T> List<T> applyCursorsToEdge(Arguments args, List<T> filtered, CursorSupport<T> cursorSupport) {
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
        return filtered;
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

    public static class SimpleDXPaginatedData<T> extends AbstractDXPaginatedData<T> {
        private final List<T> source;
        private final CursorSupport<T> cursorSupport;

        public SimpleDXPaginatedData(List<T> source, List<T> filtered, CursorSupport<T> cursorSupport, boolean hasPrevious, boolean hasNext) {
            super(filtered, hasPrevious, hasNext, filtered.size(), source.size());
            this.source = source;
            this.cursorSupport = cursorSupport;
        }

        @Override
        public String getCursor(T entity) {
            return cursorSupport.getCursor(entity);
        }

        @Override
        public int getIndex(T entity) {
            return source.indexOf(entity);
        }
    }
}
