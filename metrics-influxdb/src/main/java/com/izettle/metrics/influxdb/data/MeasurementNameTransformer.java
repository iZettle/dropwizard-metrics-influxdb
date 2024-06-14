package com.izettle.metrics.influxdb.data;

public interface MeasurementNameTransformer {
    String transform(String name);
}
