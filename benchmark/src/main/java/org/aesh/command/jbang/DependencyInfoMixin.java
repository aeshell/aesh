package org.aesh.command.jbang;

import java.util.List;
import java.util.Map;

import org.aesh.command.option.OptionGroup;
import org.aesh.command.option.OptionList;

public class DependencyInfoMixin {
    @OptionList(name = "deps", description = "Add additional dependencies")
    List<String> dependencies;

    @OptionList(name = "repos", description = "Add additional repositories")
    List<String> repositories;

    @OptionList(name = "cp", description = "Add classpaths")
    List<String> classpaths;

    @OptionGroup(shortName = 'D', description = "System properties")
    Map<String, String> properties;
}
