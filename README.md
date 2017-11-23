Dropwizard Metrics v3 InfluxDb Integration
==========================================
[![Build Status](https://travis-ci.org/iZettle/dropwizard-metrics-influxdb.svg?branch=master)](https://travis-ci.org/iZettle/dropwizard-metrics-influxdb)
[![Coverage Status](https://coveralls.io/repos/iZettle/dropwizard-metrics-influxdb/badge.svg?branch=master&service=github)](https://coveralls.io/github/iZettle/dropwizard-metrics-influxdb?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.izettle/metrics-influxdb/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.izettle/metrics-influxdb)

Support for
[InfluxDb v0.13](https://docs.influxdata.com/influxdb/v0.13/) for
[Dropwizard](http://www.dropwizard.io/) apps and
[Metrics v3.1](http://metrics.dropwizard.io/3.1.0/).

`metrics-influxdb` was copied and backported from
[Dropwizard metrics](https://github.com/dropwizard/metrics/tree/master/metrics-influxdb/src/main/java/io/dropwizard/metrics/influxdb)
which in turn may have started in
[this project](https://github.com/davidB/metrics-influxdb). Some extra features
have been added like field filtering and renaming metrics.

`dropwizard-metrics-influxdb` is a DropWizard metrics reporter factory for
building a scheduled reporter for `metrics-influxdb`.

## Usage

Add the dependency to your project:
```xml
<dependency>
    <groupId>com.izettle</groupId>
    <artifactId>dropwizard-metrics-influxdb</artifactId>
    <version>${dropwizard-metrics-influxdb.version}</version>
</dependency>
```

If you have a local influxdb running the bare minimum configuration to put in
your dropwizard apps yaml is:

```yaml
# Metrics reporting
metrics:
  reporters:
    - type: influxdb
      database: mynewdb
```

After starting your app with this configuration included you'll see some
measurements in influxdb:

    $ influx
    Connected to http://localhost:8086 version 0.9.4.2
    InfluxDB shell 0.9.4.2
    > use mynewdb
    Using database mynewdb
    > show measurements
    name: measurements
    ------------------
    auth
    client_connections
    clients
    connections
    dao
    datasources
    health
    http_server
    jvm
    jvm_buffers
    jvm_classloader
    jvm_gc
    jvm_memory
    jvm_threads
    logs
    raw_sql
    resources
    thread_pools

You'll likely want to report to a remote influxdb server though so a typical
configuration looks more like this:

```yaml
# Metrics reporting
metrics:
  reporters:
    - type: influxdb
      protocol: https
      host: myinfluxdbhost.com
      port: 8086
      database: mydb
      auth: myuser:mypassword
      tags:
        host: thehostname
        environment: staging
```

## Default configuration

The Dropwizard InfluxDb reporter factory comes with some sensible defaults. Some
of the defaults are for reducing storage requirements in the database (1m
precision reporting every minute and some default exclusions for
example). Others are to get a better fit for the influx model (gauge grouping
and measurement mapping).

### Measurement/Metric Mappings

Default metric naming that the Metrics library uses does not map particularly
well to the InfluxDB tag-based model. This is why we have added some sensible
default measurement mappings. For example, the defaults will map all `@Timed`
annotated resource methods whose fully qualified class name includes `resource`
to an influxdb measurement called `resources`. It will also tag the measurement
point with a tag `metricName` that contains the full metric name. The defaults
are for out-of-the-box Dropwizard metrics as well as the
[recommended](http://www.dropwizard.io/manual/core.html#organizing-your-project)
Dropwizard project layout.

### Tags

Tags for a metric are created by a class implementing the `Transform` interface
configured by `tagsTransformer`. By default the `ClassBasedTransformer` is used
and it creates tha following tags: `metricName`, `package`, `className`, and
`method`.

### Gauge Grouping

Gauge grouping is enabled by default. This will turn a set of metrics into a
measurement with separate fields like so:


    "org.eclipse.jetty.util.thread.QueuedThreadPool.dw.jobs" : {
      "value" : 0
    },
    "org.eclipse.jetty.util.thread.QueuedThreadPool.dw.size" : {
      "value" : 8
    },
    "org.eclipse.jetty.util.thread.QueuedThreadPool.dw.utilization" : {
      "value" : 0.375
    },
    "org.eclipse.jetty.util.thread.QueuedThreadPool.dw.utilization-max" : {
      "value" : 0.0029296875
    }

will be grouped to one measurement `org.eclipse.jetty.util.thread.QueuedThreadPool.dw`:

```
name: org.eclipse.jetty.util.thread.QueuedThreadPool.dw
-------------------------------------------------------
time                    jobs    size    utilization             utilization-max
2015-10-22T11:29:00Z    0       13      0.9230769230769231      0.01171875
```

Since by default we're also mapping the metric to a measurement it will actually
be called `thread_pools` and have a metricName tag with value
`org.eclipse.jetty.util.thread.QueuedThreadPool.dw`:

```
name: thread_pools
------------------
time                    jobs    size    utilization             utilization-max metricName
2015-10-26T07:36:14Z    0       13      0.9230769230769231      0.01171875      org.eclipse.jetty.util.thread.QueuedThreadPool.dw
```

### Fields

We default to only report the median (p50), the 99th percentile and the 1m rate
for timers, and just the 1m rate for meters. Since we report every minute the 5
and 15 minute rates can be calculated from the 1 minute rate.

## Sender Types

This library can send metrics to InfluxDB directly with `http` (default),
 `tcp`, or `udp`. In addition to these metrics can also be sent the apps
logging facility using `logger` or to a Kafka topic, see below.

### Kafka

Metrics can be passed via Kafka by using the `kafka` sender type. Example config:

```
senderType: kafka
database: topic@broker1:9092,broker2:9092
```

## All Defaults

```yaml
senderType: http
protocol: http
host: localhost
port: 8086
tags: {} # global tags, e.g. environment or host
# push median (p50), some percentiles and the 1m rate
fields:
  timers: [p50, p75, p95, p99, p999, m1_rate]
  meters: [m1_rate]
groupGauges: yes
# exclude some pre-calculated metrics
excludes:
  - ch.qos.logback.core.Appender.debug
  - ch.qos.logback.core.Appender.trace
  - io.dropwizard.jetty.MutableServletContextHandler.percent-4xx-15m
  - io.dropwizard.jetty.MutableServletContextHandler.percent-4xx-1m
  - io.dropwizard.jetty.MutableServletContextHandler.percent-4xx-5m
  - io.dropwizard.jetty.MutableServletContextHandler.percent-5xx-15m
  - io.dropwizard.jetty.MutableServletContextHandler.percent-5xx-1m
  - io.dropwizard.jetty.MutableServletContextHandler.percent-5xx-5m
  - jvm.attribute.name
  - jvm.attribute.vendor
  - jvm.memory.heap.usage
  - jvm.memory.non-heap.usage
  - jvm.memory.pools.Code-Cache.usage
  - jvm.memory.pools.Compressed-Class-Space.usage
  - jvm.memory.pools.Metaspace.usage
  - jvm.memory.pools.PS-Eden-Space.usage
  - jvm.memory.pools.PS-Old-Gen.usage
  - jvm.memory.pools.PS-Survivor-Space.usage
precision: 1m # only store time precision to the minute
prefix: ""
database: ""
auth: ""
measurementMappings: {}
defaultMeasurementMappings:
  health: .*\.health.*
  auth: .*\.auth.*
  dao: .*\.(jdbi|dao).*
  resources: .*\.resources?.*
  event_handlers: .*Handler.*
  datasources: io\.dropwizard\.db\.ManagedPooledDataSource.*
  clients: org\.apache\.http\.client\.HttpClient.*
  client_connections: org\.apache\.http\.conn\.HttpClientConnectionManager.*
  connections: org\.eclipse\.jetty\.server\.HttpConnectionFactory.*
  thread_pools: org\.eclipse\.jetty\.util\.thread\.QueuedThreadPool.*
  logs: ch\.qos\.logback\.core\.Appender.*
  http_server: io\.dropwizard\.jetty\.MutableServletContextHandler.*
  raw_sql: org\.skife\.jdbi\.v2\.DBI\.raw-sql
  jvm: ^jvm$
  jvm_attribute: jvm\.attribute.*?
  jvm_buffers: jvm\.buffers\..*
  jvm_classloader: jvm\.classloader.*
  jvm_gc: jvm\.gc\..*
  jvm_memory: jvm\.memory\..*
  jvm_threads: jvm\.threads.*
# defaults inherited from BaseReporterFactory
durationUnit: MILLISECONDS
rateUnit: SECONDS
# default inherited from MetricsFactory
frequency: 1m
tagsTransformer:
  type: ClassBased # default
```
