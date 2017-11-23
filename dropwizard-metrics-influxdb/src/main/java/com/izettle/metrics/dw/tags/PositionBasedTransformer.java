package com.izettle.metrics.dw.tags;

import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;

@JsonTypeName("PositionBased")
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
public class PositionBasedTransformer extends com.izettle.metrics.influxdb.tags.PositionBasedTransformer implements Transformer {

    public PositionBasedTransformer(Map<String, Category> mappings) {
        super(mappings);
    }
}
