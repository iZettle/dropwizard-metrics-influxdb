package com.izettle.metrics.influxdb.utils;

import com.izettle.metrics.influxdb.data.InfluxDbPoint;
import com.izettle.metrics.influxdb.data.InfluxDbWriteObject;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class InfluxDbWriteObjectSerializer {

    private static final Pattern COMMA = Pattern.compile(",");
    private static final Pattern SPACE = Pattern.compile(" ");
    private static final Pattern EQUAL = Pattern.compile("=");
    private static final Pattern DOUBLE_QUOTE = Pattern.compile("\"");
    private static final Pattern FIELD = Pattern.compile("\\.");
    private final String measurementPrefix;

    public InfluxDbWriteObjectSerializer(String measurementPrefix) {
        this.measurementPrefix = measurementPrefix;
    }

    // measurement[,tag=value,tag2=value2...] field=value[,field2=value2...] [unixnano]

    /**
     * calculate the lineprotocol for all Points.
     *
     * @return the String with newLines.
     */
    public String getLineProtocolString(InfluxDbWriteObject influxDbWriteObject) {
        StringBuilder stringBuilder = new StringBuilder();
        for (InfluxDbPoint point : influxDbWriteObject.getPoints()) {
            pointLineProtocol(point, influxDbWriteObject.getPrecision(), stringBuilder);
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    /**
     * calculate the lineprotocol for all Points - grouped with same tags and timestamp.
     *
     * @return the String with newLines.
     */
    public String getGroupedLineProtocolString(InfluxDbWriteObject influxDbWriteObject) {
        // First develop a set of timestamps.
        HashSet<Long> times = new HashSet<>();
        for (InfluxDbPoint point : influxDbWriteObject.getPoints()) {
            times.add(point.getTime());
        }

        // Write lines, one per timestamp, instead of one per point.
        StringBuilder stringBuilder = new StringBuilder();
        for (Long time : times) {
            Map<String, Object> fields = new HashMap<>();
            String realMeasurement = "";

            for (InfluxDbPoint point : influxDbWriteObject.getPoints()) {
                if (point.getTime().equals(time)) {
                    realMeasurement = mergeFields(fields, point.getFields(), point.getMeasurement());
                }
            }
            lineProtocol(influxDbWriteObject.getTags(), fields, realMeasurement, time, influxDbWriteObject.getPrecision(),
                    stringBuilder);
            stringBuilder.append("\n");
        }

        return stringBuilder.toString();
    }

    /**
     * Merge fields, also parsing up the field name and returning the measurement.
     *
     * Rip off the first "." element from the measurement, then prepend measurement to all field keys.
     * This is an ASSUMPTION of the user's naming convention, because they're using this feature. This
     * is acknowledged evil based on the current design.
     *
     *  Eg  src: {value=10}, measurement: confabulator.jvm.memory_usage.pools.Code-Cache.max
     *      dest: {jvm.memory_usage.pools.Code-Cache.max.value=10}
     *      rtn: "confabulator"
     *
     * @return measurement for the line.
     */
    private String mergeFields(Map<String, Object> dest, Map<String, Object> src, String measurement) {
        String[] words = FIELD.split(measurement, 2);
        String head;
        String tail;

        if (words.length == 2) {
            head = words[0];
            tail = words[1];
        }
        else {
            head = measurement;
            tail = measurement;
        }

        for (Map.Entry<String, Object> field : src.entrySet()) {
            dest.put(tail + "." + field.getKey(), field.getValue());
        }
        return head;
    }

    private void pointLineProtocol(InfluxDbPoint point, TimeUnit precision, StringBuilder stringBuilder) {
        lineProtocol(point.getTags(), point.getFields(), point.getMeasurement(), point.getTime(),
                precision, stringBuilder);
    }

    private void lineProtocol(Map<String, String> tags, Map<String, Object> fields,
            String measurement, Long time, TimeUnit precision, StringBuilder stringBuilder) {
        stringBuilder.append(escapeMeasurement(measurementPrefix+measurement));
        concatenatedTags(tags, stringBuilder);
        concatenateFields(fields, stringBuilder);
        formattedTime(time, precision, stringBuilder);
    }

    private void concatenatedTags(Map<String, String> tags, StringBuilder stringBuilder) {
        for (Map.Entry<String, String> tag : tags.entrySet()) {
            stringBuilder.append(",");
            stringBuilder.append(escapeKey(tag.getKey())).append("=").append(escapeKey(tag.getValue()));
        }
        stringBuilder.append(" ");
    }

    private void concatenateFields(Map<String, Object> fields, StringBuilder stringBuilder) {
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
        numberFormat.setMaximumFractionDigits(340);
        numberFormat.setGroupingUsed(false);
        numberFormat.setMinimumFractionDigits(1);

        boolean firstField = true;
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            Object value = field.getValue();
            if (value instanceof Double) {
                Double doubleValue = (Double) value;
                if (doubleValue.isNaN() || doubleValue.isInfinite()) {
                    continue;
                }
            } else if (value instanceof Float) {
                Float floatValue = (Float) value;
                if (floatValue.isNaN() || floatValue.isInfinite()) {
                    continue;
                }
            }

            if (!firstField) {
                stringBuilder.append(",");
            }
            stringBuilder.append(escapeKey(field.getKey())).append("=");
            firstField = false;
            if (value instanceof String) {
                String stringValue = (String) value;
                stringBuilder.append("\"").append(escapeField(stringValue)).append("\"");
            } else if (value instanceof Number) {
                stringBuilder.append(numberFormat.format(value));
            } else if (value instanceof Boolean) {
                stringBuilder.append(value);
            } else {
                stringBuilder.append("\"").append(escapeField(value.toString())).append("\"");
            }
        }
    }

    private void formattedTime(Long time, TimeUnit precision, StringBuilder stringBuilder) {
        if (null == time) {
            time = System.currentTimeMillis();
        }
        stringBuilder.append(" ").append(precision.convert(time, TimeUnit.MILLISECONDS));
    }

    private String escapeKey(String key) {
        String toBeEscaped = SPACE.matcher(key).replaceAll("\\\\ ");
        toBeEscaped = COMMA.matcher(toBeEscaped).replaceAll("\\\\,");
        return EQUAL.matcher(toBeEscaped).replaceAll("\\\\=");
    }

    private String escapeMeasurement(String key) {
        String toBeEscaped = SPACE.matcher(key).replaceAll("\\\\ ");
        return COMMA.matcher(toBeEscaped).replaceAll("\\\\,");
    }

    private String escapeField(String field) {
        return DOUBLE_QUOTE.matcher(field).replaceAll("\\\"");
    }
}
