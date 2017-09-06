package com.izettle.metrics.influxdb;

import java.util.concurrent.TimeUnit;

import com.izettle.metrics.influxdb.data.InfluxDbWriteObject;
import com.izettle.metrics.influxdb.utils.InfluxDbWriteObjectSerializer;

/**
 *  Passthrough to ultimately select a different style of serializer: grouped fields on one influxdb protocol line,
 *  instead of one field per protocol line.
 */
public class GroupedInfluxDbHttpSender extends InfluxDbHttpSender {
    private final String groupMeasurement;
    
    public GroupedInfluxDbHttpSender(String protocol, String hostname, int port, String database, String authString,
            TimeUnit timePrecision, int connectTimeout, int readTimeout, String measurementPrefix, String groupMeasurement) throws Exception {
        super(protocol, hostname, port, database, authString, timePrecision, connectTimeout, readTimeout, measurementPrefix);
        this.groupMeasurement = groupMeasurement;
    }

    @Override
    public int writeData() throws Exception {
        InfluxDbWriteObjectSerializer serializer = this.getSerializer();
        InfluxDbWriteObject writeObject = this.getWriteObject();
        String linestr = serializer.getGroupedLineProtocolString(writeObject, groupMeasurement);
        final byte[] line = linestr.getBytes(UTF_8);
        return super.writeData(line);
    }
}
