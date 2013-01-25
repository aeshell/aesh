package org.jboss.aesh.cl;

/**
 * A value object designed to show on which option a complete operation
 * is perfomed on.
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParsedCompleteObject {

    private String name;
    private String value;
    private Class<?> type;
    private boolean isOption; //if its not option, its an argument

    public ParsedCompleteObject(String name, String value,
                                Class<?> type, boolean option) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.isOption = option;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public Class<?> getType() {
        return type;
    }

    public boolean isOption() {
        return isOption;
    }

    public boolean isArgument() {
        return !isOption;
    }
}
