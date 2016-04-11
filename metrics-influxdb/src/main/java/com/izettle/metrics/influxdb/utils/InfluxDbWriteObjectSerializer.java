package com.izettle.metrics.influxdb.utils;

import com.izettle.metrics.influxdb.data.InfluxDbPoint;
import com.izettle.metrics.influxdb.data.InfluxDbWriteObject;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class InfluxDbWriteObjectSerializer {

    // measurement[,tag=value,tag2=value2...] field=value[,field2=value2...] [unixnano]

    /**
     * calculate the lineprotocol for all Points.
     *
     * @return the String with newLines.
     */
    public String getLineProtocolString(InfluxDbWriteObject influxDbWriteObject) {
        StringBuilder sb = new StringBuilder();
        for (InfluxDbPoint point : influxDbWriteObject.getPoints()) {
            sb.append(lineProtocol(point, influxDbWriteObject.getPrecision())).append("\n");
        }
        return sb.toString();
    }

    public String lineProtocol(InfluxDbPoint point, TimeUnit precision) {
        final StringBuilder sb = new StringBuilder();
        sb.append(escapeKey(point.getMeasurement()));
        sb.append(concatenatedTags(point.getTags()));
        sb.append(concatenateFields(point.getFields()));
        sb.append(formattedTime(point.getTime(), precision));
        return sb.toString();
    }

    private StringBuilder concatenatedTags(Map<String, String> tags) {
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> tag : tags.entrySet()) {
            sb.append(",");
            sb.append(escapeKey(tag.getKey())).append("=").append(escapeKey(tag.getValue()));
        }
        sb.append(" ");
        return sb;
    }

    private StringBuilder concatenateFields(Map<String, Object> fields) {
        final StringBuilder sb = new StringBuilder();
        final int fieldCount = fields.size();
        int loops = 0;

        NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
        numberFormat.setMaximumFractionDigits(340);
        numberFormat.setGroupingUsed(false);
        numberFormat.setMinimumFractionDigits(1);

        for (Map.Entry<String, Object> field : fields.entrySet()) {
            sb.append(escapeKey(field.getKey())).append("=");
            loops++;
            Object value = field.getValue();
            if (value instanceof String) {
                String stringValue = (String) value;
                sb.append("\"").append(escapeField(stringValue)).append("\"");
            } else if (value instanceof Number) {
                sb.append(numberFormat.format(value));
            } else {
                sb.append(value);
            }

            if (loops < fieldCount) {
                sb.append(",");
            }
        }
        return sb;
    }

    private StringBuilder formattedTime(Long time, TimeUnit precision) {
        final StringBuilder sb = new StringBuilder();
        if (null == time) {
            time = System.nanoTime();
        }
        sb.append(" ").append(TimeUnit.NANOSECONDS.convert(precision.convert(time, TimeUnit.MILLISECONDS), precision));
        return sb;
    }

    private String escapeField(String field) {
        return field.replaceAll(" ", "\\ ")
            .replaceAll(",", "\\,")
            .replaceAll("=", "\\=");
    }

    private String escapeKey(String key) {
        return key.replaceAll("\"", "\\\"");
    }
}
