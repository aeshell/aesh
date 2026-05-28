package org.aesh.command.jbang;

import java.util.List;
import java.util.Map;

import org.aesh.command.option.Option;
import org.aesh.command.option.OptionGroup;
import org.aesh.command.option.OptionList;

public class RunMixin {
    @OptionList(shortName = 'R', name = "runtime-option", description = "Options for the Java runtime")
    List<String> javaRuntimeOptions;

    @Option(name = "jfr", fallbackValue = "filename={basename}.jfr", description = "Launch with Java Flight Recorder enabled")
    String flightRecorderString;

    @Option(shortName = 'd', name = "debug", fallbackValue = "4004", description = "Launch with java debug enabled")
    String debugString;

    @Option(name = "enableassertions", hasValue = false, description = "Enable assertions")
    boolean enableAssertions;

    @Option(name = "enablesystemassertions", hasValue = false, description = "Enable system assertions")
    boolean enableSystemAssertions;

    @OptionGroup(name = "javaagent", description = "Java agent slots")
    Map<String, String> javaAgentSlots;

    @Option(name = "cds", hasValue = false, negatable = true, description = "Enable Class Data Sharing")
    boolean cds;

    @Option(shortName = 'i', name = "interactive", hasValue = false, description = "Activate interactive mode")
    boolean interactive;
}
