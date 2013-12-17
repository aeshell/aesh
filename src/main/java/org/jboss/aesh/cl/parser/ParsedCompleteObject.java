package org.jboss.aesh.cl.parser;

/**
 * A value object designed to show on which option a complete operation
 * is performed on.
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParsedCompleteObject {

    private String name;
    private String value = "";
    private Class<?> type;
    private boolean option; //if its not option, its an argument
    private boolean displayOptions = false;
    private boolean argument = false;
    private boolean displayArguments = false;
    private boolean completeOptionName = false;
    private int offset = 0;

    public ParsedCompleteObject(boolean displayArguments) {
        this.option = !displayArguments;
        this.argument = !option;
    }

    public ParsedCompleteObject(boolean displayOptions, String name, int offset) {
        this.displayOptions = displayOptions;
        this.offset = offset;
        this.name = name;
        this.value = "";
        this.type = null;
        this.option = false;
    }

    public ParsedCompleteObject(boolean displayOptions, String name, int offset, boolean completeOptionName) {
        this(displayOptions, name, offset);
        this.completeOptionName = completeOptionName;
    }

    public ParsedCompleteObject(String name, String value,
                                Class<?> type, boolean option, boolean completeOptionName) {
        this(name, value, type, option);
        this.completeOptionName = completeOptionName;
    }

    public ParsedCompleteObject(String name, String value,
                                Class<?> type, boolean option) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.option = option;
        this.argument = !this.option;
        this.offset = value.length();
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
        return option;
    }

    public boolean isArgument() {
        return argument;
    }

    public int getOffset() {
        return offset;
    }

    public boolean doDisplayOptions() {
        return displayOptions;
    }

    public boolean isCompleteOptionName() {
        return completeOptionName;
    }

    @Override
    public String toString() {
        return "ParsedCompleteObject{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", type=" + type +
                ", option=" + option +
                ", argument=" + argument +
                ", displayOptions=" + displayOptions +
                ", displayArguments=" + displayArguments +
                ", completeOptionName=" + completeOptionName +
                ", offset=" + offset +
                '}';
    }
}
