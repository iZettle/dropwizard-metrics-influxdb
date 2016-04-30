package com.izettle.metrics.influxdb;

import com.izettle.metrics.influxdb.data.InfluxDbPoint;
import com.izettle.metrics.influxdb.data.InfluxDbWriteObject;
import com.izettle.metrics.influxdb.utils.InfluxDbWriteObjectSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of InfluxDbSender that uses TCP Connection.
 *
 * Warning: This class uses non encrypted TCP connection to connect to the remote host.
 */
public class InfluxDbTcpSender implements InfluxDbSender {

    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private static final int NUM_OF_RETRIES = 2;

    private final InfluxDbWriteObject influxDbWriteObject;
    private final InfluxDbWriteObjectSerializer influxDbWriteObjectSerializer;
    private final String hostname;
    private final int port;
    private final int socketTimeout;
    private Socket tcpSocket;

    /**
     * Creates an instance of [[InfluxDbTcpSender]]
     *
     * @param hostname          The hostname to connect
     * @param port              The port to connect
     * @param database          The database to write into
     * @param timePrecision     The time precision to use
     * @param socketTimeout     A socket timeout to use
     */
    public InfluxDbTcpSender(final String hostname, final int port, final String database, final TimeUnit timePrecision,
                             final int socketTimeout) {
        this.hostname = hostname;
        this.port = port;
        this.socketTimeout = socketTimeout;

        this.influxDbWriteObject = new InfluxDbWriteObject(database, timePrecision);
        this.influxDbWriteObjectSerializer = new InfluxDbWriteObjectSerializer();
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
    public void appendPoints(InfluxDbPoint point) {
        if (point != null) {
            influxDbWriteObject.getPoints().add(point);
        }
    }

    @Override
    public int writeData() throws Exception {
        retryConnect(false);
        final byte[] line = influxDbWriteObjectSerializer.getLineProtocolString(influxDbWriteObject).getBytes(UTF_8);

        for (int i = 1 ; i <= NUM_OF_RETRIES; i++) {
            try {
                OutputStream outputStream = tcpSocket.getOutputStream();
                outputStream.write(line);
                outputStream.flush();

                return 0;
            } catch (IOException e) {
                // In case of exception, force reconnect
                retryConnect(true);
            }
        }

        return 0;
    }

    @Override
    public void setTags(Map<String, String> tags) {
        if (tags != null) {
            influxDbWriteObject.setTags(tags);
        }
    }

    @Override
    public Map<String, String> getTags() {
        return influxDbWriteObject.getTags();
    }

    /**
     * Creates a new socket to the server
     *
     * @param force Whether to force reconnect
     * @throws IOException On connection error (Server is not alive, not responding, etc')
     */
    private void retryConnect(boolean force) throws IOException {
        if (force || tcpSocket == null) {

            if (tcpSocket != null) {
                tcpSocket.close();
                tcpSocket = null;
            }

            tcpSocket = new Socket(hostname, port);
            tcpSocket.setSoTimeout(socketTimeout);
        }

    }

}
