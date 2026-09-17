package org.aesh.io.filter;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

public class AllResourceFilterTest {

    private final AllResourceFilter filter = new AllResourceFilter();

    @Test
    public void testAcceptsEverything() {
        assertTrue(filter.accept(new org.aesh.io.FileResource(new File("anything"))));
    }
}
