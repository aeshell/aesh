package org.aesh.command.option;

/**
 * Controls how an option appears in help output and tab completion.
 */
public enum OptionVisibility {
    /** Always shown in help output. This is the default. */
    BRIEF,
    /** Only shown when full help is requested (e.g. --help=all). */
    FULL,
    /** Never shown in help output or tab completion. Still accepted when typed. */
    HIDDEN
}
