package org.aesh.command.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.AeshCommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionGroup;
import org.aesh.command.option.OptionList;
import org.junit.Test;

/**
 * Comprehensive tests for @OptionList and @OptionGroup parsing.
 * Covers all value attachment syntaxes: space-separated, equals-attached,
 * and bare-attached (short name only).
 * <p>
 * These tests operate at the parser level (raw string values) without
 * needing InvocationProviders for type conversion.
 */
public class OptionListParserTest {

    // ========== @OptionList: short name attached values (#541) ==========

    @Test
    public void testOptionList_ShortNameAttachedValue() throws Exception {
        List<String> vals = optionValues(RuntimeOptsCmd.class, "test -R-Xmx4G", "runtime-option");
        assertEquals(1, vals.size());
        assertEquals("-Xmx4G", vals.get(0));
    }

    @Test
    public void testOptionList_ShortNameAttachedMultipleTokens() throws Exception {
        List<String> vals = optionValues(RuntimeOptsCmd.class, "test -R-Xmx4G -R-Xms4G", "runtime-option");
        assertEquals(2, vals.size());
        assertEquals("-Xmx4G", vals.get(0));
        assertEquals("-Xms4G", vals.get(1));
    }

    @Test
    public void testOptionList_ShortNameAttachedValueWithColon() throws Exception {
        // The hang reproducer from #541
        List<String> vals = optionValues(RuntimeOptsCmd.class, "test -R-Xlog:cpu", "runtime-option");
        assertEquals(1, vals.size());
        assertEquals("-Xlog:cpu", vals.get(0));
    }

    @Test
    public void testOptionList_ShortNameAttachedThenPositional() throws Exception {
        List<String> vals = optionValues(RuntimeOptsCmd.class, "test -R-Xmx4G server.jar", "runtime-option");
        assertEquals(1, vals.size());
        assertEquals("-Xmx4G", vals.get(0));
        assertEquals("server.jar", argumentValue(RuntimeOptsCmd.class, "test -R-Xmx4G server.jar"));
    }

    @Test
    public void testOptionList_ShortNameAttachedMultipleThenPositional() throws Exception {
        String line = "test -R-Xmx4G -R-Xms4G server.jar";
        List<String> vals = optionValues(RuntimeOptsCmd.class, line, "runtime-option");
        assertEquals(2, vals.size());
        assertEquals("-Xmx4G", vals.get(0));
        assertEquals("-Xms4G", vals.get(1));
        assertEquals("server.jar", argumentValue(RuntimeOptsCmd.class, line));
    }

    @Test
    public void testOptionList_ShortNameAttachedWithSpecialChars() throws Exception {
        // Value contains =, :, / — common in JVM args
        List<String> vals = optionValues(RuntimeOptsCmd.class,
                "test -R-agentlib:jdwp=transport=dt_socket", "runtime-option");
        assertEquals(1, vals.size());
        assertEquals("-agentlib:jdwp=transport=dt_socket", vals.get(0));
    }

    @Test
    public void testOptionList_ShortNameAttachedCommaInValueSplits() throws Exception {
        // Default separator is comma — commas in attached values ARE split
        List<String> vals = optionValues(RuntimeOptsCmd.class,
                "test -Rfoo,bar", "runtime-option");
        assertEquals("Commas should split into separate values", 2, vals.size());
        assertEquals("foo", vals.get(0));
        assertEquals("bar", vals.get(1));
    }

    // ========== @OptionList: space-separated values (regression) ==========

    @Test
    public void testOptionList_ShortNameSpaceSeparated() throws Exception {
        List<String> vals = optionValues(RuntimeOptsCmd.class, "test -R -Xmx4G", "runtime-option");
        assertEquals(1, vals.size());
        assertEquals("-Xmx4G", vals.get(0));
    }

    @Test
    public void testOptionList_ShortNameSpaceMultiple() throws Exception {
        List<String> vals = optionValues(RuntimeOptsCmd.class,
                "test -R -Xmx4G -R -Xms4G", "runtime-option");
        assertEquals(2, vals.size());
        assertEquals("-Xmx4G", vals.get(0));
        assertEquals("-Xms4G", vals.get(1));
    }

    @Test
    public void testOptionList_LongNameSpaceSeparated() throws Exception {
        List<String> vals = optionValues(RuntimeOptsCmd.class,
                "test --runtime-option -Xmx4G", "runtime-option");
        assertEquals(1, vals.size());
        assertEquals("-Xmx4G", vals.get(0));
    }

    // ========== @OptionList: equals-attached values (regression) ==========

    @Test
    public void testOptionList_ShortNameEquals() throws Exception {
        List<String> vals = optionValues(RuntimeOptsCmd.class, "test -R=-Xmx4G", "runtime-option");
        assertEquals(1, vals.size());
        assertEquals("-Xmx4G", vals.get(0));
    }

    @Test
    public void testOptionList_LongNameEquals() throws Exception {
        List<String> vals = optionValues(RuntimeOptsCmd.class,
                "test --runtime-option=-Xmx4G", "runtime-option");
        assertEquals(1, vals.size());
        assertEquals("-Xmx4G", vals.get(0));
    }

    // ========== @OptionList: comma-separated values ==========

    @Test
    public void testOptionList_CommaSeparatedSpaced() throws Exception {
        List<String> vals = optionValues(CommaListCmd.class, "test -o foo,bar,baz", "option");
        assertEquals(3, vals.size());
        assertEquals("foo", vals.get(0));
        assertEquals("bar", vals.get(1));
        assertEquals("baz", vals.get(2));
    }

    @Test
    public void testOptionList_CommaSeparatedEquals() throws Exception {
        List<String> vals = optionValues(CommaListCmd.class, "test -o=foo,bar", "option");
        assertEquals(2, vals.size());
        assertEquals("foo", vals.get(0));
        assertEquals("bar", vals.get(1));
    }

    @Test
    public void testOptionList_CommaSeparatedAttached() throws Exception {
        List<String> vals = optionValues(CommaListCmd.class, "test -ofoo,bar", "option");
        assertEquals(2, vals.size());
        assertEquals("foo", vals.get(0));
        assertEquals("bar", vals.get(1));
    }

    @Test
    public void testOptionList_CommaSeparatedLongName() throws Exception {
        List<String> vals = optionValues(CommaListCmd.class,
                "test --option=alpha,beta,gamma", "option");
        assertEquals(3, vals.size());
        assertEquals("alpha", vals.get(0));
        assertEquals("beta", vals.get(1));
        assertEquals("gamma", vals.get(2));
    }

    // ========== @OptionList: mixed with other options ==========

    @Test
    public void testOptionList_MixedWithBooleanOption() throws Exception {
        String line = "test --verbose -R-Xmx4G server.jar";
        List<String> vals = optionValues(RuntimeOptsCmd.class, line, "runtime-option");
        assertEquals(1, vals.size());
        assertEquals("-Xmx4G", vals.get(0));
        // verbose should be set
        List<String> verboseVals = optionValues(RuntimeOptsCmd.class, line, "verbose");
        assertEquals("true", verboseVals.get(0));
    }

    @Test
    public void testOptionList_BooleanBeforeAndAfterList() throws Exception {
        String line = "test --verbose -R-Xmx4G --debug server.jar";
        List<String> vals = optionValues(RuntimeOptsCmd.class, line, "runtime-option");
        assertEquals(1, vals.size());
        assertEquals("-Xmx4G", vals.get(0));
        assertEquals("true", optionValues(RuntimeOptsCmd.class, line, "verbose").get(0));
        assertEquals("true", optionValues(RuntimeOptsCmd.class, line, "debug").get(0));
    }

    // ========== @OptionList: long name does NOT support bare attached ==========

    @Test
    public void testOptionList_LongNameBareAttached_Fails() throws Exception {
        assertTrue("Long name bare attached should fail",
                hasParserException(RuntimeOptsCmd.class, "test --runtime-option-Xmx4G"));
    }

    // ========== Infinite loop prevention (#541) ==========

    @Test(timeout = 5000)
    public void testOptionList_NoInfiniteLoopOnUnknownNextToken() throws Exception {
        // This was the hang reproducer from #541: -R-Xlog:cpu followed by
        // a positional argument caused an infinite loop because processList()
        // left status as null and doParse() couldn't handle the unconsumed word.
        // With the fix, this should complete in milliseconds, not hang forever.
        List<String> vals = optionValues(RuntimeOptsCmd.class,
                "test -R-Xlog:cpu env@jbangdev", "runtime-option");
        assertEquals(1, vals.size());
        assertEquals("-Xlog:cpu", vals.get(0));
    }

    // ========== @OptionGroup: existing syntax (regression) ==========

    @Test
    public void testOptionGroup_ShortNameKeyValue() throws Exception {
        Map<String, String> props = propertyValues(PropCmd.class, "test -DFoo=Bar", "define");
        assertEquals("Bar", props.get("Foo"));
    }

    @Test
    public void testOptionGroup_LongNameEqualsKeyValue() throws Exception {
        Map<String, String> props = propertyValues(PropCmd.class, "test --define=Foo=Bar", "define");
        assertEquals("Bar", props.get("Foo"));
    }

    @Test
    public void testOptionGroup_MultipleProperties() throws Exception {
        Map<String, String> props = propertyValues(PropCmd.class, "test -DFoo=Bar -DBaz=Qux", "define");
        assertEquals("Bar", props.get("Foo"));
        assertEquals("Qux", props.get("Baz"));
    }

    @Test
    public void testOptionGroup_PropertyWithDefaultValue() throws Exception {
        Map<String, String> props = propertyValues(PropWithDefaultCmd.class, "test -DFoo", "define");
        assertEquals("defaultVal", props.get("Foo"));
    }

    @Test
    public void testOptionGroup_PropertyValueWithEquals() throws Exception {
        // -Dkey=val=ue — value contains =
        Map<String, String> props = propertyValues(PropCmd.class, "test -Dkey=val=ue", "define");
        assertEquals("val=ue", props.get("key"));
    }

    @Test
    public void testOptionGroup_PropertyValueWithSpecialChars() throws Exception {
        // -Dpath=/usr/local/bin
        Map<String, String> props = propertyValues(PropCmd.class, "test -Dpath=/usr/local/bin", "define");
        assertEquals("/usr/local/bin", props.get("path"));
    }

    @Test
    public void testOptionGroup_MixedWithOtherOptions() throws Exception {
        String line = "test --verbose -DFoo=Bar -DBaz=Qux --debug";
        Map<String, String> props = propertyValues(MixedCmd.class, line, "define");
        assertEquals("Bar", props.get("Foo"));
        assertEquals("Qux", props.get("Baz"));
        assertEquals("true", optionValues(MixedCmd.class, line, "verbose").get(0));
        assertEquals("true", optionValues(MixedCmd.class, line, "debug").get(0));
    }

    // ========== Helpers ==========

    @SuppressWarnings("rawtypes")
    private AeshCommandLineParser createParser(Class<? extends Command> cmdClass) throws Exception {
        return (AeshCommandLineParser) new AeshCommandContainerBuilder<>().create(cmdClass).getParser();
    }

    @SuppressWarnings("rawtypes")
    private List<String> optionValues(Class<? extends Command> cmdClass,
            String line, String optionName) throws Exception {
        AeshCommandLineParser parser = createParser(cmdClass);
        parser.parse(line);
        if (parser.getProcessedCommand().parserExceptions().size() > 0)
            throw (Exception) parser.getProcessedCommand().parserExceptions().get(0);
        ProcessedOption opt = parser.getProcessedCommand().findLongOptionNoActivatorCheck(optionName);
        return opt != null ? opt.getValues() : Collections.emptyList();
    }

    @SuppressWarnings("rawtypes")
    private String argumentValue(Class<? extends Command> cmdClass, String line) throws Exception {
        AeshCommandLineParser parser = createParser(cmdClass);
        parser.parse(line);
        if (parser.getProcessedCommand().parserExceptions().size() > 0)
            throw (Exception) parser.getProcessedCommand().parserExceptions().get(0);
        ProcessedOption arg = parser.getProcessedCommand().getArgument();
        return arg != null ? arg.getValue() : null;
    }

    @SuppressWarnings("rawtypes")
    private Map<String, String> propertyValues(Class<? extends Command> cmdClass,
            String line, String optionName) throws Exception {
        AeshCommandLineParser parser = createParser(cmdClass);
        parser.parse(line);
        if (parser.getProcessedCommand().parserExceptions().size() > 0)
            throw (Exception) parser.getProcessedCommand().parserExceptions().get(0);
        ProcessedOption opt = parser.getProcessedCommand().findLongOptionNoActivatorCheck(optionName);
        return opt != null ? opt.getProperties() : Collections.emptyMap();
    }

    @SuppressWarnings("rawtypes")
    private boolean hasParserException(Class<? extends Command> cmdClass, String line) {
        try {
            AeshCommandLineParser parser = createParser(cmdClass);
            parser.parse(line);
            return parser.getProcessedCommand().parserExceptions().size() > 0;
        } catch (Exception e) {
            return true;
        }
    }

    // ========== Test commands ==========

    @CommandDefinition(name = "test", description = "Runtime options test")
    public static class RuntimeOptsCmd implements Command<CommandInvocation> {
        @OptionList(shortName = 'R', name = "runtime-option")
        public List<String> javaRuntimeOptions;

        @Option(hasValue = false, description = "Verbose")
        public boolean verbose;

        @Option(hasValue = false, description = "Debug")
        public boolean debug;

        @Argument(description = "Script file")
        public String script;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test", description = "Comma list test")
    public static class CommaListCmd implements Command<CommandInvocation> {
        @OptionList(shortName = 'o', name = "option", valueSeparator = ',')
        public List<String> items;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test", description = "Property test")
    public static class PropCmd implements Command<CommandInvocation> {
        @OptionGroup(shortName = 'D', name = "define", description = "Define properties")
        public Map<String, String> properties;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test", description = "Property with default")
    public static class PropWithDefaultCmd implements Command<CommandInvocation> {
        @OptionGroup(shortName = 'D', name = "define", description = "Properties", defaultValue = "defaultVal")
        public Map<String, String> properties;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test", description = "Mixed options")
    public static class MixedCmd implements Command<CommandInvocation> {
        @Option(hasValue = false)
        public boolean verbose;
        @Option(hasValue = false)
        public boolean debug;
        @OptionGroup(shortName = 'D', name = "define")
        public Map<String, String> properties;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }
}
