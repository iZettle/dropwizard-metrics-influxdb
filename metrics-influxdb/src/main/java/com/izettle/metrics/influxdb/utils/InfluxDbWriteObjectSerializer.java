package com.izettle.metrics.influxdb.utils;

import com.izettle.metrics.influxdb.data.InfluxDbPoint;
import com.izettle.metrics.influxdb.data.InfluxDbWriteObject;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class InfluxDbWriteObjectSerializer {

    private static final Pattern COMMA = Pattern.compile(",");
    private static final Pattern SPACE = Pattern.compile(" ");
    private static final Pattern EQUAL = Pattern.compile("=");
    private static final Pattern DOUBLE_QUOTE = Pattern.compile("\"");

    // measurement[,tag=value,tag2=value2...] field=value[,field2=value2...] [unixnano]

    /**
     * calculate the lineprotocol for all Points.
     *
     * @return the String with newLines.
     */
    public String getLineProtocolString(InfluxDbWriteObject influxDbWriteObject) {
        StringBuilder stringBuilder = new StringBuilder();
        for (InfluxDbPoint point : influxDbWriteObject.getPoints()) {
            lineProtocol(point, influxDbWriteObject.getPrecision(), stringBuilder);
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    private void lineProtocol(InfluxDbPoint point, TimeUnit precision, StringBuilder stringBuilder) {
        stringBuilder.append(escapeKey(point.getMeasurement()));
        concatenatedTags(point.getTags(), stringBuilder);
        concatenateFields(point.getFields(), stringBuilder);
        formattedTime(point.getTime(), precision, stringBuilder);
    }

    private void concatenatedTags(Map<String, String> tags, StringBuilder stringBuilder) {
        for (Map.Entry<String, String> tag : tags.entrySet()) {
            stringBuilder.append(",");
            stringBuilder.append(escapeKey(tag.getKey())).append("=").append(escapeKey(tag.getValue()));
        }
        stringBuilder.append(" ");
    }

    private void concatenateFields(Map<String, Object> fields, StringBuilder stringBuilder) {
        final int fieldCount = fields.size();
        int loops = 0;

        NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
        numberFormat.setMaximumFractionDigits(340);
        numberFormat.setGroupingUsed(false);
        numberFormat.setMinimumFractionDigits(1);

        for (Map.Entry<String, Object> field : fields.entrySet()) {
            stringBuilder.append(escapeKey(field.getKey())).append("=");
            loops++;
            Object value = field.getValue();
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

            if (loops < fieldCount) {
                stringBuilder.append(",");
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
        String toBeEscaped = SPACE.matcher(key).replaceAll("\\ ");
        toBeEscaped = COMMA.matcher(toBeEscaped).replaceAll("\\,");
        return EQUAL.matcher(toBeEscaped).replaceAll("\\=");
    }

    private String escapeField(String field) {
        return DOUBLE_QUOTE.matcher(field).replaceAll("\\\"");
    }
}
