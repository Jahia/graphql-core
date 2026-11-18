package org.jahia.modules.graphql.provider.dxm.websocket;

import java.util.function.LongSupplier;

/**
 * Lets one report through per interval, and counts the events since the previous report.
 */
public final class ThrottledReport {

    private final long intervalNanos;
    private final LongSupplier nanoTime;
    private long nextReport;
    private int unreported;

    public ThrottledReport(long intervalNanos) {
        this(intervalNanos, System::nanoTime);
    }

    ThrottledReport(long intervalNanos, LongSupplier nanoTime) {
        this.intervalNanos = intervalNanos;
        this.nanoTime = nanoTime;
        this.nextReport = nanoTime.getAsLong();
    }

    /**
     * Counts an event.
     *
     * @return the number of events since the previous report when a report is due, or {@code 0} within the interval
     */
    public synchronized int count() {
        unreported++;
        long now = nanoTime.getAsLong();
        if (now - nextReport < 0) {
            return 0;
        }
        int events = unreported;
        unreported = 0;
        nextReport = now + intervalNanos;
        return events;
    }
}
