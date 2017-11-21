package com.izettle.metrics.influxdb.tags;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract the class name and function from the metricName.
 */
public class ClassBasedTransformer implements Transformer {

    private static final Pattern PACKAGE = Pattern.compile("(.*)\\.[A-Z].*");
    private static final Pattern CLASS_NAME = Pattern.compile(".*\\.([A-Z][^\\.]*).*");
    private static final Pattern METHOD = Pattern.compile(".*\\.[A-Z][^\\.]*\\.(.*)");

    @Override
    public Map<String, String> getTags(String metricsName) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("metricName", metricsName);

        Matcher packageMatcher = PACKAGE.matcher(metricsName);
        if(packageMatcher.find()) {
            tags.put("package", packageMatcher.group(1));
        }
        Matcher classMatcher = CLASS_NAME.matcher(metricsName);
        if(classMatcher.find()) {
            tags.put("className", classMatcher.group(1));
        }
        Matcher methodMatcher = METHOD.matcher(metricsName);
        if(methodMatcher.find()) {
            tags.put("method", methodMatcher.group(1));
        }
        return tags;
    }
}
