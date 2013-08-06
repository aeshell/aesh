package org.jboss.aesh.cl;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Command(name = "test", description = "a simple test")
public class TestPopulator4 {

    @Option
    private Long veryLong;

    @OptionList
    private Set<String> basicSet;

    @OptionGroup(shortName = 'D', description = "define properties")
    private TreeMap<String, Integer> define;

    @Arguments
    private Set<String> arguments;

    public TestPopulator4() {
    }

    public Set<String> getBasicSet() {
        return basicSet;
    }

    public Map<String, Integer> getDefine() {
        return define;
    }

    public Set<String> getArguments() {
        return arguments;
    }
}