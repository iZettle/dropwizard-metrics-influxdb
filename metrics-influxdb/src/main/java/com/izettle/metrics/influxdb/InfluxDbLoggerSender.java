package com.izettle.metrics.influxdb;

import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfluxDbLoggerSender extends InfluxDbBaseSender {

    private static final Logger logger = LoggerFactory.getLogger(InfluxDbLoggerSender.class);

    public InfluxDbLoggerSender(String database, TimeUnit timePrecision, String measurementPrefix) {
        super(database, timePrecision, measurementPrefix);
    }

    @Override
    protected int writeData(byte[] line) throws Exception {
        logger.info(new String(line, Charsets.UTF_8));
        return 0;
    }
}
