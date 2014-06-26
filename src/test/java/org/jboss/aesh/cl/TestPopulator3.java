/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import java.util.Map;
import java.util.TreeMap;

@CommandDefinition(name = "test", description = "a simple test")
public class TestPopulator3 {

    @OptionGroup(shortName = 'b')
    private Map<String, String> basicMap;

    @OptionGroup(shortName = 'i')
    private TreeMap<String, Integer> integerMap;

    public TestPopulator3() {
    }

    public Map<String,String> getBasicMap() {
        return basicMap;
    }

    public Map<String, Integer> getIntegerMap() {
        return integerMap;
    }
}