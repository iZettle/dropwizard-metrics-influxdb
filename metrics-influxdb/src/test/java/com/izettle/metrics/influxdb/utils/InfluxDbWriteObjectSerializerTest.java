package com.izettle.metrics.influxdb.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.izettle.metrics.influxdb.data.InfluxDbPoint;
import com.izettle.metrics.influxdb.data.InfluxDbWriteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class InfluxDbWriteObjectSerializerTest {
    
    @Test
    public void shouldSerializeUsingLineProtocol() {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("tag1Key", "tag1Value");
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("field1Key", "field1Value");
        InfluxDbPoint point1 = new InfluxDbPoint("measurement1", tags, 123l, fields);
        Set<InfluxDbPoint> set = new HashSet<InfluxDbPoint>();
        set.add(point1);
        InfluxDbWriteObject influxDbWriteObject = mock(InfluxDbWriteObject.class);
        when(influxDbWriteObject.getPoints()).thenReturn(set);
        when(influxDbWriteObject.getPrecision()).thenReturn(TimeUnit.MILLISECONDS);
        InfluxDbWriteObjectSerializer influxDbWriteObjectSerializer = new InfluxDbWriteObjectSerializer();
        String lineString = influxDbWriteObjectSerializer.getLineProtocolString(influxDbWriteObject);
        assertThat(lineString).isEqualTo(
            "measurement1,tag1Key=tag1Value field1Key=\"field1Value\" 123000000\n");
        InfluxDbPoint point2 = new InfluxDbPoint("measurement1", tags, 123l, fields);
        set.add(point2);
        lineString = influxDbWriteObjectSerializer.getLineProtocolString(influxDbWriteObject);
        assertThat(lineString).isEqualTo(
            "measurement1,tag1Key=tag1Value field1Key=\"field1Value\" 123000000\n"
                + "measurement1,tag1Key=tag1Value field1Key=\"field1Value\" 123000000\n");
    }
}
