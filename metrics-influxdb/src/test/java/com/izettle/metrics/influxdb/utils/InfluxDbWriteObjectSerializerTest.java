package com.izettle.metrics.influxdb.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.izettle.metrics.influxdb.data.InfluxDbPoint;
import com.izettle.metrics.influxdb.data.InfluxDbWriteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
        InfluxDbPoint point1 = new InfluxDbPoint("measurement1", tags, 456l, fields);
        Set<InfluxDbPoint> set = new HashSet<InfluxDbPoint>();
        set.add(point1);
        InfluxDbWriteObject influxDbWriteObject = mock(InfluxDbWriteObject.class);
        when(influxDbWriteObject.getPoints()).thenReturn(set);
        when(influxDbWriteObject.getPrecision()).thenReturn(TimeUnit.MICROSECONDS);
        InfluxDbWriteObjectSerializer influxDbWriteObjectSerializer = new InfluxDbWriteObjectSerializer("");
        String lineString = influxDbWriteObjectSerializer.getLineProtocolString(influxDbWriteObject);
        assertThat(lineString).isEqualTo(
            "measurement1,tag1Key=tag1Value field1Key=\"field1Value\" 456000\n");
        InfluxDbPoint point2 = new InfluxDbPoint("measurement1", tags, 456l, fields);
        set.add(point2);
        lineString = influxDbWriteObjectSerializer.getLineProtocolString(influxDbWriteObject);
        assertThat(lineString).isEqualTo(
            "measurement1,tag1Key=tag1Value field1Key=\"field1Value\" 456000\n"
                + "measurement1,tag1Key=tag1Value field1Key=\"field1Value\" 456000\n");
    }

    @Test
    public void shouldSerializeUsingGroupedLineProtocol() {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("tag1Key", "tag1Value");
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("field1Key", "field1Value");
        InfluxDbPoint point1 = new InfluxDbPoint("aaa.bbb.ccc", tags, 456l, fields);
        Set<InfluxDbPoint> set = new HashSet<InfluxDbPoint>();
        set.add(point1);
        InfluxDbWriteObject influxDbWriteObject = mock(InfluxDbWriteObject.class);
        when(influxDbWriteObject.getPoints()).thenReturn(set);
        when(influxDbWriteObject.getPrecision()).thenReturn(TimeUnit.MICROSECONDS);
        InfluxDbWriteObjectSerializer influxDbWriteObjectSerializer = new InfluxDbWriteObjectSerializer("");
        String lineString = influxDbWriteObjectSerializer.getGroupedLineProtocolString(influxDbWriteObject, "xxx");
        assertThat(lineString).isEqualTo(
                "xxx,tag1Key=tag1Value bbb.ccc.field1Key=\"field1Value\" 456000\n");
        InfluxDbPoint point2 = new InfluxDbPoint("www.yyy.zzz", tags, 456l, fields);
        set.add(point2);
        lineString = influxDbWriteObjectSerializer.getGroupedLineProtocolString(influxDbWriteObject, "xxx");
        assertThat(lineString).isEqualTo(
                "xxx,tag1Key=tag1Value bbb.ccc.field1Key=\"field1Value\",yyy.zzz.field1Key=\"field1Value\" 456000\n");
    }

    @Test
    public void groupedLinesShouldFoldValueFields() {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("tag1Key", "tag1Value");
        Map<String, Object> fields1 = new HashMap<String, Object>();
        fields1.put("value", "111");
        Map<String, Object> fields2 = new HashMap<String, Object>();
        fields2.put("value", "222");
        InfluxDbPoint point1 = new InfluxDbPoint("aa.bb.cc1", tags, 456l, fields1);
        InfluxDbPoint point2 = new InfluxDbPoint("aa.bb.cc2", tags, 456l, fields2);
        Set<InfluxDbPoint> set = new HashSet<InfluxDbPoint>();
        set.add(point1);
        set.add(point2);
        InfluxDbWriteObject influxDbWriteObject = mock(InfluxDbWriteObject.class);
        when(influxDbWriteObject.getPoints()).thenReturn(set);
        when(influxDbWriteObject.getPrecision()).thenReturn(TimeUnit.MICROSECONDS);
        InfluxDbWriteObjectSerializer influxDbWriteObjectSerializer = new InfluxDbWriteObjectSerializer("");
        String lineString = influxDbWriteObjectSerializer.getGroupedLineProtocolString(influxDbWriteObject, "xxx");
        assertThat(lineString).isEqualTo("xxx,tag1Key=tag1Value bb.cc2=\"222\",bb.cc1=\"111\" 456000\n");
    }

    @Test
    public void undottedMeasurementShouldFallback() {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("tag1Key", "tag1Value");
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("field1Key", "field1Value");
        InfluxDbPoint point1 = new InfluxDbPoint("aaa", tags, 456l, fields);
        Set<InfluxDbPoint> set = new HashSet<InfluxDbPoint>();
        set.add(point1);
        InfluxDbWriteObject influxDbWriteObject = mock(InfluxDbWriteObject.class);
        when(influxDbWriteObject.getPoints()).thenReturn(set);
        when(influxDbWriteObject.getPrecision()).thenReturn(TimeUnit.MICROSECONDS);
        InfluxDbWriteObjectSerializer influxDbWriteObjectSerializer = new InfluxDbWriteObjectSerializer("");
        String lineString = influxDbWriteObjectSerializer.getGroupedLineProtocolString(influxDbWriteObject, "xxx");
        assertThat(lineString).isEqualTo(
                "xxx,tag1Key=tag1Value aaa.field1Key=\"field1Value\" 456000\n");
        InfluxDbPoint point2 = new InfluxDbPoint("bbb", tags, 456l, fields);
        set.add(point2);
        lineString = influxDbWriteObjectSerializer.getGroupedLineProtocolString(influxDbWriteObject, "xxx");
        assertThat(lineString).isEqualTo(
                "xxx,tag1Key=tag1Value aaa.field1Key=\"field1Value\",bbb.field1Key=\"field1Value\" 456000\n");
    }

    @Test
    public void shouldOmitNaNsEtcWhenSerializingUsingLineProtocolForDouble() {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        fields.put("field1Key", "field1Value");
        fields.put("field2Key", Double.NaN);
        fields.put("field3Key", Double.POSITIVE_INFINITY);
        fields.put("field4Key", Double.NEGATIVE_INFINITY);
        fields.put("field5Key", 0.432);
        InfluxDbWriteObject influxDbWriteObject = new InfluxDbWriteObject("test-db", TimeUnit.MICROSECONDS);
        influxDbWriteObject.getPoints().add(new InfluxDbPoint("measurement1", 456l, fields));

        InfluxDbWriteObjectSerializer influxDbWriteObjectSerializer = new InfluxDbWriteObjectSerializer("");
        String lineString = influxDbWriteObjectSerializer.getLineProtocolString(influxDbWriteObject);

        assertThat(lineString).isEqualTo("measurement1 field1Key=\"field1Value\",field5Key=0.432 456000\n");
    }

    @Test
    public void shouldOmitNaNsEtcWhenSerializingUsingLineProtocolForFloat() {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        fields.put("field1Key", "field1Value");
        fields.put("field2Key", Float.NaN);
        fields.put("field3Key", Float.POSITIVE_INFINITY);
        fields.put("field4Key", Float.NEGATIVE_INFINITY);
        fields.put("field5Key", 0.432);
        InfluxDbWriteObject influxDbWriteObject = new InfluxDbWriteObject("test-db", TimeUnit.MICROSECONDS);
        influxDbWriteObject.getPoints().add(new InfluxDbPoint("measurement1", 456l, fields));

        InfluxDbWriteObjectSerializer influxDbWriteObjectSerializer = new InfluxDbWriteObjectSerializer("");
        String lineString = influxDbWriteObjectSerializer.getLineProtocolString(influxDbWriteObject);

        assertThat(lineString).isEqualTo("measurement1 field1Key=\"field1Value\",field5Key=0.432 456000\n");
    }

    @Test
    public void shouldEscapeKeys() {
        Map<String, String> tags = new LinkedHashMap<String, String>();
        tags.put("tag1 Key", "tag1 Value");
        tags.put("tag1,Key", "tag1,Value");
        tags.put("tag1=Key", "tag1=Value");
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        fields.put("field1 Key", "field1Value");
        InfluxDbWriteObject influxDbWriteObject = new InfluxDbWriteObject("test-db", TimeUnit.MICROSECONDS);
        influxDbWriteObject.getPoints().add(new InfluxDbPoint("my measurement,1", tags, 456l, fields));

        InfluxDbWriteObjectSerializer influxDbWriteObjectSerializer = new InfluxDbWriteObjectSerializer("");
        String lineString = influxDbWriteObjectSerializer.getLineProtocolString(influxDbWriteObject);

        assertThat(lineString)
            .isEqualTo(
                "my\\ measurement\\,1,tag1\\ Key=tag1\\ Value,tag1\\,Key=tag1\\,Value,tag1\\=Key=tag1\\=Value field1\\ Key=\"field1Value\" 456000\n");
    }

    @Test
    public void shouldEscapeMeasurement() {
        Map<String, String> tags = new HashMap<String, String>();
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("field1 Key", "field1Value");
        InfluxDbWriteObject influxDbWriteObject = new InfluxDbWriteObject("test-db", TimeUnit.MICROSECONDS);
        influxDbWriteObject.getPoints().add(new InfluxDbPoint("my measurement,1=1", tags, 456l, fields));

        InfluxDbWriteObjectSerializer influxDbWriteObjectSerializer = new InfluxDbWriteObjectSerializer("");
        String lineString = influxDbWriteObjectSerializer.getLineProtocolString(influxDbWriteObject);

        assertThat(lineString)
            .isEqualTo("my\\ measurement\\,1=1 field1\\ Key=\"field1Value\" 456000\n");
    }

    @Test
    public void shouldAddPrefixToMeasurementName() {
        Map<String, String> tags = new HashMap<String, String>();
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("field1 Key", "field1Value");
        InfluxDbWriteObject influxDbWriteObject = new InfluxDbWriteObject("test-db", TimeUnit.MICROSECONDS);
        influxDbWriteObject.getPoints().add(new InfluxDbPoint("my measurement,1=1", tags, 456l, fields));

        InfluxDbWriteObjectSerializer influxDbWriteObjectSerializer = new InfluxDbWriteObjectSerializer("prefix.");
        String lineString = influxDbWriteObjectSerializer.getLineProtocolString(influxDbWriteObject);

        assertThat(lineString)
            .isEqualTo("prefix.my\\ measurement\\,1=1 field1\\ Key=\"field1Value\" 456000\n");
    }
}
