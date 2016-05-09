package com.izettle.metrics.influxdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class InfluxDbUdpSenderTest {
    @Test(expected = UnknownHostException.class)
    public void shouldThrowUnknownHostException() throws Exception {
        InfluxDbUdpSender influxDbUdpSender = new InfluxDbUdpSender(
            "testtestasdfg",
            10080,
            1000,
            "test",
            TimeUnit.MINUTES
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
            TimeUnit.MINUTES
        );
        assertThat(influxDbUdpSender.writeData(new byte[0]) == 0);
    }
}
