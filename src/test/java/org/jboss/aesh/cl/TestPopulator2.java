package org.jboss.aesh.cl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@CommandDefinition(name = "test", description = "a simple test")
public class TestPopulator2 {

    @OptionList(shortName = 'b')
    private Set<String> basicSet;

    @OptionList(shortName = 'a')
    private List<Integer> basicList;

    @OptionList(shortName = 'i')
    private ArrayList<Short> implList;

    /*
    @OptionGroup(shortName = 'D', description = "define properties")
    public Map<String, String> define;
    */

    public TestPopulator2() {
    }

    public Set<String> getBasicSet() {
        return basicSet;
    }

    public List<Integer> getBasicList() {
        return basicList;
    }

    public List<Short> getImplList() {
        return implList;
    }
}