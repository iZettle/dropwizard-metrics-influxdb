package com.izettle.metrics.dw.tags;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.dropwizard.jackson.Discoverable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_INTERFACE")
public interface Transformer extends Discoverable, com.izettle.metrics.influxdb.tags.Transformer {
}
