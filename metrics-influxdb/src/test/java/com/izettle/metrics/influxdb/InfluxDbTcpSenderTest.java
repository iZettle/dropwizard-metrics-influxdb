package com.izettle.metrics.influxdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class InfluxDbTcpSenderTest {
    @Test(expected = UnknownHostException.class)
    public void shouldThrowUnknownHostException() throws Exception {
        InfluxDbTcpSender influxDbTcpSender = new InfluxDbTcpSender(
            "testtestasdfg",
            10080,
            1000,
            "test",
            TimeUnit.MINUTES,
            ""
        );
        influxDbTcpSender.writeData(new byte[0]);
    }

    @Test(expected = ConnectException.class)
    public void shouldThrowConnectionException() throws Exception {
        InfluxDbTcpSender influxDbTcpSender = new InfluxDbTcpSender(
            "0.0.0.0",
            10080,
            1000,
            "test",
            TimeUnit.MINUTES,
            ""
        );
        influxDbTcpSender.writeData(new byte[0]);
    }

    @Test
    public void shouldNotThrowException() throws Exception {
        ServerSocket server = new ServerSocket(10080);
        InfluxDbTcpSender influxDbTcpSender = new InfluxDbTcpSender(
            "0.0.0.0",
            10080,
            1000,
            "test",
            TimeUnit.MINUTES,
            ""
        );
        assertThat(influxDbTcpSender.writeData(new byte[0]) == 0);
        server.close();
    }
}
