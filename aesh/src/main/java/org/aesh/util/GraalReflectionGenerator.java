package org.aesh.util;

import org.aesh.AeshRuntimeRunner;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.util.graal.GraalReflectionCommand;

/**
 * @author Aesh team
 */
public class GraalReflectionGenerator {

    public static void main(String[] args) throws CommandRegistryException {
        AeshRuntimeRunner.builder().command(GraalReflectionCommand.class).args(args).execute();
    }
}
