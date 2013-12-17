package org.jboss.aesh.cl;

import java.util.List;

@CommandDefinition(name = "test", description = "a simple test")
public class TestPopulator1 {

    @Option(shortName = 'X', name = "X", description = "enable X", hasValue = false)
    private Boolean enableX;

    @Option(shortName = 'b', hasValue = false)
    public boolean bar;

    @Option(shortName = 'f', name = "foo", description = "enable foo", hasValue = false)
    public boolean foo;

    @Option(shortName = 'e', name = "equal", description = "enable equal", required = true)
    public String equal;

    @Option(shortName = 'i', name = "int1", defaultValue = {"42"})
    private Integer int1;

    @Option(shortName = 'n')
    public int int2;

    @Arguments(defaultValue = {"foo"})
    public List<String> arguments;

    /*
    @OptionGroup(shortName = 'D', description = "define properties")
    public Map<String, String> define;
    */

    public TestPopulator1() {
    }

    public String getEqual() {
        return equal;
    }

    public Boolean getEnableX() {
        return enableX;
    }

    public Integer getInt1() {
        return int1;
    }
}