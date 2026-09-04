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
package org.aesh.builtins.cd;

import java.io.IOException;

import org.aesh.command.registry.CommandRegistryException;
import org.aesh.builtins.common.AeshTestCommons;
import org.aesh.builtins.ls.Ls;
import org.aesh.terminal.utils.Config;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:danielsoro@gmail.com">Daniel Cunha (soro)</a>
 */
public class CdTest extends AeshTestCommons {

    @Test
    public void testCd() throws IOException, CommandRegistryException {
        prepare(Cd.class, Ls.class);
        if(Config.isOSPOSIXCompatible()) {
            pushToOutput("cd /tmp");
            assertEquals("/tmp", getAeshContext().getCurrentWorkingDirectory().getAbsolutePath());

            pushToOutput("cd /var/log");
            assertEquals("/var/log", getAeshContext().getCurrentWorkingDirectory().getAbsolutePath());

            pushToOutput("cd ..");
            assertEquals("/var", getAeshContext().getCurrentWorkingDirectory().getAbsolutePath());
            pushToOutput("cd ..");
            assertEquals("/", getAeshContext().getCurrentWorkingDirectory().getAbsolutePath());
            pushToOutput("cd ..");
            assertEquals("/", getAeshContext().getCurrentWorkingDirectory().getAbsolutePath());
        }
        finish();
    }
}
