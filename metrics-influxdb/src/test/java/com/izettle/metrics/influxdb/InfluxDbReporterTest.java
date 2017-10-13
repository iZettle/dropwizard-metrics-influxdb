package com.izettle.metrics.influxdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.izettle.metrics.influxdb.data.InfluxDbPoint;
import com.izettle.metrics.influxdb.data.InfluxDbWriteObject;
import java.io.IOException;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.collections.Sets;

public class InfluxDbReporterTest {
    @Mock
    private InfluxDbSender influxDb;
    @Mock
    private MetricRegistry registry;
    private InfluxDbReporter reporter;
    private Map<String, String> globalTags;

    @Before
    public void init() {
        globalTags = new HashMap<String, String>();
        globalTags.put("global", "tag001");
        MockitoAnnotations.initMocks(this);
        reporter = InfluxDbReporter
            .forRegistry(registry)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .filter(MetricFilter.ALL)
            .withTags(globalTags)
            .build(influxDb);

    }

    @Test
    public void reportsCounters() throws Exception {
        final Counter counter = mock(Counter.class);
        when(counter.getCount()).thenReturn(100L);

        reporter.report(this.<Gauge>map(), this.map("counter", counter), this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());
        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        verify(influxDb).setTags(globalTags);
        InfluxDbPoint point = influxDbPointCaptor.getValue();
        assertThat(point.getMeasurement()).isEqualTo("counter");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(1);
        assertThat(point.getFields()).containsEntry("count", 100L);
        assertThat(point.getTags())
            .containsEntry("metricName", "counter");

    }

    @Test
    public void reportsGauges() throws Exception {
        final Gauge gauge = mock(Gauge.class);
        Mockito.when(gauge.getValue()).thenReturn(10L);
        reporter.report(this.map("gauge", gauge), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());
        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        verify(influxDb).setTags(globalTags);
        InfluxDbPoint point = influxDbPointCaptor.getValue();
        assertThat(point.getMeasurement()).isEqualTo("gauge");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(1);
        assertThat(point.getFields()).contains(entry("value", 10L));
        assertThat(point.getTags()).containsEntry("metricName", "gauge");
    }

    @Test
    public void reportsGroupedGauges() throws Exception {
        final Gauge gauge = mock(Gauge.class);
        Mockito.when(gauge.getValue()).thenReturn(10L);

        InfluxDbReporter groupReporter = InfluxDbReporter
            .forRegistry(registry)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .filter(MetricFilter.ALL)
            .groupGauges(true)
            .build(influxDb);

        groupReporter.report(this.map("gauge", gauge), this.<Counter>map(), this.<Histogram>map(),
            this.<Meter>map(), this.<Timer>map());
        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        verify(influxDb).setTags(globalTags);
        InfluxDbPoint point = influxDbPointCaptor.getValue();
        assertThat(point.getMeasurement()).isEqualTo("gauge");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(1);
        assertThat(point.getFields()).contains(entry("value", 10L));
        assertThat(point.getTags()).containsEntry("metricName", "gauge");

        groupReporter.report(this.map("gauge.1", gauge), this.<Counter>map(), this.<Histogram>map(),
            this.<Meter>map(), this.<Timer>map());
        verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        point = influxDbPointCaptor.getValue();
        assertThat(point.getMeasurement()).isEqualTo("gauge");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(1);
        assertThat(point.getFields()).contains(entry("1", 10L));
        assertThat(point.getTags()).containsEntry("metricName", "gauge");

        // if metric name terminates in `.' field name should be empty
        groupReporter.report(this.map("gauge.", gauge), this.<Counter>map(), this.<Histogram>map(),
            this.<Meter>map(), this.<Timer>map());
        verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        point = influxDbPointCaptor.getValue();
        assertThat(point.getMeasurement()).isEqualTo("gauge");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(1);
        assertThat(point.getFields()).contains(entry("", 10L));
        assertThat(point.getTags()).containsEntry("metricName", "gauge");

        SortedMap<String, Gauge> gauges = this.map("gauge.a", gauge);
        gauges.put("gauge.b", gauge);
        gauges.put("gauge.", gauge);
        gauges.put("gauge", gauge);
        groupReporter.report(gauges, this.<Counter>map(), this.<Histogram>map(),
            this.<Meter>map(), this.<Timer>map());
        verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());

        point = influxDbPointCaptor.getValue();
        assertThat(point.getMeasurement()).isEqualTo("gauge");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(4);
        assertThat(point.getFields()).contains(entry("", 10L), entry("a", 10L), entry("b", 10L), entry("value", 10L));
        assertThat(point.getTags()).containsEntry("metricName", "gauge");
    }

    @Test
    public void reportsHistograms() throws Exception {
        final Histogram histogram = mock(Histogram.class);
        when(histogram.getCount()).thenReturn(1L);

        final Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getMax()).thenReturn(2L);
        when(snapshot.getMean()).thenReturn(3.0);
        when(snapshot.getMin()).thenReturn(4L);
        when(snapshot.getStdDev()).thenReturn(5.0);
        when(snapshot.getMedian()).thenReturn(6.0);
        when(snapshot.get75thPercentile()).thenReturn(7.0);
        when(snapshot.get95thPercentile()).thenReturn(8.0);
        when(snapshot.get98thPercentile()).thenReturn(9.0);
        when(snapshot.get99thPercentile()).thenReturn(10.0);
        when(snapshot.get999thPercentile()).thenReturn(11.0);

        when(histogram.getSnapshot()).thenReturn(snapshot);

        reporter.report(this.<Gauge>map(), this.<Counter>map(), this.map("histogram", histogram), this.<Meter>map(), this.<Timer>map());

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        assertThat(point.getMeasurement()).isEqualTo("histogram");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(11);
        assertThat(point.getFields()).contains(entry("max", 2L));
        assertThat(point.getFields()).contains(entry("mean", 3.0));
        assertThat(point.getFields()).contains(entry("min", 4L));
        assertThat(point.getFields()).contains(entry("stddev", 5.0));
        assertThat(point.getFields()).contains(entry("p50", 6.0));
        assertThat(point.getFields()).contains(entry("p75", 7.0));
        assertThat(point.getFields()).contains(entry("p95", 8.0));
        assertThat(point.getFields()).contains(entry("p98", 9.0));
        assertThat(point.getFields()).contains(entry("p99", 10.0));
        assertThat(point.getFields()).contains(entry("p999", 11.0));
        assertThat(point.getTags()).containsEntry("metricName", "histogram");
    }

    @Test
    public void reportsMeters() throws Exception {
        final Meter meter = mock(Meter.class);
        when(meter.getCount()).thenReturn(1L);
        when(meter.getOneMinuteRate()).thenReturn(2.0);
        when(meter.getFiveMinuteRate()).thenReturn(3.0);
        when(meter.getFifteenMinuteRate()).thenReturn(4.0);
        when(meter.getMeanRate()).thenReturn(5.0);

        reporter.report(this.<Gauge>map(), this.<Counter>map(), this.<Histogram>map(), this.map("meter", meter), this.<Timer>map());

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        assertThat(point.getMeasurement()).isEqualTo("meter");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(5);
        assertThat(point.getFields()).contains(entry("count", 1L));
        assertThat(point.getFields()).contains(entry("m1_rate", 2.0));
        assertThat(point.getFields()).contains(entry("m5_rate", 3.0));
        assertThat(point.getFields()).contains(entry("m15_rate", 4.0));
        assertThat(point.getFields()).contains(entry("mean_rate", 5.0));
        assertThat(point.getTags()).containsEntry("metricName", "meter");
    }

    @Test
    public void reportsIncludedMeters() throws Exception {

        InfluxDbReporter filteredReporter = InfluxDbReporter
            .forRegistry(registry)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .filter(MetricFilter.ALL)
            .groupGauges(true)
            .includeMeterFields(Sets.newSet("m1_rate"))
            .build(influxDb);


        final Meter meter = mock(Meter.class);
        when(meter.getCount()).thenReturn(1L);
        when(meter.getOneMinuteRate()).thenReturn(2.0);
        when(meter.getFiveMinuteRate()).thenReturn(3.0);
        when(meter.getFifteenMinuteRate()).thenReturn(4.0);
        when(meter.getMeanRate()).thenReturn(5.0);

        filteredReporter.report(this.<Gauge>map(), this.<Counter>map(), this.<Histogram>map(), this.map("filteredMeter", meter), this.<Timer>map());

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        assertThat(point.getMeasurement()).isEqualTo("filteredMeter");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(1);
        assertThat(point.getFields()).contains(entry("m1_rate", 2.0));
        assertThat(point.getTags()).containsEntry("metricName", "filteredMeter");
    }

    @Test
    public void shouldMapMeasurementToDefinedMeasurementNameAndRegex() {
        Map<String, String> measurementMappings = new HashMap<String, String>();
        measurementMappings.put("resources", ".*resources.*");

        final InfluxDbReporter reporter = InfluxDbReporter
            .forRegistry(registry)
            .measurementMappings(measurementMappings)
            .build(influxDb);

        reporter.report(
            this.<Gauge>map(),
            this.<Counter>map(),
            this.<Histogram>map(),
            this.map("com.example.resources.RandomResource", mock(Meter.class)),
            this.<Timer>map()
        );

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        assertThat(point.getMeasurement()).isEqualTo("resources");
        assertThat(point.getTags()).containsEntry("metricName", "com.example.resources.RandomResource");
    }

    @Test
    public void shouldNotMapMeasurementToDefinedMeasurementNameAndRegex() {
        Map<String, String> measurementMappings = new HashMap<String, String>();
        measurementMappings.put("health", ".*health.*");

        final InfluxDbReporter reporter = InfluxDbReporter
            .forRegistry(registry)
            .measurementMappings(measurementMappings)
            .build(influxDb);

        reporter.report(
            this.<Gauge>map(),
            this.<Counter>map(),
            this.<Histogram>map(),
            this.map("com.example.resources.RandomResource", mock(Meter.class)),
            this.<Timer>map()
        );

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        assertThat(point.getMeasurement()).isEqualTo("com.example.resources.RandomResource");
        assertThat(point.getTags()).containsEntry("metricName", "com.example.resources.RandomResource");
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowRuntimeExceptionWhenIncorrectMeasurementRegex() {
        Map<String, String> measurementMappings = new HashMap<String, String>();
        measurementMappings.put("health", ".**.*");

        InfluxDbReporter
            .forRegistry(registry)
            .measurementMappings(measurementMappings)
            .build(influxDb);
    }

    @Test
    public void shouldRewriteMetricNames() {
        final InfluxDbReporter reporter = InfluxDbReporter
            .forRegistry(registry)
            .metricNameRewriter(String::toUpperCase)
            .build(influxDb);

        reporter.report(
            this.<Gauge>map(),
            this.<Counter>map(),
            this.<Histogram>map(),
            this.map("com.example.resources.RandomResource", mock(Meter.class)),
            this.<Timer>map()
        );

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        assertThat(point.getTags().get("metricName")).isEqualTo("COM.EXAMPLE.RESOURCES.RANDOMRESOURCE");
    }

    @Test
    public void reportsTimers() throws Exception {

        InfluxDbReporter filteredReporter = InfluxDbReporter
            .forRegistry(registry)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .filter(MetricFilter.ALL)
            .groupGauges(true)
            .includeTimerFields(Sets.newSet("m1_rate"))
            .build(influxDb);

        final Timer timer = mock(Timer.class);
        when(timer.getCount()).thenReturn(1L);
        when(timer.getMeanRate()).thenReturn(2.0);
        when(timer.getOneMinuteRate()).thenReturn(3.0);
        when(timer.getFiveMinuteRate()).thenReturn(4.0);
        when(timer.getFifteenMinuteRate()).thenReturn(5.0);

        final Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getMin()).thenReturn(TimeUnit.MILLISECONDS.toNanos(100));
        when(snapshot.getMean()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(200));
        when(snapshot.getMax()).thenReturn(TimeUnit.MILLISECONDS.toNanos(300));
        when(snapshot.getStdDev()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(400));
        when(snapshot.getMedian()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(500));
        when(snapshot.get75thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(600));
        when(snapshot.get95thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(700));
        when(snapshot.get98thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(800));
        when(snapshot.get99thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(900));
        when(snapshot.get999thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(1000));

        when(timer.getSnapshot()).thenReturn(snapshot);

        filteredReporter.report(this.<Gauge>map(), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), map("filteredTimer", timer));

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        assertThat(point.getMeasurement()).isEqualTo("filteredTimer");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(1);
        assertThat(point.getFields()).contains(entry("m1_rate", 3.0));
        assertThat(point.getTags()).containsEntry("metricName", "filteredTimer");
    }

    @Test
    public void reportsIncludedTimers() throws Exception {
        final Timer timer = mock(Timer.class);
        when(timer.getCount()).thenReturn(1L);
        when(timer.getMeanRate()).thenReturn(2.0);
        when(timer.getOneMinuteRate()).thenReturn(3.0);
        when(timer.getFiveMinuteRate()).thenReturn(4.0);
        when(timer.getFifteenMinuteRate()).thenReturn(5.0);

        final Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getMin()).thenReturn(TimeUnit.MILLISECONDS.toNanos(100));
        when(snapshot.getMean()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(200));
        when(snapshot.getMax()).thenReturn(TimeUnit.MILLISECONDS.toNanos(300));
        when(snapshot.getStdDev()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(400));
        when(snapshot.getMedian()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(500));
        when(snapshot.get75thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(600));
        when(snapshot.get95thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(700));
        when(snapshot.get98thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(800));
        when(snapshot.get99thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(900));
        when(snapshot.get999thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(1000));

        when(timer.getSnapshot()).thenReturn(snapshot);

        reporter.report(this.<Gauge>map(), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), map("timer", timer));

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        assertThat(point.getMeasurement()).isEqualTo("timer");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(15);
        assertThat(point.getFields()).contains(entry("count", 1L));
        assertThat(point.getFields()).contains(entry("mean_rate", 2.0));
        assertThat(point.getFields()).contains(entry("m1_rate", 3.0));
        assertThat(point.getFields()).contains(entry("m5_rate", 4.0));
        assertThat(point.getFields()).contains(entry("m15_rate", 5.0));
        assertThat(point.getFields()).contains(entry("min", 100.0));
        assertThat(point.getFields()).contains(entry("mean", 200.0));
        assertThat(point.getFields()).contains(entry("max", 300.0));
        assertThat(point.getFields()).contains(entry("stddev", 400.0));
        assertThat(point.getFields()).contains(entry("p50", 500.0));
        assertThat(point.getFields()).contains(entry("p75", 600.0));
        assertThat(point.getFields()).contains(entry("p95", 700.0));
        assertThat(point.getFields()).contains(entry("p98", 800.0));
        assertThat(point.getFields()).contains(entry("p99", 900.0));
        assertThat(point.getFields()).contains(entry("p999", 1000.0));
        assertThat(point.getTags()).containsEntry("metricName", "timer");
    }

    @Test
    public void reportsIntegerGaugeValues() throws Exception {
        reporter.report(map("gauge", gauge(1)), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        assertThat(point.getMeasurement()).isEqualTo("gauge");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(1);
        assertThat(point.getFields()).contains(entry("value", 1));
        assertThat(point.getTags()).containsEntry("metricName", "gauge");
    }

    @Test
    public void reportsLongGaugeValues() throws Exception {
        reporter.report(map("gauge", gauge(1L)), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        assertThat(point.getMeasurement()).isEqualTo("gauge");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(1);
        assertThat(point.getFields()).contains(entry("value", 1L));
        assertThat(point.getTags()).containsEntry("metricName", "gauge");
    }

    @Test
    public void reportsFloatGaugeValues() throws Exception {
        reporter.report(map("gauge", gauge(1.1f)), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        assertThat(point.getMeasurement()).isEqualTo("gauge");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(1);
        assertThat(point.getFields()).contains(entry("value", 1.1f));
        assertThat(point.getTags()).containsEntry("metricName", "gauge");
    }

    @Test
    public void reportsDoubleGaugeValues() throws Exception {
        reporter.report(map("gauge", gauge(1.1)), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        assertThat(point.getMeasurement()).isEqualTo("gauge");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(1);
        assertThat(point.getFields()).contains(entry("value", 1.1));
        assertThat(point.getTags()).containsEntry("metricName", "gauge");
    }

    @Test
    public void reportsByteGaugeValues() throws Exception {
        reporter
            .report(map("gauge", gauge((byte) 1)), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        assertThat(point.getMeasurement()).isEqualTo("gauge");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(1);
        assertThat(point.getFields()).contains(entry("value", (byte) 1));
        assertThat(point.getTags()).containsEntry("metricName", "gauge");
    }

    @Test
    public void shouldSkipIdleMetrics() throws Exception {
        when(influxDb.hasSeriesData()).thenReturn(true);

        final Meter meter = mock(Meter.class);
        when(meter.getCount()).thenReturn(1L);
        when(meter.getOneMinuteRate()).thenReturn(2.0);
        when(meter.getFiveMinuteRate()).thenReturn(3.0);
        when(meter.getFifteenMinuteRate()).thenReturn(4.0);
        when(meter.getMeanRate()).thenReturn(5.0);

        InfluxDbReporter skippingReporter = InfluxDbReporter
            .forRegistry(registry)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .filter(MetricFilter.ALL)
            .withTags(globalTags)
            .skipIdleMetrics(true)
            .build(influxDb);

        skippingReporter.report(this.<Gauge>map(), this.<Counter>map(), this.<Histogram>map(), this.map("meter", meter), this.<Timer>map());
        skippingReporter.report(this.<Gauge>map(), this.<Counter>map(), this.<Histogram>map(), this.map("meter", meter), this.<Timer>map());

        verify(influxDb, times(1)).appendPoints(Mockito.any(InfluxDbPoint.class));
    }

    @Test
    public void shouldCatchExceptions() throws Exception {
        doThrow(ConnectException.class).when(influxDb).flush();
        reporter
        .report(map("gauge", gauge((byte) 1)), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());
        doThrow(IOException.class).when(influxDb).flush();
        reporter
        .report(map("gauge", gauge((byte) 1)), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());
    }

    private <T> SortedMap<String, T> map() {
        return new TreeMap<String, T>();
    }

    private <T> SortedMap<String, T> map(String name, T metric) {
        final TreeMap<String, T> map = new TreeMap<String, T>();
        map.put(name, metric);
        return map;
    }

    private <T> Gauge gauge(T value) {
        final Gauge gauge = mock(Gauge.class);
        when(gauge.getValue()).thenReturn(value);
        return gauge;
    }
}
