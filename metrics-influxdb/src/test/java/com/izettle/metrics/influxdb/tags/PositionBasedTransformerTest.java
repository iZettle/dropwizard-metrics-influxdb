package com.izettle.metrics.influxdb.tags;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PositionBasedTransformerTest {

    @Test
    public void shouldExtractMultipleTags() {
        Map<String, PositionBasedTransformer.Category> mappings = new HashMap<String, PositionBasedTransformer.Category>();
        mappings.put("className", new PositionBasedTransformer.Category(
                5, "com\\.izettle\\.metrics\\.influxdb\\.tags\\..*"));
        mappings.put("function", new PositionBasedTransformer.Category(
                6, "com\\.izettle\\.metrics\\.influxdb\\.tags\\..*"));
        PositionBasedTransformer transformer = new PositionBasedTransformer(mappings);
        assertThat(transformer.getTags("com.izettle.metrics.influxdb.tags.PositionBasedTransformer.count"))
                .containsEntry("className", "PositionBasedTransformer")
                .containsEntry("function", "count");
        assertThat(transformer.getTags("com.izettle.metrics.influxdb.tags.NoopTransformer.count"))
                .containsEntry("className", "NoopTransformer")
                .containsEntry("function", "count");
    }

    @Test
    public void shouldNotAllowInvalidPosition() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
            () -> new PositionBasedTransformer.Category(-1, ".*")
        );
    }

    @Test
    public void shouldIgnoreIncorrectPosition() {
        Map<String, PositionBasedTransformer.Category> mappings = new HashMap<String, PositionBasedTransformer.Category>();
        mappings.put("incorrectTag", new PositionBasedTransformer.Category(42, ".*"));
        PositionBasedTransformer transformer = new PositionBasedTransformer(mappings);
        assertThat(transformer.getTags("com.izettle.metrics.influxdb.tags.PositionBasedTransformer.count")).isEmpty();
    }

    @Test
    public void shouldFailWithIncorrectRegExp() {
        assertThatExceptionOfType(PatternSyntaxException.class).isThrownBy(
            () -> new PositionBasedTransformer.Category(1, "[[a-z,\\.\']")
        );
    }
}
