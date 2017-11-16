package com.izettle.metrics.influxdb.tags;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassBasedTransformer implements Transformer{

    private final Pattern className;

    public ClassBasedTransformer() {
        this.className = Pattern.compile("\\w+(?= )");
    }

    @Override
    public Map<String, String> getTags(String metricsName) {
        Map<String, String> tags = new HashMap<String, String>();

        Matcher classMatcher = className.matcher(metricsName);
        if(classMatcher.find()) {
            tags.put("class", classMatcher.group(0));
        }
        return tags;
    }
}
