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
package org.aesh.console;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Builds the OS command line for a shell escape invocation.
 * PowerShell scripts are dispatched to pwsh/powershell since neither
 * {@code cmd /c} nor {@code sh -c} can run them; everything else keeps
 * the default shell handling.
 */
final class NativeCommand {

    private static volatile Boolean pwshAvailable;

    private NativeCommand() {
    }

    static String[] build(String command, boolean isWindows) {
        return build(command, isWindows, isPwshAvailable(isWindows));
    }

    static String[] build(String command, boolean isWindows, boolean pwshAvailable) {
        String[] tokens = split(command);
        if (tokens.length > 0 && tokens[0].toLowerCase().endsWith(".ps1")) {
            if (isWindows || pwshAvailable) {
                String shell = pwshAvailable ? "pwsh" : "powershell";
                String[] result = new String[tokens.length + 4];
                result[0] = shell;
                result[1] = "-ExecutionPolicy";
                result[2] = "Bypass";
                result[3] = "-File";
                System.arraycopy(tokens, 0, result, 4, tokens.length);
                return result;
            }
        }
        if (isWindows)
            return new String[] { "cmd", "/c", command };
        return new String[] { "sh", "-c", command };
    }

    private static String[] split(String command) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (quote != 0) {
                if (c == quote)
                    quote = 0;
                else
                    current.append(c);
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0)
            tokens.add(current.toString());
        return tokens.toArray(new String[0]);
    }

    static boolean isPwshAvailable(boolean isWindows) {
        Boolean cached = pwshAvailable;
        if (cached != null)
            return cached;
        boolean available = probePwsh(isWindows);
        pwshAvailable = available;
        return available;
    }

    private static boolean probePwsh(boolean isWindows) {
        try {
            java.lang.Process probe = isWindows
                    ? new ProcessBuilder("where", "pwsh").start()
                    : new ProcessBuilder("sh", "-c", "command -v pwsh").start();
            boolean finished = probe.waitFor(5, TimeUnit.SECONDS);
            return finished && probe.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException)
                Thread.currentThread().interrupt();
            return false;
        }
    }
}
