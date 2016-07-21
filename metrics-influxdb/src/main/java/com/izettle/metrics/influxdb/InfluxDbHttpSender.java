package com.izettle.metrics.influxdb;

import com.izettle.metrics.influxdb.utils.TimeUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;
import org.apache.commons.codec.binary.Base64;

/**
 * An implementation of InfluxDbSender that writes to InfluxDb via http.
 */
public class InfluxDbHttpSender extends InfluxDbBaseSender {

    private final URL url;
    // The base64 encoded authString.
    private final String authStringEncoded;
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
    public InfluxDbHttpSender(
        final String protocol, final String hostname, final int port, final String database, final String authString,
        final TimeUnit timePrecision, final int connectTimeout, final int readTimeout, final String measurementPrefix)
        throws Exception {
        super(database, timePrecision, measurementPrefix);

        String endpoint = new URL(protocol, hostname, port, "/write").toString();
        String queryDb = String.format("db=%s", URLEncoder.encode(database, "UTF-8"));
        String queryPrecision = String.format("precision=%s", TimeUtils.toTimePrecision(timePrecision));
        this.url = new URL(endpoint + "?" + queryDb + "&" + queryPrecision);

        if (authString != null && !authString.isEmpty()) {
            this.authStringEncoded = Base64.encodeBase64String(authString.getBytes(UTF_8));
        } else {
            this.authStringEncoded = "";
        }

        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    @Deprecated
    public InfluxDbHttpSender(
        final String protocol, final String hostname, final int port, final String database, final String authString,
        final TimeUnit timePrecision) throws Exception {
        this(protocol, hostname, port, database, authString, timePrecision, 1000, 1000, "");
    }

    @Override
    protected int writeData(byte[] line) throws Exception {
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
            out.write(line);
            out.flush();
        } finally {
            out.close();
        }

        int responseCode = con.getResponseCode();

        // Check if non 2XX response code.
        if (responseCode / 100 != 2) {
            throw new IOException(
                "Server returned HTTP response code: " + responseCode + " for URL: " + url + " with content :'"
                    + con.getResponseMessage() + "'");
        }
        return responseCode;
    }
}
