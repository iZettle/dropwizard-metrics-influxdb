package com.izettle.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.metrics.BaseReporterFactory;
import io.dropwizard.metrics.influxdb.InfluxDbHttpSender;
import io.dropwizard.metrics.influxdb.InfluxDbReporter;
import io.dropwizard.util.Duration;
import java.util.HashMap;
import java.util.Map;
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

    @NotNull
    private String database = "";

    @NotNull
    private String auth = "";

    private boolean groupGauges;

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

    @Override
    public ScheduledReporter build(MetricRegistry registry) {
        try {
            return builder(registry).build(new InfluxDbHttpSender(protocol, host, port, database, auth));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    protected InfluxDbReporter.Builder builder(MetricRegistry registry) {
        return InfluxDbReporter.forRegistry(registry)
                .convertDurationsTo(getDurationUnit())
                .convertRatesTo(getRateUnit())
                .roundTimestampTo(getFrequency().or(Duration.minutes(1)).getUnit())
                .filter(getFilter())
                .groupGauges(getGroupGauges())
                .withTags(getTags());
    }
}
