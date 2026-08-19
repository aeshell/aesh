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
package org.aesh.command.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.aesh.AeshRuntimeRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.junit.Test;

/**
 * Tests for issue #584: custom enum converter not invoked because
 * auto-populated allowedValues rejects the input before the converter runs.
 */
public class CustomEnumConverterTest {

    // Enum with annoying prefix
    public enum CRS {
        epsg_4326,
        epsg_3857,
        epsg_32632
    }

    // Custom converter that accepts input without the epsg_ prefix
    public static class CrsConverter implements Converter<CRS, ConverterInvocation> {
        @Override
        public CRS convert(ConverterInvocation input) {
            String value = input.getInput();
            // Try exact match first
            for (CRS crs : CRS.values()) {
                if (crs.name().equalsIgnoreCase(value))
                    return crs;
            }
            // Try without prefix
            for (CRS crs : CRS.values()) {
                String withoutPrefix = crs.name().substring("epsg_".length());
                if (withoutPrefix.equalsIgnoreCase(value))
                    return crs;
            }
            throw new IllegalArgumentException("Unknown CRS: " + value);
        }
    }

    @CommandDefinition(name = "geo", description = "Geo command")
    public static class GeoCommand implements Command<CommandInvocation> {
        @Option(name = "crs", converter = CrsConverter.class)
        private CRS crs;

        static CRS lastCrs;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            lastCrs = crs;
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testCustomConverterAcceptsShortForm() {
        GeoCommand.lastCrs = null;
        CommandResult result = AeshRuntimeRunner.builder()
                .command(GeoCommand.class)
                .args("--crs", "4326")
                .execute();
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals(CRS.epsg_4326, GeoCommand.lastCrs);
    }

    @Test
    public void testCustomConverterAcceptsFullForm() {
        GeoCommand.lastCrs = null;
        CommandResult result = AeshRuntimeRunner.builder()
                .command(GeoCommand.class)
                .args("--crs", "epsg_4326")
                .execute();
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals(CRS.epsg_4326, GeoCommand.lastCrs);
    }

    @Test
    public void testCustomConverterMultipleValues() {
        GeoCommand.lastCrs = null;
        CommandResult result = AeshRuntimeRunner.builder()
                .command(GeoCommand.class)
                .args("--crs", "3857")
                .execute();
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals(CRS.epsg_3857, GeoCommand.lastCrs);
    }

    // Enum WITHOUT custom converter should still auto-populate allowedValues
    public enum Color {
        RED,
        GREEN,
        BLUE
    }

    @CommandDefinition(name = "paint", description = "Paint command")
    public static class PaintCommand implements Command<CommandInvocation> {
        @Option(name = "color")
        private Color color;

        static Color lastColor;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            lastColor = color;
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testDefaultEnumConverterStillValidates() {
        PaintCommand.lastColor = null;
        CommandResult result = AeshRuntimeRunner.builder()
                .command(PaintCommand.class)
                .args("--color", "red")
                .execute();
        assertEquals(CommandResult.SUCCESS, result);
        assertEquals(Color.RED, PaintCommand.lastColor);
    }

    @Test
    public void testDefaultEnumConverterRejectsInvalid() {
        // Without custom converter, invalid values should still be rejected
        // AeshRuntimeRunner wraps OptionValidatorException in RuntimeException
        boolean rejected = false;
        try {
            AeshRuntimeRunner.builder()
                    .command(PaintCommand.class)
                    .args("--color", "purple")
                    .execute();
        } catch (RuntimeException e) {
            rejected = true;
            assertTrue("Should mention allowed values",
                    e.getMessage().contains("Allowed values") || e.getCause().getMessage().contains("Allowed values"));
        }
        assertTrue("Invalid enum value should be rejected", rejected);
    }
}
