package com.izettle.metrics.influxdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.UnknownHostException;

import org.junit.Test;

public class InfluxDbUdpSenderTest {
    @Test(expected = UnknownHostException.class)
    public void shouldThrowUnknownHostException() throws Exception {
        InfluxDbUdpSender influxDbUdpSender = new InfluxDbUdpSender(
            "testtestasdfg",
            10080,
            1000,
            "test",
            ""
        );
        influxDbUdpSender.writeData(new byte[0]);
    }

    @Test
    public void shouldNotThrowException() throws Exception {
        InfluxDbUdpSender influxDbUdpSender = new InfluxDbUdpSender(
            "localhost",
            10080,
            1000,
            "test",
            ""
        );
        assertThat(influxDbUdpSender.writeData(new byte[0]) == 0);
    }
}
