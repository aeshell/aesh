package org.aesh.console;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

/**
 * Command mapping for shell escape (#568). Pure function tests — no OS
 * process is started, so Windows branches are covered on any platform.
 */
public class NativeExecutionTest {

    @Test
    public void testWindowsBatchUsesCmd() {
        assertArrayEquals(new String[] { "cmd", "/c", "deploy.bat --prod" },
                NativeCommand.build("deploy.bat --prod", true, false));
    }

    @Test
    public void testWindowsBareCommandUsesCmd() {
        assertArrayEquals(new String[] { "cmd", "/c", "dir" },
                NativeCommand.build("dir", true, true));
    }

    @Test
    public void testWindowsPs1PrefersPwsh() {
        assertArrayEquals(
                new String[] { "pwsh", "-ExecutionPolicy", "Bypass", "-File", "deploy.ps1", "--prod" },
                NativeCommand.build("deploy.ps1 --prod", true, true));
    }

    @Test
    public void testWindowsPs1FallsBackToPowershell() {
        assertArrayEquals(
                new String[] { "powershell", "-ExecutionPolicy", "Bypass", "-File", "deploy.ps1" },
                NativeCommand.build("deploy.ps1", true, false));
    }

    @Test
    public void testWindowsPs1ExtensionCaseInsensitive() {
        assertArrayEquals(
                new String[] { "powershell", "-ExecutionPolicy", "Bypass", "-File", "DEPLOY.PS1" },
                NativeCommand.build("DEPLOY.PS1", true, false));
    }

    @Test
    public void testWindowsPs1QuotedPathWithSpaces() {
        assertArrayEquals(
                new String[] { "pwsh", "-ExecutionPolicy", "Bypass", "-File", "C:\\My Scripts\\deploy.ps1",
                        "--prod" },
                NativeCommand.build("\"C:\\My Scripts\\deploy.ps1\" --prod", true, true));
    }

    @Test
    public void testUnixUsesSh() {
        assertArrayEquals(new String[] { "sh", "-c", "ls -la" },
                NativeCommand.build("ls -la", false, false));
    }

    @Test
    public void testUnixPs1UsesPwshWhenAvailable() {
        assertArrayEquals(
                new String[] { "pwsh", "-ExecutionPolicy", "Bypass", "-File", "deploy.ps1" },
                NativeCommand.build("deploy.ps1", false, true));
    }

    @Test
    public void testUnixPs1FallsBackToSh() {
        assertArrayEquals(new String[] { "sh", "-c", "deploy.ps1" },
                NativeCommand.build("deploy.ps1", false, false));
    }
}
