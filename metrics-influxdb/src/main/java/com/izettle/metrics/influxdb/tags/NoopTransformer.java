package com.izettle.metrics.influxdb.tags;

import java.util.HashMap;
import java.util.Map;

public class NoopTransformer implements Transformer {
    @Override
    public Map<String, String> getTags(String metricName) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("metricName", metricName);
        return tags;
    }
}
