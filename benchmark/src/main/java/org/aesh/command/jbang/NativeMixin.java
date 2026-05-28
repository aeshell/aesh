package org.aesh.command.jbang;

import java.util.List;

import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;

public class NativeMixin {
    @Option(name = "native-image", hasValue = false, description = "Build native image")
    boolean nativeImage;

    @OptionList(name = "native-option", description = "Options for native-image")
    List<String> nativeOptions;
}
