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
package org.jahia.modules.graphql.provider.dxm.relay;

import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.collections.iterators.IteratorChain;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.lang.mutable.MutableObject;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.modules.graphql.provider.dxm.util.StreamUtils;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.util.Base64.getEncoder;

public class PaginationHelper {

    private static Logger logger = org.slf4j.LoggerFactory.getLogger(PaginationHelper.class);
    private static AtomicInteger nodeLimit = new AtomicInteger(5000);

    private PaginationHelper() {
    }

    public static <T> DXPaginatedData<T> paginate(List<T> source, DataFetchingEnvironment environment) {
        PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);
        return paginate(source, obj -> encodeCursor("index:" + source.indexOf(obj)), arguments);
    }

    public static <T> DXPaginatedData<T> paginate(List<T> source, CursorSupport<T> cursorSupport, Arguments arguments) {
        List<T> filtered = applyCursorsToEdge(arguments, source, cursorSupport);
        filtered = applyFirstLast(filtered, arguments);
        filtered = applyLimitOffset(filtered, arguments);

        boolean hasPrevious = !filtered.isEmpty() && filtered.get(0) != source.get(0);
        boolean hasNext = !filtered.isEmpty() && filtered.get(filtered.size() - 1) != source.get(source.size() - 1);

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
            filtered = filtered.subList(filtered.size() > args.last ? filtered.size() - args.last : 0, filtered.size());
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

    public static <T> DXPaginatedData<T> paginate(Stream<T> source, CursorSupport<T> cursorSupport, Arguments arguments) {
        MutableInt count = new MutableInt(0);
        MutableObject last = new MutableObject();
        if (arguments.isCursor() && arguments.after != null) {
            // Drop first elements until cursor match, then also skip matching element
            source = StreamUtils.dropUntil(source, t -> cursorSupport.getCursor(t).equals(arguments.after), count).skip(1);
        } else if (arguments.isOffsetLimit() && arguments.offset != null && arguments.offset > 0) {
            // Drop first elements until the offset is reached
            source = StreamUtils.dropUntil(source, t -> count.intValue() == arguments.offset, count).skip(1);
        }
        Iterator<T> it = source.iterator();

        // Execute, collect items until condition is met
        List<T> filtered = collectItems(it, arguments, cursorSupport, count, last);

        // Substract result size from count to get the offset of first item
        count.subtract(filtered.size());
        boolean hasPrevious = count.intValue() > 0;
        boolean hasNext = false;

        if (arguments.limit != null) {
            hasNext = filtered.size() > arguments.limit;
        } else if (arguments.first != null) {
            hasNext = filtered.size() > arguments.first;
        }
        if (!hasNext && arguments.before != null) {
            hasNext = !filtered.isEmpty() && arguments.before.equals(cursorSupport.getCursor(filtered.get(filtered.size()-1)));
            if (!hasNext && hasPrevious && arguments.last != null) {
                // Cursor was not found, drop first element
                filtered = filtered.subList(1, filtered.size());
                count.increment();
            }
        }
        if (hasNext) {
            // Restore last item in iterator to get correct total count
            it = new IteratorChain(Collections.singleton(filtered.get(filtered.size() - 1)).iterator(), it);
            filtered = filtered.subList(0, filtered.size() -1 );
        }
        return new StreamBasedDXPaginatedData<>(filtered, cursorSupport, hasPrevious, hasNext, count.intValue(), it);
    }

    @NotNull
    private static <T> List<T> collectItems(Iterator<T> it, Arguments arguments, CursorSupport<T> cursorSupport, MutableInt count, MutableObject last) {
        int nodeLimit = getNodeLimit();
        // Elements collected in dedicated list, depending on last/before combination
        Collection<T> items;
        if (arguments.last == null) {
            items = new ArrayList<>();
        } else if (arguments.before == null) {
            items = new CircularFifoQueue<>(arguments.last);
        } else {
            items = new CircularFifoQueue<>(arguments.last + 1);
        }

        while (it.hasNext()
                && (arguments.limit == null || items.size() <= arguments.limit)
                && (arguments.first == null || items.size() <= arguments.first)
               ) {
            T value = it.next();
            last.setValue(value);
            items.add(value);
            count.increment();

            if (arguments.before != null && cursorSupport.getCursor(value).equals(arguments.before)) {
                break;
            }

            //Adding a limit of 1000 to avoid OOM
            if(count.intValue() == 500) {
                logger.warn("The current paginated query is returning more than 500 items. This may cause a memory leak.");
            } else if(count.intValue() == nodeLimit && logger.isWarnEnabled()) {
                logger.warn("The current paginated query is returning more than {} items. Stopping the query.", nodeLimit, new DataFetchingException("The current paginated query is returning more than " + nodeLimit + " items. Stopping the query here."));
                break;
            }
        }
        return new ArrayList<>(items);
    }

    public static void updateLimit(int limit) {
        logger.info("Node limit has been updated to {}", limit);
        nodeLimit.set(limit);
    }

    public static int getNodeLimit() {
        return nodeLimit.get();
    }

    public static Arguments parseArguments(DataFetchingEnvironment environment) {
        return new Arguments(environment.getArgument("before"),
                environment.getArgument("after"),
                environment.getArgument("first"),
                environment.getArgument("last"),
                environment.getArgument("offset"),
                environment.getArgument("limit"));
    }

    public static String encodeCursor(String s) {
        return getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    public static class Arguments {
        String before;
        String after;
        Integer first;
        Integer last;
        Integer offset;
        Integer limit;

        public boolean isOffsetLimit() {
            return (this.offset != null || this.limit != null);
        }

        public boolean isCursor() {
            return (this.before != null || this.after != null || this.first != null || this.last != null);
        }

        private Integer validateNotNegativeValue(Integer value, String argument) {
            if (value != null && value < 0) {
                throw new GqlJcrWrongInputException("Argument '" + argument + "' can't be negative");
            }
            return value;
        }

        public Arguments(String before, String after, Integer first, Integer last, Integer offset, Integer limit) {
            this.before = before;
            this.after = after;
            this.first = validateNotNegativeValue(first, "first");
            this.last = validateNotNegativeValue(last, "last");
            this.offset = validateNotNegativeValue(offset, "offset");
            this.limit = validateNotNegativeValue(limit, "limit");

            if (isCursor() && isOffsetLimit()) {
                throw new GqlJcrWrongInputException("Offset and/or Limit argument(s) can't be used with other pagination arguments");
            }
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

    public static class StreamBasedDXPaginatedData<T> extends AbstractDXPaginatedData<T> {
        private final List<T> filtered;
        private final Iterator<T> remaining;
        private final CursorSupport<T> cursorSupport;
        private int startOffset = 0;

        public StreamBasedDXPaginatedData(List<T> filtered, CursorSupport<T> cursorSupport, boolean hasPrevious, boolean hasNext, int startOffset, Iterator<T> remaining) {
            super(filtered, hasPrevious, hasNext, filtered.size(), -1);
            this.filtered = filtered;
            this.remaining = remaining;
            this.cursorSupport = cursorSupport;
            this.startOffset = startOffset;
        }

        @Override
        public String getCursor(T entity) {
            return cursorSupport.getCursor(entity);
        }

        @Override
        public int getIndex(T entity) {
            return startOffset + filtered.indexOf(entity);
        }

        @Override
        public int getTotalCount() {
            if (totalCount == -1) {
                totalCount = startOffset + filtered.size();
                while (remaining.hasNext()) {
                    remaining.next();
                    totalCount++;
                }
            }
            return totalCount;
        }
    }
}
