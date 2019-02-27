/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command.settings;

import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.io.FileResource;
import org.aesh.io.Resource;
import org.aesh.readline.editing.EditMode;

import java.io.File;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class RuntimeSettings {

    public static Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation, OptionActivator, CommandActivator> readRuntimeProperties(
                                      Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                                                      OptionActivator, CommandActivator> settings) {
       SettingsBuilder<CommandInvocation, ConverterInvocation, CompleterInvocation,
                              ValidatorInvocation, OptionActivator, CommandActivator>
               builder = new SettingsBuilder<>(settings);

            String editMode = System.getProperty("aesh.editmode");
            if(editMode != null && editMode.length() > 0) {
                if(editMode.equalsIgnoreCase("VI"))
                    builder.mode(EditMode.Mode.VI);
                else if(editMode.equalsIgnoreCase("EMACS"))
                    builder.mode(EditMode.Mode.EMACS);
            }
            String readInputrc = System.getProperty("aesh.readinputrc");
            if(readInputrc != null && readInputrc.length() > 0)
                if(readInputrc.equalsIgnoreCase("true") ||
                        readInputrc.equalsIgnoreCase("false"))
                    builder.readInputrc(Boolean.parseBoolean(readInputrc));

            String inputrc = System.getProperty("aesh.inputrc");
            if(inputrc != null && inputrc.length() > 0)
                if(new File(inputrc).isFile())
                    builder.inputrc(new File(inputrc));

            String historyFile = System.getProperty("aesh.historyfile");
            if(historyFile != null && historyFile.length() > 0)
                if(new File(historyFile).isFile())
                    builder.historyFile(new File(historyFile));

            String historyPersistent = System.getProperty("aesh.historypersistent");
            if(historyPersistent != null && historyPersistent.length() > 0)
                if(historyPersistent.equalsIgnoreCase("true") ||
                        historyPersistent.equalsIgnoreCase("false"))
                    builder.persistHistory(Boolean.parseBoolean(historyPersistent));

            String historyDisabled = System.getProperty("aesh.historydisabled");
            if(historyDisabled != null && historyDisabled.length() > 0)
                if(historyDisabled.equalsIgnoreCase("true") ||
                        historyDisabled.equalsIgnoreCase("false"))
                    builder.disableHistory(Boolean.parseBoolean(historyDisabled));

            String historySize = System.getProperty("aesh.historysize");
            if(historySize != null && historySize.length() > 0)
                builder.historySize(Integer.parseInt(historySize));

            String doLogging = System.getProperty("aesh.logging");
            if(doLogging != null && doLogging.length() > 0)
                if(doLogging.equalsIgnoreCase("true") ||
                        doLogging.equalsIgnoreCase("false"))
                    builder.logging(Boolean.parseBoolean(doLogging));

            String logFile = System.getProperty("aesh.logfile");
            if(logFile != null && logFile.length() > 0)
                builder.logfile(logFile);

            String disableCompletion = System.getProperty("aesh.disablecompletion");
            if(disableCompletion != null && disableCompletion.length() > 0)
                if(disableCompletion.equalsIgnoreCase("true") ||
                        disableCompletion.equalsIgnoreCase("false"))
                    builder.disableCompletion(Boolean.parseBoolean(disableCompletion));

            String execute = System.getProperty("aesh.execute");
            if(execute != null && execute.length() > 0)
                builder.setExecuteAtStart(execute);

            String executeFile = System.getProperty("aesh.executefile");
            if(executeFile != null && executeFile.length() > 0) {
               Resource resourceFile =  new FileResource(executeFile);
                if(resourceFile.isLeaf())
                    builder.setExecuteFileAtStart(resourceFile);
            }

        return builder.build();
    }
}
