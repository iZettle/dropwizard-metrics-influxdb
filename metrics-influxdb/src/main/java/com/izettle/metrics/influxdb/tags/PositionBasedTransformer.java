package com.izettle.metrics.influxdb.tags;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Based on tag name and Category mapping this class extracts map of tags from metrics name
 *
 * <br>
 * Example using the category mapping ["className", [5, "com\\.izettle\\.metrics\\.influxdb\\.tags\\.PositionBasedTransformer"]]
 * a metric called `com.izettle.metrics.influxdb.tags.PositionBasedTransformer.count` will be turned into
 * a tag:
 * <pre>
 *    tags: [[className=PositionBasedTransformer]]
 * </pre>
 */
public class PositionBasedTransformer implements Transformer {

    private static final String SEPARATOR = "\\.";

    private final Map<String, Category> mappings;

    public static class Category {
        private final int position;
        private final Pattern mapping;

        public Category(int position, String mapping) {
            if (position < 0) {
                throw new IllegalArgumentException("position should be gte 0");
            }
            this.position = position;
            this.mapping = Pattern.compile(mapping);
        }

        public int getPosition() {
            return position;
        }

        Pattern getMapping() {
            return mapping;
        }
    }

    public PositionBasedTransformer(Map<String, Category> mappings) {
        this.mappings = mappings;
    }

    @Override
    public Map<String, String> getTags(String metricsName) {
        String[] parts = metricsName.split(SEPARATOR);
        Map<String, String> tags = new HashMap<String, String>();

        for (Map.Entry<String, Category> entry : mappings.entrySet()) {
            final Pattern pattern = entry.getValue().getMapping();
            final int position = entry.getValue().position;

            if ((parts.length > position) && pattern.matcher(metricsName).matches()) {
                tags.put(entry.getKey(), parts[position]);
            }
        }

        return tags;
    }
}
