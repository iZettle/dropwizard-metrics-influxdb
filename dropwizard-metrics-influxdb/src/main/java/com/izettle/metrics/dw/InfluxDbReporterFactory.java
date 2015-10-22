package com.izettle.metrics.dw;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.izettle.metrics.influxdb.InfluxDbHttpSender;
import com.izettle.metrics.influxdb.InfluxDbReporter;
import io.dropwizard.metrics.BaseReporterFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;

/**
 * A factory for {@link InfluxDbReporter} instances.
 * <p/>
 * <b>Configuration Parameters:</b>
 * <table>
 *     <tr>
 *         <td>Name</td>
 *         <td>Default</td>
 *         <td>Description</td>
 *     </tr>
 *     <tr>
 *         <td>protocol</td>
 *         <td>http</td>
 *         <td>The protocol (http or https) of the InfluxDb server to report to.</td>
 *     </tr>
 *     <tr>
 *         <td>host</td>
 *         <td>localhost</td>
 *         <td>The hostname of the InfluxDb server to report to.</td>
 *     </tr>
 *     <tr>
 *         <td>port</td>
 *         <td>8086</td>
 *         <td>The port of the InfluxDb server to report to.</td>
 *     </tr>
 *     <tr>
 *         <td>prefix</td>
 *         <td><i>None</i></td>
 *         <td>The prefix for Metric key names to report to InfluxDb.</td>
 *     </tr>
 *     <tr>
 *         <td>tags</td>
 *         <td><i>None</i></td>
 *         <td>tags for all metrics reported to InfluxDb.</td>
 *     </tr>
 *     <tr>
 *         <td>fields</td>
 *         <td>timers = p50, p99, m1_rate; meters = m1_rate</td>
 *         <td>fields by metric type to reported to InfluxDb.</td>
 *     </tr>
 *     <tr>
 *         <td>database</td>
 *         <td><i>None</i></td>
 *         <td>The database that metrics will be reported to InfluxDb.</td>
 *     </tr>
 *     <tr>
 *         <td>auth</td>
 *         <td><i>None</i></td>
 *         <td>An auth string of format username:password to authenticate with when reporting to InfluxDb.</td>
 *     </tr>
 *     <tr>
 *         <td>groupGauges</td>
 *         <td><i>None</i></td>
 *         <td>A boolean to signal whether to group gauges when reporting to InfluxDb.</td>
 *     </tr>
 *     <tr>
 *         <td>measurementMappings</td>
 *         <td><i>None</i></td>
 *         <td>A map for measurement mappings to be added, overridden or removed from the defaultMeasurementMappings.</td>
 *     </tr>
 *     <tr>
 *         <td>defaultMeasurementMappings</td>
 *         <td><i>None</i></td>
 *         <td>A map with default measurement mappings.</td>
 *     </tr>
 * </table>
 */
@JsonTypeName("influxdb")
public class InfluxDbReporterFactory extends BaseReporterFactory {
    @NotEmpty
    private String protocol = "http";

    @NotEmpty
    private String host = "localhost";

    @Range(min = 0, max = 49151)
    private int port = 8086;

    @NotNull
    private String prefix = "";

    @NotNull
    private Map<String, String> tags = new HashMap<>();

    @NotEmpty
    private ImmutableMap<String, ImmutableSet<String>> fields = ImmutableMap.of(
        "timers",
        ImmutableSet.of("p50", "p99", "m1_rate"),
        "meters",
        ImmutableSet.of("m1_rate"));

    @NotNull
    private String database = "";

    @NotNull
    private String auth = "";

    @NotNull
    private TimeUnit precision = TimeUnit.MINUTES;

    private boolean groupGauges = true;

    private Map<String, String> measurementMappings;

    private Map<String, String> defaultMeasurementMappings = ImmutableMap.<String, String>builder()
        .put("health", "*.health.*")
        .put("dao", "*.(jdbi|dao).*")
        .put("resources", "*.resources.*")
        .put("datasources", "io.dropwizard.db.ManagedPooledDataSource.*")
        .put("clients", "org.apache.http.client.HttpClient.*")
        .put("connections", "org.eclipse.jetty.server.HttpConnectionFactory.*")
        .put("thread-pools", "org.eclipse.jetty.util.thread.QueuedThreadPool.*")
        .put("logs", "ch.qos.logback.core.Appender.*")
        .put("http-server", "io.dropwizard.jetty.MutableServletContextHandler.*")
        .put("raw-sql", "org.skife.jdbi.v2.DBI.raw-sql")
        .build();

    @JsonProperty
    public String getProtocol() {
        return protocol;
    }

    @JsonProperty
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @JsonProperty
    public String getHost() {
        return host;
    }

    @JsonProperty
    public void setHost(String host) {
        this.host = host;
    }

    @JsonProperty
    public int getPort() {
        return port;
    }

    @JsonProperty
    public void setPort(int port) {
        this.port = port;
    }

    @JsonProperty
    public String getPrefix() {
        return prefix;
    }

    @JsonProperty
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @JsonProperty
    private Map<String, String> getTags() {
        return tags;
    }

    @JsonProperty
    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    @JsonProperty
    public ImmutableMap<String, ImmutableSet<String>> getFields() {
        return fields;
    }

    @JsonProperty
    public void setFields(ImmutableMap<String, ImmutableSet<String>> fields) {
        this.fields = fields;
    }

    @JsonProperty
    public String getDatabase() {
        return database;
    }

    @JsonProperty
    public void setDatabase(String database) {
        this.database = database;
    }

    @JsonProperty
    public String getAuth() {
        return auth;
    }

    @JsonProperty
    public void setAuth(String auth) {
        this.auth = auth;
    }

    @JsonProperty
    public boolean getGroupGauges() {
        return groupGauges;
    }

    @JsonProperty
    public void setGroupGauges(boolean groupGauges) {
        this.groupGauges = groupGauges;
    }

    @JsonProperty
    public TimeUnit getPrecision() {
        return precision;
    }

    @JsonProperty
    public void setPrecision(TimeUnit precision) {
        this.precision = precision;
    }

    @JsonProperty
    public Map<String, String> getMeasurementMappings() {
        return measurementMappings;
    }

    @JsonProperty
    public void setMeasurementMappings(Map<String, String> measurementMappings) {
        if (measurementMappings == null) {
            this.measurementMappings = Collections.emptyMap();
        } else {
            this.measurementMappings = measurementMappings;
        }
    }

    @JsonProperty
    public Map<String, String> getDefaultMeasurementMappings() {
        return defaultMeasurementMappings;
    }

    @Override
    public ScheduledReporter build(MetricRegistry registry) {
        try {
            return builder(registry).build(new InfluxDbHttpSender(protocol, host, port, database, auth, precision));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> buildMeasurementMappings() {
        Map<String, String> mappings = new HashMap<>(defaultMeasurementMappings);

        for (Map.Entry<String, String> entry : measurementMappings.entrySet()) {
            String mappingKey = entry.getKey();
            String mappingValue = entry.getValue();

            if (mappingValue.isEmpty()) {
                mappings.remove(mappingKey);
                continue;
            }

            mappings.put(mappingKey, mappingValue);
        }

        return mappings;
    }

    @VisibleForTesting
    protected InfluxDbReporter.Builder builder(MetricRegistry registry) {
        return InfluxDbReporter.forRegistry(registry)
            .convertDurationsTo(getDurationUnit())
            .convertRatesTo(getRateUnit())
            .includeMeterFields(fields.get("meters"))
            .includeTimerFields(fields.get("timers"))
            .filter(getFilter())
            .groupGauges(getGroupGauges())
            .withTags(getTags())
            .measurementMappings(buildMeasurementMappings());
    }
}
