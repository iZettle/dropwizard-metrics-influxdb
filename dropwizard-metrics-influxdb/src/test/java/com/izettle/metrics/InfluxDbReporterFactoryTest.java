package com.izettle.metrics;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.MetricRegistry;
import com.izettle.metrics.dw.InfluxDbReporterFactory;
import com.izettle.metrics.influxdb.InfluxDbHttpSender;
import com.izettle.metrics.influxdb.InfluxDbReporter;
import io.dropwizard.jackson.DiscoverableSubtypeResolver;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class InfluxDbReporterFactoryTest {

    private InfluxDbReporterFactory factory = new InfluxDbReporterFactory();

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

    @Test
    public void shouldReturnDefaultMeasurementMappings() {
        factory.setMeasurementMappings(null);
        Map<String, String> measurementMappings = factory.buildMeasurementMappings();
        assertThat(measurementMappings).isEqualTo(factory.getDefaultMeasurementMappings());
    }

    @Test
    public void shouldChangeDefaultMappingValue() {
        Map<String,String> mappings = new HashMap<>();
        mappings.put("health", "*.healthchecks.*");
        factory.setMeasurementMappings(mappings);

        Map<String, String> defaultMeasurementMappings = factory.getDefaultMeasurementMappings();
        Map<String, String> measurementMappings = factory.buildMeasurementMappings();

        assertThat(measurementMappings.size()).isEqualTo(defaultMeasurementMappings.size());
        assertThat(measurementMappings.get("health")).isEqualTo(mappings.get("health"));
    }

    @Test
    public void shouldNotChangeDefaultMappingValueWhenValueIsSame() {
        Map<String,String> mappings = new HashMap<>();
        mappings.put("health", "*.health.*");
        factory.setMeasurementMappings(mappings);

        Map<String, String> defaultMeasurementMappings = factory.getDefaultMeasurementMappings();
        Map<String, String> measurementMappings = factory.buildMeasurementMappings();

        assertThat(measurementMappings.size()).isEqualTo(defaultMeasurementMappings.size());
        assertThat(measurementMappings).isEqualTo(defaultMeasurementMappings);
    }

    @Test
    public void shouldAddNewMeasurementMapping() {
        Map<String,String> mappingsToAdd = new HashMap<>();
        mappingsToAdd.put("mappingKey", "*.mappingValue.*");
        factory.setMeasurementMappings(mappingsToAdd);

        Map<String, String> defaultMeasurementMappings = factory.getDefaultMeasurementMappings();
        Map<String, String> measurementMappings = factory.buildMeasurementMappings();

        assertThat(measurementMappings.size()).isEqualTo(defaultMeasurementMappings.size() + mappingsToAdd.size());
        assertThat(measurementMappings).containsEntry("mappingKey", "*.mappingValue.*");
    }

    @Test
    public void shouldRemoveDefaultMeasurementMappingWhenValueIsEmpty() {
        Map<String,String> mappingsToRemove = new HashMap<>();
        mappingsToRemove.put("health", "");
        mappingsToRemove.put("dao", "");
        factory.setMeasurementMappings(mappingsToRemove);

        Map<String, String> defaultMeasurementMappings = factory.getDefaultMeasurementMappings();
        Map<String, String> measurementMappings = factory.buildMeasurementMappings();

        assertThat(measurementMappings.size()).isEqualTo(defaultMeasurementMappings.size() - mappingsToRemove.size());
        assertThat(measurementMappings).doesNotContainKeys("health", "dao");
    }
}
