package org.aesh.command.jbang;

import java.util.List;

import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;

public class BuildMixin {
    @Option(shortName = 'j', name = "java", description = "Java version to use")
    String javaVersion;

    @Option(shortName = 'm', name = "main", description = "Main class to use")
    String main;

    @Option(name = "module", description = "Module name")
    String module;

    @OptionList(shortName = 'C', name = "compile-option", description = "Compiler options")
    List<String> compileOptions;

    @OptionList(name = "manifest-option", description = "Manifest options")
    List<String> manifestOptions;

    @OptionList(name = "integration", description = "Integration classes")
    List<String> integrations;
}
