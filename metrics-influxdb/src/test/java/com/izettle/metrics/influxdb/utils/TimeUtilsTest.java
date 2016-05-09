package com.izettle.metrics.influxdb.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class TimeUtilsTest {
    @Test
    public void test() {
        assertThat("m".equals(TimeUtils.toTimePrecision(TimeUnit.MINUTES)));
        assertThat("h".equals(TimeUtils.toTimePrecision(TimeUnit.HOURS)));
        assertThat("s".equals(TimeUtils.toTimePrecision(TimeUnit.SECONDS)));
        assertThat("ms".equals(TimeUtils.toTimePrecision(TimeUnit.MILLISECONDS)));
        assertThat("u".equals(TimeUtils.toTimePrecision(TimeUnit.MICROSECONDS)));
        assertThat("n".equals(TimeUtils.toTimePrecision(TimeUnit.NANOSECONDS)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowException() {
        TimeUtils.toTimePrecision(TimeUnit.DAYS);
    }
}
