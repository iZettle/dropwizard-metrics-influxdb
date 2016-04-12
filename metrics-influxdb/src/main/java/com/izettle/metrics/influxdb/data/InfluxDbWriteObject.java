package com.izettle.metrics.influxdb.data;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This class contains the request object to be sent to InfluxDb for writing. It contains a collection of points.
 */
public class InfluxDbWriteObject {

    private String database;

    private TimeUnit precision;

    private Set<InfluxDbPoint> points;

    private Map<String, String> tags = Collections.emptyMap();

    public InfluxDbWriteObject(final String database, final TimeUnit precision) {
        this.points = new HashSet<InfluxDbPoint>();
        this.database = database;
        this.precision = precision;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public TimeUnit getPrecision() {
        return precision;
    }

    public void setPrecision(TimeUnit precision) {
        this.precision = precision;
    }

    public Set<InfluxDbPoint> getPoints() {
        return points;
    }

    public void setPoints(Set<InfluxDbPoint> points) {
        this.points = points;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = Collections.unmodifiableMap(tags);
    }
}
