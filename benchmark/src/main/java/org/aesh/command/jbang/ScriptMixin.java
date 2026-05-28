package org.aesh.command.jbang;

import java.util.List;

import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;

public class ScriptMixin {
    @OptionList(shortName = 's', name = "sources", description = "Add additional sources")
    List<String> sources;

    @OptionList(name = "files", description = "Add additional files")
    List<String> resources;

    @Option(shortName = 'T', name = "source-type", description = "Force source type")
    String forceType;

    @Option(name = "catalog", description = "Path to catalog file")
    String catalog;

    @Argument(description = "A reference to a source file", paramLabel = "scriptOrFile")
    String scriptOrFile;
}
