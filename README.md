Dropwizard Metrics v3 InfluxDb Integration
==========================================
[![Build Status](https://travis-ci.org/iZettle/dropwizard-metrics-influxdb.svg?branch=master)](https://travis-ci.org/iZettle/dropwizard-metrics-influxdb)

Support for InfluxDb for Dropwizard apps and metrics v3. `metrics-influxdb` was
copied and backported from
https://github.com/dropwizard/metrics/tree/master/metrics-influxdb/src/main/java/io/dropwizard/metrics/influxdb
which in turn may have started in
https://github.com/davidB/metrics-influxdb. Some extra features have been added
like field filtering and renaming metrics. `dropwizard-metrics-influxdb` is a
DropWizard metrics reporter factory for building a scheduled reporter.
