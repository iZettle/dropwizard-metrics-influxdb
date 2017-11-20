package com.izettle.metrics.influxdb;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Counting;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.izettle.metrics.influxdb.data.InfluxDbPoint;
import java.net.ConnectException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.izettle.metrics.influxdb.tags.NoopTransformer;
import com.izettle.metrics.influxdb.tags.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InfluxDbReporter extends ScheduledReporter {
    public static class Builder {
        private final MetricRegistry registry;
        private Map<String, String> tags;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;
        private boolean skipIdleMetrics;
        private boolean groupGauges;
        private Set<String> includeTimerFields;
        private Set<String> includeMeterFields;
        private Map<String, Pattern> measurementMappings;
        private Transformer tagsTransformer;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.tags = null;
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
        }

        /**
         * Add these tags to all metrics.
         *
         * @param tags a map containing tags common to all metrics
         * @return {@code this}
         */
        public Builder withTags(Map<String, String> tags) {
            this.tags = Collections.unmodifiableMap(tags);
            return this;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit a unit of time
         * @return {@code this}
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit a unit of time
         * @return {@code this}
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter a {@link MetricFilter}
         * @return {@code this}
         */
        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Only report metrics that have changed.
         *
         * @param skipIdleMetrics true/false for skipping metrics not reported
         * @return {@code this}
         */
        public Builder skipIdleMetrics(boolean skipIdleMetrics) {
            this.skipIdleMetrics = skipIdleMetrics;
            return this;
        }

        /**
         * Group gauges by metric name with field names as everything after the last period
         * <p>
         * If there is no `.', field name will be `value'. If the metric name terminates in a `.' field name will be empty.
         * </p>
         *
         * @param groupGauges true/false for whether to group gauges or not
         * @return {@code this}
         */
        public Builder groupGauges(boolean groupGauges) {
            this.groupGauges = groupGauges;
            return this;
        }

        /**
         * Only report timer fields in the set.
         *
         * @param fields Fields to include.
         * @return {@code this}
         */
        public Builder includeTimerFields(Set<String> fields) {
            this.includeTimerFields = fields;
            return this;
        }

        /**
         * Only report meter fields in the set.
         *
         * @param fields Fields to include.
         * @return {@code this}
         */
        public Builder includeMeterFields(Set<String> fields) {
            this.includeMeterFields = fields;
            return this;
        }

        /**
         * Map measurement to a defined measurement name, where the key is the measurement name
         * and the value is the reqex the measurement should be mapped by.
         *
         * @param measurementMappings
         * @return {@code this}
         */
        public Builder measurementMappings(Map<String, String> measurementMappings) {
            Map<String, Pattern> mappingsByPattern = new HashMap<String, Pattern>();

            for (Map.Entry<String, String> entry : measurementMappings.entrySet()) {
                try {
                    final Pattern pattern = Pattern.compile(entry.getValue());
                    mappingsByPattern.put(entry.getKey(), pattern);
                } catch (PatternSyntaxException e) {
                    throw new RuntimeException("Could not compile regex: " + entry.getValue(), e);
                }
            }

            this.measurementMappings = mappingsByPattern;
            return this;
        }

        public Builder tagsTransformer(Transformer tagsTransformer) {
            this.tagsTransformer = tagsTransformer;
            return this;
        }

        public InfluxDbReporter build(final InfluxDbSender influxDb) {
            return new InfluxDbReporter(
                registry, influxDb, tags, rateUnit, durationUnit, filter, skipIdleMetrics,
                groupGauges, includeTimerFields, includeMeterFields, measurementMappings, tagsTransformer
            );
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(InfluxDbReporter.class);
    private final InfluxDbSender influxDb;
    private final boolean skipIdleMetrics;
    private final Map<String, Long> previousValues;
    private final boolean groupGauges;
    private final Set<String> includeTimerFields;
    private final Set<String> includeMeterFields;
    private final Map<String, Pattern> measurementMappings;
    private final Transformer tagsTransformer;

    private InfluxDbReporter(
        final MetricRegistry registry,
        final InfluxDbSender influxDb,
        final Map<String, String> tags,
        final TimeUnit rateUnit,
        final TimeUnit durationUnit,
        final MetricFilter filter,
        final boolean skipIdleMetrics,
        final boolean groupGauges,
        final Set<String> includeTimerFields,
        final Set<String> includeMeterFields,
        final Map<String, Pattern> measurementMappings,
        final Transformer tagsTransformer
    ) {
        super(registry, "influxDb-reporter", filter, rateUnit, durationUnit);
        influxDb.setTags(tags);
        this.influxDb = influxDb;
        this.skipIdleMetrics = skipIdleMetrics;
        this.groupGauges = groupGauges;
        this.includeTimerFields = includeTimerFields;
        this.includeMeterFields = includeMeterFields;
        this.previousValues = new TreeMap<String, Long>();
        this.measurementMappings =
            measurementMappings == null ? Collections.<String, Pattern>emptyMap() : measurementMappings;
        this.tagsTransformer =
            tagsTransformer == null ? new NoopTransformer() : tagsTransformer;
    }

    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    @Override
    public void report(
        final SortedMap<String, Gauge> gauges,
        final SortedMap<String, Counter> counters,
        final SortedMap<String, Histogram> histograms,
        final SortedMap<String, Meter> meters,
        final SortedMap<String, Timer> timers) {
        final long now = System.currentTimeMillis();

        try {
            influxDb.flush();

            reportGauges(gauges, now);

            for (Map.Entry<String, Counter> entry : counters.entrySet()) {
                reportCounter(entry.getKey(), entry.getValue(), now);
            }

            for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
                reportHistogram(entry.getKey(), entry.getValue(), now);
            }

            for (Map.Entry<String, Meter> entry : meters.entrySet()) {
                reportMeter(entry.getKey(), entry.getValue(), now);
            }

            for (Map.Entry<String, Timer> entry : timers.entrySet()) {
                reportTimer(entry.getKey(), entry.getValue(), now);
            }

            if (influxDb.hasSeriesData()) {
                influxDb.writeData();
            }
        } catch (ConnectException e) {
            LOGGER.warn("Unable to connect to InfluxDB. Discarding data.");
        } catch (Exception e) {
            LOGGER.warn("Unable to report to InfluxDB with error '{}'. Discarding data.", e.getMessage());
        }
    }

    private void reportGauges(SortedMap<String, Gauge> gauges, long now) {
        if (groupGauges) {
            Map<String, Map<String, Gauge>> groupedGauges = groupGauges(gauges);
            for (Map.Entry<String, Map<String, Gauge>> entry : groupedGauges.entrySet()) {
                reportGaugeGroup(entry.getKey(), entry.getValue(), now);
            }
        } else {
            for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
                reportGauge(entry.getKey(), entry.getValue(), now);
            }
        }
    }

    private Map<String, Map<String, Gauge>> groupGauges(SortedMap<String, Gauge> gauges) {
        Map<String, Map<String, Gauge>> groupedGauges = new HashMap<String, Map<String, Gauge>>();
        for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
            final String metricName;
            final String fieldName;
            int lastDotIndex = entry.getKey().lastIndexOf(".");
            if (lastDotIndex != -1) {
                metricName = entry.getKey().substring(0, lastDotIndex);
                fieldName = entry.getKey().substring(lastDotIndex + 1);
            } else {
                // no `.` to group by in the metric name, just report the metric as is
                metricName = entry.getKey();
                fieldName = "value";
            }
            Map<String, Gauge> fields = groupedGauges.get(metricName);

            if (fields == null) {
                fields = new HashMap<String, Gauge>();
            }
            fields.put(fieldName, entry.getValue());
            groupedGauges.put(metricName, fields);
        }
        return groupedGauges;
    }

    private void reportGaugeGroup(String name, Map<String, Gauge> gaugeGroup, long now) {
        Map<String, Object> fields = new HashMap<String, Object>();
        for (Map.Entry<String, Gauge> entry : gaugeGroup.entrySet()) {
            Object gaugeValue = sanitizeGauge(entry.getValue().getValue());
            if (gaugeValue != null) {
                fields.put(entry.getKey(), gaugeValue);
            }
        }

        if (!fields.isEmpty()) {
            influxDb.appendPoints(
                new InfluxDbPoint(
                    getMeasurementName(name),
                    getTags(name),
                    now,
                    fields));
        }
    }

    /**
     * InfluxDB does not like "NaN" for number fields, use null instead
     *
     * @param value the value to sanitize
     * @return value, or null if value is a number and is finite
     */
    private Object sanitizeGauge(Object value) {
        final Object finalValue;
        if (value instanceof Double && (Double.isInfinite((Double) value) || Double.isNaN((Double) value))) {
            finalValue = null;
        } else if (value instanceof Float && (Float.isInfinite((Float) value) || Float.isNaN((Float) value))) {
            finalValue = null;
        } else {
            finalValue = value;
        }
        return finalValue;
    }

    private void reportTimer(String name, Timer timer, long now) {
        if (canSkipMetric(name, timer)) {
            return;
        }
        final Snapshot snapshot = timer.getSnapshot();
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("count", timer.getCount());
        fields.put("min", convertDuration(snapshot.getMin()));
        fields.put("max", convertDuration(snapshot.getMax()));
        fields.put("mean", convertDuration(snapshot.getMean()));
        fields.put("stddev", convertDuration(snapshot.getStdDev()));
        fields.put("p50", convertDuration(snapshot.getMedian()));
        fields.put("p75", convertDuration(snapshot.get75thPercentile()));
        fields.put("p95", convertDuration(snapshot.get95thPercentile()));
        fields.put("p98", convertDuration(snapshot.get98thPercentile()));
        fields.put("p99", convertDuration(snapshot.get99thPercentile()));
        fields.put("p999", convertDuration(snapshot.get999thPercentile()));
        fields.put("m1_rate", convertRate(timer.getOneMinuteRate()));
        fields.put("m5_rate", convertRate(timer.getFiveMinuteRate()));
        fields.put("m15_rate", convertRate(timer.getFifteenMinuteRate()));
        fields.put("mean_rate", convertRate(timer.getMeanRate()));

        if (includeTimerFields != null) {
            fields.keySet().retainAll(includeTimerFields);
        }

        influxDb.appendPoints(
            new InfluxDbPoint(
                getMeasurementName(name),
                getTags(name),
                now,
                fields));
    }

    private void reportHistogram(String name, Histogram histogram, long now) {
        if (canSkipMetric(name, histogram)) {
            return;
        }
        final Snapshot snapshot = histogram.getSnapshot();
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("count", histogram.getCount());
        fields.put("min", snapshot.getMin());
        fields.put("max", snapshot.getMax());
        fields.put("mean", snapshot.getMean());
        fields.put("stddev", snapshot.getStdDev());
        fields.put("p50", snapshot.getMedian());
        fields.put("p75", snapshot.get75thPercentile());
        fields.put("p95", snapshot.get95thPercentile());
        fields.put("p98", snapshot.get98thPercentile());
        fields.put("p99", snapshot.get99thPercentile());
        fields.put("p999", snapshot.get999thPercentile());

        influxDb.appendPoints(
            new InfluxDbPoint(
                getMeasurementName(name),
                getTags(name),
                now,
                fields));
    }

    private void reportCounter(String name, Counter counter, long now) {
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("count", counter.getCount());

        influxDb.appendPoints(
            new InfluxDbPoint(
                getMeasurementName(name),
                getTags(name),
                now,
                fields));
    }

    private void reportGauge(String name, Gauge<?> gauge, long now) {
        Map<String, Object> fields = new HashMap<String, Object>();
        Object sanitizeGauge = sanitizeGauge(gauge.getValue());
        if (sanitizeGauge != null) {

            fields.put("value", sanitizeGauge);
            influxDb.appendPoints(
                new InfluxDbPoint(
                    getMeasurementName(name),
                    getTags(name),
                    now,
                    fields));
        }
    }

    private void reportMeter(String name, Metered meter, long now) {
        if (canSkipMetric(name, meter)) {
            return;
        }
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("count", meter.getCount());
        fields.put("m1_rate", convertRate(meter.getOneMinuteRate()));
        fields.put("m5_rate", convertRate(meter.getFiveMinuteRate()));
        fields.put("m15_rate", convertRate(meter.getFifteenMinuteRate()));
        fields.put("mean_rate", convertRate(meter.getMeanRate()));

        if (includeMeterFields != null) {
            fields.keySet().retainAll(includeMeterFields);
        }

        influxDb.appendPoints(
            new InfluxDbPoint(
                getMeasurementName(name),
                getTags(name),
                now,
                fields));
    }

    private boolean canSkipMetric(String name, Counting counting) {
        boolean isIdle = (calculateDelta(name, counting.getCount()) == 0);
        if (skipIdleMetrics && !isIdle) {
            previousValues.put(name, counting.getCount());
        }
        return skipIdleMetrics && isIdle;
    }

    private long calculateDelta(String name, long count) {
        Long previous = previousValues.get(name);
        if (previous == null) {
            return -1;
        }
        if (count < previous) {
            LOGGER.warn("Saw a non-monotonically increasing value for metric '{}'", name);
            return 0;
        }
        return count - previous;
    }

    private Map<String, String> getTags(String name) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.putAll(influxDb.getTags());
        tags.putAll(tagsTransformer.getTags(name));
        return tags;
    }

    private String getMeasurementName(final String name) {
        for (Map.Entry<String, Pattern> entry : measurementMappings.entrySet()) {
            final Pattern pattern = entry.getValue();

            if (pattern.matcher(name).matches()) {
                return entry.getKey();
            }
        }
        return name;
    }

}
