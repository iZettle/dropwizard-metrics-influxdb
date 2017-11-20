package com.izettle.metrics.influxdb.tags;

import java.util.Map;

public interface Transformer {
    Map<String, String> getTags(String metricName);
}
