package org.jboss.aesh.cl;

import java.util.List;
import java.util.Set;

@Command(name = "test", description = "a simple test")
public class TestPopulator2 {

    @OptionList(shortName = 'b')
    private Set<String> basicList;

    /*
    @OptionGroup(shortName = 'D', description = "define properties")
    public Map<String, String> define;
    */

    public TestPopulator2() {
    }

    public Set<String> getBasicList() {
        return basicList;
    }
}