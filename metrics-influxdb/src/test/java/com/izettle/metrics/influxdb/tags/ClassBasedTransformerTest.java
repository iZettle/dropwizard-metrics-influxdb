package com.izettle.metrics.influxdb.tags;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ClassBasedTransformerTest {

    @Test
    public void shouldFindClassNames() {

        assertThat(new ClassBasedTransformer().getTags("com.izettle.metrics.influxdb.tags.ClassBasedTransformer.count"))
            .containsEntry("package", "com.izettle.metrics.influxdb.tags")
            .containsEntry("className", "ClassBasedTransformer")
            .containsEntry("method", "count");

        assertThat(new ClassBasedTransformer().getTags("com.izettle.metrics.influxdb.tags.ClassBasedTransformer"))
            .containsEntry("package", "com.izettle.metrics.influxdb.tags")
            .containsEntry("className", "ClassBasedTransformer")
            .doesNotContainKeys("method");

        assertThat(new ClassBasedTransformer().getTags("metric.without.class"))
            .containsEntry("metricName", "metric.without.class")
            .doesNotContainKeys("package")
            .doesNotContainKeys("className")
            .doesNotContainKeys("method");
    }
}
