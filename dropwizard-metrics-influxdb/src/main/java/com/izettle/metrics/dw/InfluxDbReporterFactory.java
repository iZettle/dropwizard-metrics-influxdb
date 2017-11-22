package com.izettle.metrics.dw;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.activation.UnsupportedDataTypeException;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.izettle.metrics.influxdb.InfluxDBKafkaSender;
import com.izettle.metrics.influxdb.InfluxDbHttpSender;
import com.izettle.metrics.influxdb.InfluxDbLoggerSender;
import com.izettle.metrics.influxdb.InfluxDbReporter;
import com.izettle.metrics.influxdb.InfluxDbTcpSender;
import com.izettle.metrics.influxdb.InfluxDbUdpSender;
import com.izettle.metrics.dw.tags.ClassBasedTransformer;
import com.izettle.metrics.dw.tags.Transformer;
import io.dropwizard.metrics.BaseReporterFactory;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.ValidationMethod;

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
 *         <td>senderType</td>
 *         <td>http</td>
 *         <td>The sender type (http, tcp, udp, logger, or kafka) used to send metrics to the InfluxDb server.</td>
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
 *         <td>The prefix for Metric key names (measurement) to report to InfluxDb.</td>
 *     </tr>
 *     <tr>
 *         <td>tags</td>
 *         <td><i>None</i></td>
 *         <td>tags for all metrics reported to InfluxDb.</td>
 *     </tr>
 *     <tr>
 *         <td>fields</td>
 *         <td>timers = p50, p75, p95, p99, p999, m1_rate<br>meters = m1_rate</td>
 *         <td>fields by metric type to reported to InfluxDb.</td>
 *     </tr>
 *     <tr>
 *         <td>database</td>
 *         <td><i>None</i></td>
 *         <td>The database that metrics will be reported to InfluxDb.</td>
 *     </tr>
 *     <tr>
 *         <td>precision</td>
 *         <td>1m</td>
 *         <td>The precision of timestamps. Does not take into account the quantity, so for example `5m` will be minute precision</td>
 *     </tr>
 *     <tr>
 *         <td>connectTimeout</td>
 *         <td>1500</td>
 *         <td>The connect timeout in milliseconds for connecting to InfluxDb.</td>
 *     </tr>
 *     <tr>
 *         <td>readTimeout</td>
 *         <td>1500</td>
 *         <td>The read timeout in milliseconds for reading from InfluxDb.</td>
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
 *         <td>
 *             health = .*\.health(\..*)?<br>
 *             auth = .*\.auth\..*<br>
 *             dao = *\.(jdbi|dao)\..*<br>
 *             resources = *\.resources?\..*<br>
 *             datasources = io\.dropwizard\.db\.ManagedPooledDataSource.*<br>
 *             clients = org\.apache\.http\.client\.HttpClient.*<br>
 *             client_connections = org\.apache\.http\.conn\.HttpClientConnectionManager.*<br>
 *             connections = org\.eclipse\.jetty\.server\.HttpConnectionFactory.*<br>
 *             thread_pools = org\.eclipse\.jetty\.util\.thread\.QueuedThreadPool.*<br>
 *             logs = ch\.qos\.logback\.core\.Appender.*<br>
 *             http_server = io\.dropwizard\.jetty\.MutableServletContextHandler.*<br>
 *             raw_sql = org\.skife\.jdbi\.v2\.DBI\.raw-sql<br>
 *             jvm = ^jvm$<br>
 *             jvm_attribute = jvm\.attribute.*?<br>
 *             jvm_buffers = jvm\.buffers\..*<br>
 *             jvm_classloader = jvm\.classloader.*<br>
 *             jvm_gc = jvm\.gc\..*<br>
 *             jvm_memory = jvm\.memory\..*<br>
 *             jvm_threads = jvm\.threads.*<br>
 *             event_handlers = .*Handler.*
 *          </td>
 *         <td>A map with default measurement mappings.</td>
 *     </tr>
 *     <tr>
 *         <td>excludes</td>
 *         <td><i>defaultExcludes</i></tr>
 *         <td>A set of pre-calculated metrics like usage and percentage, and unchanging JVM
 *             metrics to exclude by default</td>
 *     </tr>
 *     <tr>
 *         <td>tagsTransformer</td>
 *         <td><i>ClassBased</i></tr>
 *         <td>A <code>JsonTypeName</code> for a class implementing the
 *         <code>com.izettle.metrics.dw.tags.Transformer</code> interface.</td>
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
        ImmutableSet.of("p50", "p75", "p95", "p99", "p999", "m1_rate"),
        "meters",
        ImmutableSet.of("m1_rate"));

    @NotNull
    private String database = "";

    @NotNull
    private String auth = "";

    @Range(min = 500, max = 30000)
    private int connectTimeout = 1500;

    @Range(min = 500, max = 30000)
    private int readTimeout = 1500;

    @NotNull
    private Duration precision = Duration.minutes(1);

    @NotNull
    private SenderType senderType = SenderType.HTTP;

    private boolean groupGauges = true;

    private ImmutableMap<String, String> measurementMappings = ImmutableMap.of();

    private ImmutableMap<String, String> defaultMeasurementMappings = ImmutableMap.<String, String>builder()
        .put("health", ".*\\.health(\\..*)?$")
        .put("auth", ".*\\.auth\\..*")
        .put("dao", ".*\\.(jdbi|dao)\\..*")
        .put("resources", ".*\\.resources?\\..*")
        .put("event_handlers", ".*\\.messaging\\..*")
        .put("datasources", "io\\.dropwizard\\.db\\.ManagedPooledDataSource.*")
        .put("clients", "org\\.apache\\.http\\.client\\.HttpClient.*")
        .put("client_connections", "org\\.apache\\.http\\.conn\\.HttpClientConnectionManager.*")
        .put("connections", "org\\.eclipse\\.jetty\\.server\\.HttpConnectionFactory.*")
        .put("thread_pools", "org\\.eclipse\\.jetty\\.util\\.thread\\.QueuedThreadPool.*")
        .put("logs", "ch\\.qos\\.logback\\.core\\.Appender.*")
        .put("http_server", "io\\.dropwizard\\.jetty\\.MutableServletContextHandler.*")
        .put("raw_sql", "org\\.skife\\.jdbi\\.v2\\.DBI\\.raw-sql")
        .put("jvm", "^jvm$")
        .put("jvm_attribute", "jvm\\.attribute.*?")
        .put("jvm_buffers", "jvm\\.buffers\\..*")
        .put("jvm_classloader", "jvm\\.classloader.*")
        .put("jvm_gc", "jvm\\.gc\\..*")
        .put("jvm_memory", "jvm\\.memory\\..*")
        .put("jvm_threads", "jvm\\.threads.*")
        .build();

    private ImmutableSet<String> excludes = ImmutableSet.<String>builder()
        .add("ch.qos.logback.core.Appender.debug")
        .add("ch.qos.logback.core.Appender.trace")
        .add("io.dropwizard.jetty.MutableServletContextHandler.percent-4xx-15m")
        .add("io.dropwizard.jetty.MutableServletContextHandler.percent-4xx-1m")
        .add("io.dropwizard.jetty.MutableServletContextHandler.percent-4xx-5m")
        .add("io.dropwizard.jetty.MutableServletContextHandler.percent-5xx-15m")
        .add("io.dropwizard.jetty.MutableServletContextHandler.percent-5xx-1m")
        .add("io.dropwizard.jetty.MutableServletContextHandler.percent-5xx-5m")
        .add("jvm.attribute.name")
        .add("jvm.attribute.vendor")
        .add("jvm.memory.heap.usage")
        .add("jvm.memory.non-heap.usage")
        .add("jvm.memory.pools.Code-Cache.usage")
        .add("jvm.memory.pools.Compressed-Class-Space.usage")
        .add("jvm.memory.pools.Metaspace.usage")
        .add("jvm.memory.pools.PS-Eden-Space.usage")
        .add("jvm.memory.pools.PS-Old-Gen.usage")
        .add("jvm.memory.pools.PS-Survivor-Space.usage")
        .build();

    @NotNull
    private Transformer tagsTransformer = new ClassBasedTransformer();

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
    public int getConnectTimeout() {
        return connectTimeout;
    }

    @JsonProperty
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    @JsonProperty
    public int getReadTimeout() {
        return readTimeout;
    }

    @JsonProperty
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
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
    public Duration getPrecision() {
        return precision;
    }

    @JsonProperty
    public void setPrecision(Duration precision) {
        this.precision = precision;
    }

    @JsonProperty
    public Map<String, String> getMeasurementMappings() {
        return measurementMappings;
    }

    @JsonProperty
    public void setMeasurementMappings(ImmutableMap<String, String> measurementMappings) {
        this.measurementMappings = measurementMappings;
    }

    @JsonProperty
    public Map<String, String> getDefaultMeasurementMappings() {
        return defaultMeasurementMappings;
    }

    @JsonProperty
    @Override
    public ImmutableSet<String> getExcludes() {
        return this.excludes;
    }

    @JsonProperty
    @Override
    public void setExcludes(ImmutableSet<String> excludes) {
        this.excludes = excludes;
    }

    @JsonProperty
    public void setDefaultMeasurementMappings(ImmutableMap<String, String> defaultMeasurementMappings) {
        this.defaultMeasurementMappings = defaultMeasurementMappings;
    }

    @JsonProperty
    public void setSenderType(SenderType senderType) {
        this.senderType = senderType;
    }

    @JsonProperty
    public SenderType getSenderType() {
        return senderType;
    }

    @JsonProperty
    public void setTagsTransformer(Transformer tagsTransformer) {
        this.tagsTransformer = tagsTransformer;
    }

    @JsonProperty
    public Transformer getTagsTransformer() {
        return tagsTransformer;
    }

    @Override
    public ScheduledReporter build(MetricRegistry registry) {
        try {
            InfluxDbReporter.Builder builder = builder(registry);

            switch (senderType) {
                case HTTP:
                    return builder.build(
                        new InfluxDbHttpSender(
                            protocol,
                            host,
                            port,
                            database,
                            auth,
                            precision.getUnit(),
                            connectTimeout,
                            readTimeout,
                            prefix
                        )
                    );
                case TCP:
                    return builder.build(
                        new InfluxDbTcpSender(
                            host,
                            port,
                            readTimeout,
                            database,
                            prefix
                        )
                    );
                case UDP:
                    return builder.build(
                        new InfluxDbUdpSender(
                            host,
                            port,
                            readTimeout,
                            database,
                            prefix
                        )
                    );
                case LOGGER:
                    return builder.build(
                        new InfluxDbLoggerSender(
                            database,
                            TimeUnit.MILLISECONDS,
                            prefix
                        )
                    );
                case KAFKA:
                        return builder.build(
                        new InfluxDBKafkaSender(
                            database,
                            TimeUnit.MILLISECONDS,
                            prefix
                        )
                     );
                default:
                    throw new UnsupportedDataTypeException("The Sender Type is not supported. ");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected Map<String, String> buildMeasurementMappings() {
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

    @ValidationMethod(message = "measurementMappings must be regular expressions")
    public boolean isMeasurementMappingRegularExpressions() {
        for (Map.Entry<String, String> entry : buildMeasurementMappings().entrySet()) {
            try {
                Pattern.compile(entry.getValue());
            } catch (PatternSyntaxException e) {
                return false;
            }
        }
        return true;
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
            .tagsTransformer(tagsTransformer)
            .measurementMappings(buildMeasurementMappings());
    }
}
