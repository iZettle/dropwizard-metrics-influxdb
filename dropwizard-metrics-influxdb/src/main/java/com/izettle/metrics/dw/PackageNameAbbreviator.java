package com.izettle.metrics.dw;

import java.util.regex.Pattern;

public class PackageNameAbbreviator {

    // Capture the first letter of all dot-separated words that comes before the first capitalized word. E.g:
    // Input:      org.example.MyClass.myMethod
    // Captures:   ^   ^
    private Pattern pattern = Pattern.compile("([a-z])\\w+(?=.*\\.[A-Z])");

    public String abbreviate(String fullyQualifiedName) {
        return pattern
            .matcher(fullyQualifiedName)
            .replaceAll("$1");
    }
}
