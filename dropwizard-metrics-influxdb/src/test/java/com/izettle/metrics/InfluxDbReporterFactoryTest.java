package com.izettle.metrics;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.MetricRegistry;
import com.izettle.metrics.InfluxDbReporterFactory;
import io.dropwizard.jackson.DiscoverableSubtypeResolver;
import io.dropwizard.metrics.influxdb.InfluxDbHttpSender;
import io.dropwizard.metrics.influxdb.InfluxDbReporter;
import java.net.URL;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class InfluxDbReporterFactoryTest {

    @Test
    public void isDiscoverable() throws Exception {
        assertThat(new DiscoverableSubtypeResolver().getDiscoveredSubtypes())
                .contains(InfluxDbReporterFactory.class);
    }

    @Test
    public void testNoAddressResolutionForInfluxDb() throws Exception {
        final InfluxDbReporter.Builder builderSpy = mock(InfluxDbReporter.Builder.class);
        new InfluxDbReporterFactory() {
            @Override
            protected InfluxDbReporter.Builder builder(MetricRegistry registry) {
                return builderSpy;
            }
        }.build(new MetricRegistry());

        final ArgumentCaptor<InfluxDbHttpSender> argument = ArgumentCaptor.forClass(InfluxDbHttpSender.class);
        verify(builderSpy).build(argument.capture());

        final InfluxDbHttpSender influxDb = argument.getValue();

        assertThat(getField(influxDb, "url")).isEqualTo(new URL("http", "localhost", 8086, "/write"));
        assertThat(getField(influxDb, "authStringEncoded")).isEqualTo(Base64.encodeBase64String("".getBytes(UTF_8)));
    }

    private static Object getField(InfluxDbHttpSender influxDb, String name) {
        try {
            return FieldUtils.getDeclaredField(InfluxDbHttpSender.class, name, true).get(influxDb);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
