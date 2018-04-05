package com.izettle.metrics.dw;

import com.izettle.metrics.dw.tags.NoopTransformer;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class NoopTransformerTest {

    @Test
    public void shouldDoNoTransform() {
        Assertions.assertThat(new NoopTransformer().getTags("com.izettle.metrics.influxdb.tags.NoopTransformer.count"))
            .containsEntry("metricName", "com.izettle.metrics.influxdb.tags.NoopTransformer.count");
    }
}
