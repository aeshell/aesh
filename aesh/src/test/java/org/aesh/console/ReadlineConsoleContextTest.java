package org.aesh.console;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.io.Resource;
import org.junit.Test;

/**
 * Tests that ReadlineConsole propagates an explicitly configured AeshContext to its own runtime,
 * instead of always creating a fresh default context. Commands executed through the interactive
 * console must observe exactly the instance provided via SettingsBuilder.aeshContext(...).
 */
public class ReadlineConsoleContextTest {

    private static AeshContext customContext() {
        return new AeshContext() {
            @Override
            public Resource getCurrentWorkingDirectory() {
                return null;
            }

            @Override
            public void setCurrentWorkingDirectory(Resource cwd) {
            }

            @Override
            public Set<String> exportedVariableNames() {
                return Collections.emptySet();
            }

            @Override
            public String exportedVariable(String key) {
                return null;
            }
        };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void explicitSettingsContextIsUsedByConsole() {
        AeshContext custom = customContext();
        Settings settings = SettingsBuilder.builder().aeshContext(custom).build();
        ReadlineConsole console = new ReadlineConsole(settings);

        assertSame("console must use the explicitly configured context, not a fresh default",
                custom, console.context());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void unsetSettingsFallBackToDefaultContext() {
        Settings settings = SettingsBuilder.builder().build();
        ReadlineConsole console = new ReadlineConsole(settings);

        assertNotNull(console.context());
        assertTrue("without an explicit context the console keeps its managed default",
                console.context() instanceof DefaultAeshContext);
    }
}
