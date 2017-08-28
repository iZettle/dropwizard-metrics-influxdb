package com.izettle.metrics.influxdb;

import java.util.concurrent.TimeUnit;

/**
 *  Passthrough to ultimately select a different style of serializer: grouped fields on one influxdb protocol line,
 *  instead of one field per protocol line.
 */
public class GroupedInfluxDbHttpSender extends InfluxDbHttpSender {
    public GroupedInfluxDbHttpSender(String protocol, String hostname, int port, String database, String authString,
            TimeUnit timePrecision, int connectTimeout, int readTimeout, String measurementPrefix, String measurement) throws Exception {
        super(protocol, hostname, port, database, authString, timePrecision, connectTimeout, readTimeout, measurementPrefix, measurement,
              true);
    }
}
