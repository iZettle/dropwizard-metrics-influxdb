package com.izettle.metrics.dw.tags;

import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@JsonTypeName("ClassBased")
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
public class ClassBasedTransformer extends com.izettle.metrics.influxdb.tags.ClassBasedTransformer implements Transformer {
}
