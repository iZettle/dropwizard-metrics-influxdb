package com.izettle.metrics.dw.tags;

import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@JsonTypeName("Noop")
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
public class NoopTransformer extends com.izettle.metrics.influxdb.tags.NoopTransformer implements Transformer {
}
