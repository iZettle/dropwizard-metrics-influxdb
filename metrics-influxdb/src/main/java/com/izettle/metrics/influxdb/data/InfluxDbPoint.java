package com.izettle.metrics.influxdb.data;

import java.util.Collections;
import java.util.Map;

/**
 * This class is a bean that holds time series data of a point. A point co relates to a metric.
 */
public class InfluxDbPoint {
    private String measurement;
    private Map<String, String> tags = Collections.emptyMap();
    private Long time;
    private Map<String, Object> fields = Collections.emptyMap();

    public InfluxDbPoint(
        final String measurement,
        final Long time,
        final Map<String, Object> fields) {
        this.measurement = measurement;
        this.time = time;
        if (fields != null) {
            this.fields = Collections.unmodifiableMap(fields);
        }

    }

    public InfluxDbPoint(
        String measurement,
        Map<String, String> tags,
        Long time,
        Map<String, Object> fields) {
        this.measurement = measurement;
        if (tags != null) {
            this.tags = Collections.unmodifiableMap(tags);
        }
        this.time = time;
        if (fields != null) {
            this.fields = Collections.unmodifiableMap(fields);
        }
    }

    public String getMeasurement() {
        return measurement;
    }

    public void setMeasurement(String measurement) {
        this.measurement = measurement;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        if (tags != null) {
            this.tags = Collections.unmodifiableMap(tags);
        }
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        if (fields != null) {
            this.fields = Collections.unmodifiableMap(fields);
        }
    }
}
