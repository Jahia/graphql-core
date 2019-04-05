/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.graphql.provider.dxm.relay;

import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.lang.mutable.MutableObject;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.modules.graphql.provider.dxm.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Base64.getEncoder;

public class PaginationHelper {

    public static <T> DXPaginatedData<T> paginate(List<T> source, DataFetchingEnvironment environment) {
        PaginationHelper.Arguments arguments = PaginationHelper.parseArguments(environment);
        return paginate(source, obj -> encodeCursor("index:" + source.indexOf(obj)), arguments);
    }

    public static <T> DXPaginatedData<T> paginate(List<T> source, CursorSupport<T> cursorSupport, Arguments arguments) {
        List<T> filtered = applyCursorsToEdge(arguments, source, cursorSupport);
        filtered = applyFirstLast(filtered, arguments);
        filtered = applyLimitOffset(filtered, arguments);

        boolean hasPrevious = filtered.size() > 0 && filtered.get(0) != source.get(0);
        boolean hasNext = filtered.size() > 0 && filtered.get(filtered.size() - 1) != source.get(source.size() - 1);

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
        } else if (arguments.isOffsetLimit() && arguments.offset != null) {
            source = source.skip(arguments.offset);
            count.add(arguments.offset);
        }

        // Elements collected in dedicated list, depending on last/before combination
        Collection<T> items = arguments.last == null ? new ArrayList<>() : (arguments.before == null ? new CircularFifoQueue<>(arguments.last) : new CircularFifoQueue<>(arguments.last + 1));

        // Execute, collect items until condition is met
        Iterator<T> it = source.iterator();
        while (it.hasNext()
                && (arguments.limit == null || items.size() < arguments.limit)
                && (arguments.first == null || items.size() < arguments.first)) {
            last.setValue(it.next());
            String cursor = cursorSupport.getCursor((T) last.getValue());

            if (arguments.before == null || cursor.equals(arguments.before)) {
                items.add((T) last.getValue());
                count.increment();
            } else if (cursor.equals(arguments.before)) {
                //stop and skip the match
                count.increment();
                break;
            }

        }

        List<T> filtered = new ArrayList<>(items);

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
            hasNext = filtered.size() > 0 && arguments.before.equals(cursorSupport.getCursor(filtered.get(filtered.size()-1)));
            if (!hasNext && arguments.last != null) {
                // Cursor was not found, drop first element
                filtered = filtered.subList(1, filtered.size());
                count.increment();
            }
        }
        if (hasNext) {
            filtered = filtered.subList(0, filtered.size() -1 );
        }
        return new StreamBasedDXPaginatedData<>(filtered, cursorSupport, hasPrevious, hasNext, count.intValue(), it);
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
                    totalCount++;
                    remaining.next();
                }
            }
            return totalCount;
        }
    }
}
