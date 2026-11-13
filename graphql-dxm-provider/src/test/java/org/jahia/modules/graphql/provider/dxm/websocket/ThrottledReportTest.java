package org.jahia.modules.graphql.provider.dxm.websocket;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public class ThrottledReportTest {

    @Test
    public void reportsTheFirstEventThenOncePerIntervalWithTheCount() {
        AtomicLong now = new AtomicLong(Long.MAX_VALUE - 5);
        ThrottledReport report = new ThrottledReport(10, now::get);

        assertEquals(1, report.count());
        assertEquals(0, report.count());
        now.addAndGet(9);
        assertEquals(0, report.count());
        now.addAndGet(1);
        assertEquals(3, report.count());
        assertEquals(0, report.count());
    }
}
