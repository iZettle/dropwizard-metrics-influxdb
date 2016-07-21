package com.izettle.metrics.influxdb;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of InfluxDbSender that uses UDP Connection.
 *
 * Warning: This class uses non encrypted UDP connection to connect to the remote host.
 */
public class InfluxDbUdpSender extends InfluxDbBaseSender {

    private final String hostname;
    private final int port;
    private final int socketTimeout;
    private DatagramSocket udpSocket;

    public InfluxDbUdpSender(
        String hostname,
        int port,
        int socketTimeout,
        String database,
        TimeUnit timePrecision,
        String measurementPrefix) {
        super(database, timePrecision, measurementPrefix);
        this.hostname = hostname;
        this.port = port;
        this.socketTimeout = socketTimeout;
    }

    @Override
    protected int writeData(byte[] line) throws Exception {
        createSocket();

        udpSocket.send(new DatagramPacket(line, line.length, InetAddress.getByName(hostname), port));

        return 0;
    }

    private void createSocket() throws IOException {
        if (udpSocket == null) {
            udpSocket = new DatagramSocket();
            udpSocket.setSoTimeout(socketTimeout);
        }
    }
}
