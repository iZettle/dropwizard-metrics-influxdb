package com.izettle.metrics.dw;

/**
 * All type of datapoint senders which builder supported
 */
public enum SenderType {
    HTTP,
    TCP,
    UDP,
    LOGGER,
    KAFKA;
}
