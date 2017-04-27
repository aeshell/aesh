/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aesh.command.impl.operator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.console.AeshContext;

/**
 *
 * @author jdenise@redhat.com
 */
/**
 *
 * @author jdenise@redhat.com
 */
public class AppendOutputRedirectionOperator implements ConfigurationOperator {

    private class OutputDelegateImpl extends FileOutputDelegate {

        private OutputDelegateImpl(String file) throws IOException {
            super(context, file);
        }

        @Override
        protected BufferedWriter buildWriter(File f) throws IOException {
            return Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);
        }
    }

    private CommandInvocationConfiguration config;
    private String argument;
    private final AeshContext context;

    public AppendOutputRedirectionOperator(AeshContext context) {
        this.context = context;
    }

    @Override
    public CommandInvocationConfiguration getConfiguration() throws IOException {
        if (config == null) {
            config = new CommandInvocationConfiguration(context, new OutputDelegateImpl(argument));
        }
        return config;
    }

    @Override
    public void setArgument(String argument) {
        this.argument = argument;
    }
}
