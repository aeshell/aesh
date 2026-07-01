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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command.internal;

import org.aesh.command.alias.AliasCommand;
import org.aesh.command.alias.UnAliasCommand;
import org.aesh.command.export.ExportCommand;
import org.aesh.command.metadata.CommandMetadataProvider;
import org.aesh.command.metadata.MetadataRegistry;

/**
 * Hand-written metadata registry for aesh's internal commands.
 * <p>
 * These commands cannot use the annotation processor because the aesh module
 * cannot depend on aesh-processor (circular dependency). The metadata providers
 * are inner classes of each command, giving them direct access to private fields
 * without reflection.
 *
 * @author Aesh team
 * @since 3.16
 */
public final class InternalCommandMetadataRegistry implements MetadataRegistry {

    @Override
    public CommandMetadataProvider<?> get(String commandClassName) {
        switch (commandClassName) {
            case "org.aesh.command.alias.AliasCommand":
                return new AliasCommand.Metadata();
            case "org.aesh.command.alias.UnAliasCommand":
                return new UnAliasCommand.Metadata();
            case "org.aesh.command.export.ExportCommand":
                return new ExportCommand.Metadata();
            default:
                return null;
        }
    }
}
