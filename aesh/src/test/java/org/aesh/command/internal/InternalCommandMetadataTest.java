package org.aesh.command.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.aesh.command.alias.AliasCommand;
import org.aesh.command.alias.UnAliasCommand;
import org.aesh.command.export.ExportCommand;
import org.aesh.command.export.ExportManager;
import org.aesh.command.metadata.CommandMetadataProvider;
import org.aesh.command.metadata.MetadataProviderRegistry;
import org.aesh.readline.alias.AliasManager;
import org.junit.Test;

/**
 * Tests that aesh's internal commands (AliasCommand, UnAliasCommand, ExportCommand)
 * are discovered via MetadataProviderRegistry and use the generated metadata path
 * instead of reflection (#543).
 */
public class InternalCommandMetadataTest {

    @Test
    public void testAliasCommandHasProvider() {
        CommandMetadataProvider<?> provider = MetadataProviderRegistry.getProvider(AliasCommand.class);
        assertNotNull("AliasCommand should have a metadata provider", provider);
        assertEquals("alias", provider.commandName());
    }

    @Test
    public void testUnAliasCommandHasProvider() {
        CommandMetadataProvider<?> provider = MetadataProviderRegistry.getProvider(UnAliasCommand.class);
        assertNotNull("UnAliasCommand should have a metadata provider", provider);
        assertEquals("unalias", provider.commandName());
    }

    @Test
    public void testExportCommandHasProvider() {
        CommandMetadataProvider<?> provider = MetadataProviderRegistry.getProvider(ExportCommand.class);
        assertNotNull("ExportCommand should have a metadata provider", provider);
        assertEquals("export", provider.commandName());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAliasCommandBuildProcessedCommand() throws Exception {
        CommandMetadataProvider<AliasCommand> provider = (CommandMetadataProvider<AliasCommand>) MetadataProviderRegistry
                .getProvider(AliasCommand.class);
        assertNotNull(provider);

        AliasManager aliasManager = new AliasManager(null, false);
        AliasCommand instance = new AliasCommand(aliasManager);
        org.aesh.command.impl.internal.ProcessedCommand pc = provider.buildProcessedCommand(instance);

        assertNotNull("ProcessedCommand should be built", pc);
        assertEquals("alias", pc.name());
        assertTrue("Should have 'unalias' alias", pc.getAliases().contains("unalias"));
        assertNotNull("Should have --print option",
                pc.findLongOptionNoActivatorCheck("print"));
        assertNotNull("Should have arguments",
                pc.getArguments());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExportCommandBuildProcessedCommand() throws Exception {
        CommandMetadataProvider<ExportCommand> provider = (CommandMetadataProvider<ExportCommand>) MetadataProviderRegistry
                .getProvider(ExportCommand.class);
        assertNotNull(provider);

        // ExportCommand needs an ExportManager — create a minimal one
        java.io.File tmpFile = java.io.File.createTempFile("aesh-export-test", ".tmp");
        tmpFile.deleteOnExit();
        ExportManager exportManager = new ExportManager(tmpFile);
        ExportCommand instance = new ExportCommand(exportManager);
        org.aesh.command.impl.internal.ProcessedCommand pc = provider.buildProcessedCommand(instance);

        assertNotNull("ProcessedCommand should be built", pc);
        assertEquals("export", pc.name());
        assertNotNull("Should have arguments", pc.getArguments());
    }

    @Test
    public void testRegistryReturnsNullForUnknown() {
        InternalCommandMetadataRegistry registry = new InternalCommandMetadataRegistry();
        assertNull("Unknown class should return null",
                registry.get("com.example.UnknownCommand"));
    }
}
