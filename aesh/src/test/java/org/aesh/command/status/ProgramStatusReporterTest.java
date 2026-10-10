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
package org.aesh.command.status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.terminal.utils.ProgramStatus;
import org.aesh.tty.TestConnection;
import org.junit.Test;

/**
 * Phase 1 (#676): the reporting context and control-output path, with
 * disabled/headless behavior proven before any lifecycle wiring.
 * Raw-byte assertions use {@code TestConnection(false)} so the protocol
 * bytes cannot be stripped away unnoticed.
 */
public class ProgramStatusReporterTest {

    private static ProgramStatus working(String message) {
        return ProgramStatus.builder(ProgramStatus.State.WORKING).message(message).build();
    }

    @Test
    public void testDisabledReportsNothing() {
        TestConnection connection = new TestConnection(false);
        ProgramStatusReporter reporter = ProgramStatusReporter.create(false, "app", connection);

        assertFalse(reporter.isEnabled());
        assertFalse(reporter.report(working("Deploying")));
        assertEquals("", connection.getOutputBuffer());
    }

    @Test
    public void testDisabledSingleton() {
        assertSame(ProgramStatusReporter.disabled(),
                ProgramStatusReporter.create(false, "app", new TestConnection(false)));
        assertSame(ProgramStatusReporter.disabled(),
                ProgramStatusReporter.create(true, "app", null));
        assertFalse(ProgramStatusReporter.disabled().report(working("Deploying")));
    }

    @Test
    public void testNullStatusReturnsFalse() {
        TestConnection connection = new TestConnection(false);
        ProgramStatusReporter reporter = ProgramStatusReporter.create(true, null, connection);

        assertFalse(reporter.report(null));
        assertEquals("", connection.getOutputBuffer());
    }

    @Test
    public void testWorkingReportWritesExactBytes() {
        TestConnection connection = new TestConnection(false);
        ProgramStatusReporter reporter = ProgramStatusReporter.create(true, null, connection);
        ProgramStatus status = working("Deploying");

        assertTrue(reporter.isEnabled());
        assertTrue(reporter.report(status));
        assertEquals(status.toSequence(), connection.getOutputBuffer());
    }

    @Test
    public void testAppNameAppliedWhenAbsent() {
        TestConnection connection = new TestConnection(false);
        ProgramStatusReporter reporter = ProgramStatusReporter.create(true, "deploy", connection);

        assertTrue(reporter.report(working("Deploying")));
        assertEquals(ProgramStatus.builder(ProgramStatus.State.WORKING)
                .app("deploy").message("Deploying").build().toSequence(),
                connection.getOutputBuffer());
    }

    @Test
    public void testExplicitAppNamePreserved() {
        TestConnection connection = new TestConnection(false);
        ProgramStatusReporter reporter = ProgramStatusReporter.create(true, "deploy", connection);
        ProgramStatus status = ProgramStatus.builder(ProgramStatus.State.WORKING)
                .app("other").message("Deploying").build();

        assertTrue(reporter.report(status));
        assertEquals(status.toSequence(), connection.getOutputBuffer());
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testSettingsRoundTrip() {
        Settings settings = SettingsBuilder.builder()
                .enableProgramStatus(true)
                .programStatusAppName("deploy")
                .build();
        assertTrue(settings.programStatusEnabled());
        assertEquals("deploy", settings.programStatusAppName());

        Settings copy = new SettingsBuilder(settings).build();
        assertTrue(copy.programStatusEnabled());
        assertEquals("deploy", copy.programStatusAppName());

        Settings defaults = SettingsBuilder.builder().build();
        assertFalse(defaults.programStatusEnabled());
        assertEquals(null, defaults.programStatusAppName());
    }
}
