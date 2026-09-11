/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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
package org.aesh.builtins.diag;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.aesh.builtins.common.AeshTestCommons;
import org.aesh.builtins.ls.Ls;
import org.aesh.builtins.mkdir.Mkdir;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.io.PathResolver;
import org.aesh.terminal.utils.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Temporary Windows diagnostic. Prints resolve/exists/output state to the
 * surefire log so path handling can be debugged without a Windows machine.
 */
public class WindowsDiagTest extends AeshTestCommons {

    private Path tempDir;

    @Before
    public void before() throws IOException {
        tempDir = createTempDirectory();
    }

    @After
    public void after() throws IOException {
        try {
            deleteRecursiveTempDirectory(tempDir);
        } catch (Exception e) {
            System.out.println("DIAG cleanup failed: " + e);
        }
    }

    @Test
    public void testDiag() throws IOException, CommandRegistryException {
        StringBuilder report = new StringBuilder();
        report.append("os=").append(System.getProperty("os.name"));
        report.append(" sep=[").append(Config.getPathSeparator()).append(']');
        report.append(" temp=[").append(tempDir.toFile().getAbsolutePath()).append(']');

        prepare(Mkdir.class, Ls.class, org.aesh.builtins.cd.Cd.class);

        String target = tempDir.toFile().getAbsolutePath() + Config.getPathSeparator() + "diag_rocks";
        report.append(" target=[").append(target).append(']');

        List<File> resolved = PathResolver.resolvePath(new File(target),
                new File(System.getProperty("user.dir")));
        report.append(" resolved=").append(resolved);

        pushToOutput("cd " + tempDir.toFile().getAbsolutePath() + Config.getPathSeparator());
        report.append(" cwdAfterCd=[")
                .append(getAeshContext().getCurrentWorkingDirectory().getAbsolutePath()).append(']');

        File directTarget = new File(target + "_direct");
        boolean directResult = directTarget.mkdirs();
        report.append(" directMkdirs=").append(directResult);
        report.append(" directExists=").append(directTarget.exists());
        report.append(" parentExists=").append(new File(target).getParentFile().exists());
        report.append(" parentWritable=").append(new File(target).getParentFile().canWrite());

        pushToOutput("mkdir " + target);
        report.append(" exists=").append(new File(target).exists());
        report.append(" mkdirOut=[").append(getStream()).append(']');

        connection().clearOutputBuffer();
        pushToOutput("ls -l " + tempDir.toFile().getAbsolutePath());
        report.append(" lsOut=[").append(getStream()).append(']');

        System.out.println("DIAG " + report);
        finish();
    }
}
