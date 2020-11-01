package com.izettle.metrics.influxdb;

import com.izettle.metrics.influxdb.data.InfluxDbPoint;
import java.util.Map;

public interface InfluxDbSender {
    /**
     * Flushes buffer, if applicable.
     */
    void flush();

    /**
     * @return true if there is data available to send.
     */
    boolean hasSeriesData();

    /**
     * Adds this metric point to the buffer.
     *
     * @param point metric point with tags and fields
     */
    void appendPoints(final InfluxDbPoint point);

    /**
     * Writes buffer data to InfluxDb.
     *
     * @return the response code for the request sent to InfluxDb.
     *
     * @throws Exception exception while writing to InfluxDb api
     */
    int writeData() throws Exception;

    /**
     * Set tags applicable for all the points.
     *
     * @param tags map containing tags common to all metrics
     */
    void setTags(final Map<String, String> tags);

    /**
     * Set globalFields applicable for all the points
     *
     * @param globalFields map containing globalFields common to all metrics
     */
    void setGlobalFields(final Map<String, String> globalFields);

    Map<String, String> getTags();

    Map<String, String> getGlobalFields();
}
