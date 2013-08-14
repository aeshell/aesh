package org.jboss.aesh.cl;

import java.util.Currency;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@CommandDefinition(name = "test", description = "a simple test")
public class TestPopulator5 {

    @Option
    private Long veryLong;

    @OptionList
    private Set<String> basicSet;

    @OptionGroup(shortName = 'D', description = "define properties")
    private TreeMap<String, Integer> define;

    @Option(shortName = 'c', converter = CurrencyConverter.class)
    private Currency currency;

    @Arguments
    private Set<String> arguments;

    public TestPopulator5() {
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

    public Currency getCurrency() {
        return currency;
    }


}


