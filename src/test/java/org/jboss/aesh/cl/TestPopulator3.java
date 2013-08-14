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