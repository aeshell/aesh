package org.aesh.command.jbang;

import org.aesh.command.option.Mixin;
import org.aesh.command.option.Option;

public abstract class BaseBuildCommand extends BaseCommand {
    @Mixin
    ScriptMixin scriptMixin;

    @Mixin
    BuildMixin buildMixin;

    @Mixin
    DependencyInfoMixin dependencyInfoMixin;

    @Mixin
    NativeMixin nativeMixin;

    @Option(name = "build-dir", description = "Use given directory for build results")
    String buildDir;

    @Option(name = "enable-preview", hasValue = false, description = "Activate Java preview features")
    boolean enablePreview;
}
