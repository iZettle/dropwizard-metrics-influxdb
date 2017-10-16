package com.izettle.metrics.dw;

import static org.assertj.core.api.Assertions.assertThat;

import com.izettle.metrics.dw.PackageNameAbbreviator;
import org.junit.Test;

public class PackageNameAbbreviatorTest {

    PackageNameAbbreviator abbreviator = new PackageNameAbbreviator();

    @Test
    public void shouldAbbreviateCorrectly() {
        assertAbbreviatesTo("com.izettle.project.server.resources.TestResource.fooBar", "c.i.p.s.r.TestResource.fooBar");
        assertAbbreviatesTo("com.izettle.TestClass.one_time.foo", "c.i.TestClass.one_time.foo");
        assertAbbreviatesTo("TestClass.foo", "TestClass.foo");
        assertAbbreviatesTo("CustomName", "CustomName");
        assertAbbreviatesTo("jvm.threads.used", "jvm.threads.used");
        assertAbbreviatesTo("org.eclipse.jetty.server.HttpConnectionFactory.8081.connections", "o.e.j.s.HttpConnectionFactory.8081.connections");
        assertAbbreviatesTo("jvm.memory.pools.Compressed-Class-Space.committed", "j.m.p.Compressed-Class-Space.committed");
    }

    private void assertAbbreviatesTo(String fullyQualifiedName, String expected) {
        assertThat(abbreviator.abbreviate(fullyQualifiedName))
            .isEqualTo(expected);
    }

}