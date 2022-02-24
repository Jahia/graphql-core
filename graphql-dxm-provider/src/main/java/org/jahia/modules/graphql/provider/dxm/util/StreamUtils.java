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
package org.jahia.modules.graphql.provider.dxm.util;

import org.apache.commons.lang.mutable.MutableInt;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Stream utils functions
 */
public class StreamUtils {
    public static <T> Stream<T> takeWhile(Stream<T> stream, Predicate<? super T> p) {
        class Taking extends Spliterators.AbstractSpliterator<T> implements Consumer<T> {
            private static final int CANCEL_CHECK_COUNT = 63;
            private final Spliterator<T> s;
            private int count;
            private T t;
            private final AtomicBoolean cancel = new AtomicBoolean();
            private boolean takeOrDrop = true;

            Taking(Spliterator<T> s) {
                super(s.estimateSize(), s.characteristics() & ~(Spliterator.SIZED | Spliterator.SUBSIZED));
                this.s = s;
            }

            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                boolean test = true;
                if (takeOrDrop &&               // If can take
                        (count != 0 || !cancel.get()) && // and if not cancelled
                        s.tryAdvance(this) &&   // and if advanced one element
                        (test = p.test(t))) {   // and test on element passes
                    action.accept(t);           // then accept element
                    return true;
                } else {
                    // Taking is finished
                    takeOrDrop = false;
                    // Cancel all further traversal and splitting operations
                    // only if test of element failed (short-circuited)
                    if (!test)
                        cancel.set(true);
                    return false;
                }
            }

            @Override
            public Comparator<? super T> getComparator() {
                return s.getComparator();
            }

            @Override
            public void accept(T t) {
                count = (count + 1) & CANCEL_CHECK_COUNT;
                this.t = t;
            }

            @Override
            public Spliterator<T> trySplit() {
                return null;
            }
        }
        return StreamSupport.stream(new Taking(stream.spliterator()), stream.isParallel()).onClose(stream::close);
    }

    /**
     * Drop all elements until predicate is true
     * @param stream the given stream
     * @param p the predicate
     * @return the stream
     */
    public static <T> Stream<T> dropUntil(Stream<T> stream, Predicate<? super T> p, MutableInt counter) {
        return stream.filter(FromPredicate.from(p, counter));
    }

    static class FromPredicate<T> implements Predicate<T> {
        private MutableInt counter;
        private boolean started = false;
        private Predicate<T> test;

        private FromPredicate(Predicate<T> test, MutableInt counter) {
            this.test = test;
            this.counter = counter != null ? counter : new MutableInt(0);
        }

        public static <T> Predicate<T> from(Predicate<T> test, MutableInt counter) {
            return new FromPredicate<>(test, counter);
        }

        public boolean test(T t) {
            if (!started) {
                counter.increment();
                started = test.test(t);
            }
            return started;
        }
    }
}
