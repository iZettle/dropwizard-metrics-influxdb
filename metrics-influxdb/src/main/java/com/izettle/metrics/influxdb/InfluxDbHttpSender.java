package com.izettle.metrics.influxdb;

import com.izettle.metrics.influxdb.data.InfluxDbPoint;
import com.izettle.metrics.influxdb.data.InfluxDbWriteObject;
import com.izettle.metrics.influxdb.utils.InfluxDbWriteObjectSerializer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.codec.binary.Base64;

/**
 * An implementation of InfluxDbSender that writes to InfluxDb via http.
 */
public class InfluxDbHttpSender implements InfluxDbSender {
    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private final URL url;
    // The base64 encoded authString.
    private final String authStringEncoded;
    private final InfluxDbWriteObject influxDbWriteObject;
    private final InfluxDbWriteObjectSerializer influxDbWriteObjectSerializer;
    private final int connectTimeout;
    private final int readTimeout;


    /**
     * Creates a new http sender given connection details.
     *
     * @param hostname        the influxDb hostname
     * @param port            the influxDb http port
     * @param database        the influxDb database to write to
     * @param authString      the authorization string to be used to connect to InfluxDb, of format username:password
     * @param timePrecision   the time precision of the metrics
     * @param connectTimeout  the connect timeout
     * @param connectTimeout  the read timeout
     * @throws Exception exception while creating the influxDb sender(MalformedURLException)
     */
    public InfluxDbHttpSender(final String protocol, final String hostname, final int port, final String database, final String authString,
                              final TimeUnit timePrecision, final int connectTimeout, final int readTimeout) throws Exception {
        this.url = new URL(protocol, hostname, port, "/write");

        if (authString != null && !authString.isEmpty()) {
            this.authStringEncoded = Base64.encodeBase64String(authString.getBytes(UTF_8));
        } else {
            this.authStringEncoded = "";
        }

        this.influxDbWriteObject = new InfluxDbWriteObject(database, timePrecision);
        this.influxDbWriteObjectSerializer = new InfluxDbWriteObjectSerializer();

        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    @Deprecated
    public InfluxDbHttpSender(final String protocol, final String hostname, final int port, final String database, final String authString,
                              final TimeUnit timePrecision) throws Exception {
        this(protocol, hostname, port, database, authString, timePrecision, 1000, 1000);
    }

    @Override
    public void flush() {
        influxDbWriteObject.setPoints(new HashSet<InfluxDbPoint>());
    }

    @Override
    public boolean hasSeriesData() {
        return influxDbWriteObject.getPoints() != null && !influxDbWriteObject.getPoints().isEmpty();
    }

    @Override
    public void appendPoints(final InfluxDbPoint point) {
        if (point != null) {
            influxDbWriteObject.getPoints().add(point);
        }
    }

    @Override
    public int writeData() throws Exception {
        final String json = influxDbWriteObjectSerializer.getJsonString(influxDbWriteObject);
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        if (authStringEncoded != null && !authStringEncoded.isEmpty()) {
            con.setRequestProperty("Authorization", "Basic " + authStringEncoded);
        }
        con.setDoOutput(true);
        con.setConnectTimeout(connectTimeout);
        con.setReadTimeout(readTimeout);

        OutputStream out = con.getOutputStream();
        try {
            out.write(json.getBytes(UTF_8));
            out.flush();
        } finally {
            if (out != null) out.close();
        }

        int responseCode = con.getResponseCode();

        // Check if non 2XX response code.
        if (responseCode / 100 != 2) {
            throw new IOException("Server returned HTTP response code: " + responseCode + " for URL: " + url + " with content :'"
                    + con.getResponseMessage() + "'");
        }
        return responseCode;
    }

    @Override
    public void setTags(final Map<String, String> tags) {
        if (tags != null) {
            influxDbWriteObject.setTags(tags);
        }
    }
}
