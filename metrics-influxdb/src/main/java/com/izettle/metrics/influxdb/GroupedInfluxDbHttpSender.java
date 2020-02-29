package com.izettle.metrics.influxdb;

import java.util.concurrent.TimeUnit;

import com.izettle.metrics.influxdb.data.InfluxDbWriteObject;
import com.izettle.metrics.influxdb.utils.InfluxDbWriteObjectSerializer;

/**
 * Passthrough to ultimately select a different style of serializer: grouped fields on one influxdb protocol line, instead of one field per
 * protocol line.
 * 
 * This class will write out one protocol line per timestamp. For each point that occurred at a particular timestamp, a line is output in
 * the following order with a comma delimiting the first two items and a space delimiting all other items:
 * 1. measurementPrefix+groupMeasurement
 * 2. tags (in the format of name=value) delimited by commas
 * 3. each field is written prepended with a name that indicates its measurement delimited by commas. There is an assumption made here that
 *    the measurement naming convention is a set of strings delimited by a ".". In the case where there is at least two strings in the
 *    measurement name, the first string in that name is dropped. Also, if a particular point has only one field and that field has the key
 *    "value", the field name is dropped.
 * 4. time (in milliseconds)
 * 
 * To illustrate what is described above, here are some examples of what will be written for some sets of points.
 * 
 * <h3>EXAMPLE</h3>
 * <pre>
 * [
 *   // The first two points are intended to show how points with the same timestamp are merged.
 *   Point {
 *     measurement: confabulator.a.b.c,
 *     tags {
 *       foo: one,
 *       bar: two
 *     },
 *     time: 0,
 *     fields {
 *       temp: 10,
 *       bytes: 7
 *     }
 *   },
 *   Point {
 *     measurement: confabulator.x.y.z,
 *     tags {
 *       foo: one,
 *       bar: two
 *     },
 *     time: 0,
 *     fields {
 *       pressure: 50,
 *       status: good
 *     }
 *   },
 *   // This next point shows the example of when the field has one key called "value"
 *   Point {
 *     measurement: confabulator.file.size.max,
 *     tags {},
 *     time: 1,
 *     fields {
 *       value = 10000000
 *     }
 *   },
 *   // This next point shows an example of what is written when measurement is only a single string
 *   Point {
 *     measurement: minimum,
 *     tags {},
 *     time: 2,
 *     fields {
 *       memory: 64000,
 *       temperature: 98.6
 *     }
 *   }
 * ]
 * </pre>
 * 
 * <h3>OUTPUT FOR ABOVE EXAMPLE</h3>
 * Assuming that the value for the <code>measurementPrefix</code> is <code>null</code> and the <code>groupMeasurement</code> is the string
 * "groupMeasurement", the output for the above example would result in three lines corresponding to the three unique timestamps. The lines
 * would be as follows:
 * <pre>
 * groupMeasurement,foo=one,bar=two a.b.c.temp=10,a.b.c.bytes=7,x.y.z.pressure=50,x.y.z.status=good 0
 * groupMeasurement file.size.max=10000000 1
 * groupMeasurement minimum.memory=64000,minimum.temperature=98.6 2
 * </pre>
 */
public class GroupedInfluxDbHttpSender extends InfluxDbHttpSender {
    private final String groupMeasurement;
    
    /**
     * Creates a new http sender given connection details. This sender groups all the fields under one measurement and transmit them as one
     * measurement.
     * 
     * @param protocol           the name of the protocol to use
     * @param hostname           the influxDb hostname
     * @param port               the influxDb http port
     * @param database           the influxDb database to write to
     * @param authString         the authorization string to be used to connect to InfluxDb, of format username:password
     * @param timePrecision      the time precision of the metrics
     * @param connectTimeout     the connect timeout
     * @param readTimeout        the read timeout
     * @param measurementPrefix  the measurement prefix
     * @param groupMeasurement   the group measurement name
     * @throws Exception exception while creating the influxDb sender(MalformedURLException)
     */
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
