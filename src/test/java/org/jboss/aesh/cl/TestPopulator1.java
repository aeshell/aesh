package org.jboss.aesh.cl;

@Command(name = "test", description = "a simple test")
public class TestPopulator1 {

    @Option(name = "X", description = "enable X")
    private Boolean enableX;

    @Option(shortName = 'f', name = "foo", description = "enable foo")
    public boolean foo;

    @Option(shortName = 'e', name = "equal", description = "enable equal", required = true)
    public String equal;

    @Option(shortName = 'i', name = "int1")
    private Integer int1;

    @Option(shortName = 'n')
    public int int2;

    /*
    @OptionGroup(shortName = 'D', description = "define properties")
    public Map<String, String> define;
    */

    public TestPopulator1() {
    }

    public Boolean getEnableX() {
        return enableX;
    }

    public Integer getInt1() {
        return int1;
    }
}