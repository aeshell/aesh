package org.jboss.aesh.cl;

import org.jboss.aesh.cl.validator.OptionValidator;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.command.validator.ValidatorInvocation;

import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@CommandDefinition(name = "test", description = "a simple test")
public class TestPopulator5 {

    @Option(shortName = 'v', validator = LongOptionValidator.class)
    private Long veryLong;

    @OptionList
    private Set<String> basicSet;

    @OptionGroup(shortName = 'D', description = "define properties")
    private TreeMap<String, Integer> define;

    @Option(shortName = 'c', converter = CurrencyConverter.class)
    private Currency currency;

    @OptionList(validator = LongOptionValidator.class, valueSeparator = ';')
    private List<Long> longs;

    @OptionList(name = "strings")
    private List<String> strings;

    @Option(name = "bar", hasValue = false)
    private Boolean bar;

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

    public Long getVeryLong() {
        return veryLong;
    }

    public List<Long> getLongs() {
        return longs;
    }

    public List<String> getStrings() {
        return strings;
    }

    public Boolean getBar() {
        return bar;
    }

    public class LongOptionValidator implements OptionValidator {
        @Override
        public void validate(ValidatorInvocation validatorInvocation) throws OptionValidatorException {
            Long value = (Long) validatorInvocation.getValue();
            if(value < 0 || value > 100)
                throw new OptionValidatorException("value must be between 0 and 100");
        }
    }

    public class LongValidatorInvocation implements ValidatorInvocation<Long, Object> {

        private final Long value;

        public LongValidatorInvocation(Long value) {
            this.value = value;
        }

        @Override
        public Long getValue() {
            return value;
        }

        @Override
        public Object getCommand() {
            return null;
        }

        @Override
        public AeshContext getAeshContext() {
            return null;
        }
    }

}


