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
package org.aesh.util.completer;

import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;

/**
 * Generates bash completion scripts.
 *
 * @author Aesh team
 */
public class BashCompletionGenerator implements ShellCompletionGenerator {

    private static final String NL = "\n";

    @Override
    public String generate(CommandLineParser<? extends CommandInvocation> parser, String programName) {
        StringBuilder out = new StringBuilder();
        out.append(generateHeader(programName));
        out.append(generateMainFunction(parser, programName));
        generateCommandFunctions(out, parser, programName);
        out.append(generateFooter(programName));
        return out.toString();
    }

    private void generateCommandFunctions(StringBuilder out,
            CommandLineParser<? extends CommandInvocation> parser, String prefix) {
        generateCommandFunction(out, parser, prefix);
        if (parser.isGroupCommand()) {
            for (CommandLineParser<? extends CommandInvocation> child : parser.getAllChildParsers()) {
                String childPrefix = prefix + "_" + child.getProcessedCommand().name().toLowerCase();
                generateCommandFunctions(out, child, childPrefix);
            }
        }
    }

    private String generateMainFunction(CommandLineParser<? extends CommandInvocation> parser,
            String programName) {
        StringBuilder sb = new StringBuilder();
        sb.append("_complete_").append(programName).append("() {").append(NL);
        sb.append("    local cur prev words cword").append(NL);
        sb.append("    _init_completion || return").append(NL);
        sb.append(NL);

        if (parser.isGroupCommand()) {
            sb.append("    local subcmd=\"\"").append(NL);
            sb.append("    local i").append(NL);
            sb.append("    for ((i=1; i < cword; i++)); do").append(NL);
            sb.append("        case \"${words[i]}\" in").append(NL);
            for (CommandLineParser<? extends CommandInvocation> child : parser.getAllChildParsers()) {
                sb.append("            ").append(child.getProcessedCommand().name().toLowerCase())
                        .append(") subcmd=\"").append(child.getProcessedCommand().name().toLowerCase())
                        .append("\"; break;;").append(NL);
            }
            sb.append("        esac").append(NL);
            sb.append("    done").append(NL);
            sb.append(NL);
            sb.append("    case \"$subcmd\" in").append(NL);
            for (CommandLineParser<? extends CommandInvocation> child : parser.getAllChildParsers()) {
                String childName = child.getProcessedCommand().name().toLowerCase();
                sb.append("        ").append(childName).append(") _cmd_")
                        .append(programName).append("_").append(childName).append("; return;;").append(NL);
            }
            sb.append("    esac").append(NL);
            sb.append(NL);
        }

        sb.append("    _cmd_").append(programName).append(NL);
        sb.append("}").append(NL).append(NL);
        return sb.toString();
    }

    private void generateCommandFunction(StringBuilder out,
            CommandLineParser<? extends CommandInvocation> parser, String prefix) {
        String funcName = "_cmd_" + prefix;
        out.append(funcName).append("() {").append(NL);

        StringBuilder noValueOpts = new StringBuilder();
        StringBuilder valueOpts = new StringBuilder();
        boolean hasFileOption = false;

        for (ProcessedOption option : parser.getProcessedCommand().getOptions()) {
            if (option.isProperty())
                continue;

            StringBuilder target = option.hasValue() ? valueOpts : noValueOpts;

            target.append(" --").append(option.name());
            if (option.shortName() != null && !option.shortName().isEmpty())
                target.append(" -").append(option.shortName());

            for (String alias : option.getAliases()) {
                target.append(" --").append(alias);
            }

            if (option.isNegatable() && option.getNegatedName() != null) {
                noValueOpts.append(" --").append(option.getNegatedName());
            }

            if (option.isTypeAssignableByResourcesOrFile())
                hasFileOption = true;
        }

        StringBuilder childNames = new StringBuilder();
        if (parser.isGroupCommand()) {
            for (CommandLineParser<? extends CommandInvocation> child : parser.getAllChildParsers()) {
                childNames.append(" ").append(child.getProcessedCommand().name().toLowerCase());
            }
        }

        out.append("    local no_value_opts=\"").append(noValueOpts).append("\"").append(NL);
        out.append("    local value_opts=\"").append(valueOpts).append("\"").append(NL);
        if (childNames.length() > 0)
            out.append("    local subcmds=\"").append(childNames).append("\"").append(NL);
        out.append(NL);

        boolean hasValueOptions = false;
        for (ProcessedOption option : parser.getProcessedCommand().getOptions()) {
            if (option.hasValue() && !option.isProperty())
                hasValueOptions = true;
        }

        if (hasValueOptions) {
            out.append("    case \"$prev\" in").append(NL);
            for (ProcessedOption option : parser.getProcessedCommand().getOptions()) {
                if (!option.hasValue() || option.isProperty())
                    continue;

                StringBuilder pattern = new StringBuilder();
                pattern.append("--").append(option.name());
                if (option.shortName() != null && !option.shortName().isEmpty())
                    pattern.append("|-").append(option.shortName());
                for (String alias : option.getAliases())
                    pattern.append("|--").append(alias);

                out.append("        ").append(pattern).append(")").append(NL);

                if (option.isTypeAssignableByResourcesOrFile()) {
                    out.append("            _filedir").append(NL);
                    out.append("            return;;").append(NL);
                } else if (option.hasDefaultValue() || isBooleanType(option)) {
                    StringBuilder vals = new StringBuilder();
                    for (String v : option.getDefaultValues())
                        vals.append(v).append(" ");
                    if (isBooleanType(option))
                        vals.append("true false");
                    out.append("            COMPREPLY=( $(compgen -W \"").append(vals.toString().trim())
                            .append("\" -- \"$cur\") )").append(NL);
                    out.append("            return;;").append(NL);
                } else {
                    out.append("            return;;").append(NL);
                }
            }
            out.append("    esac").append(NL);
            out.append(NL);
        }

        if (parser.getProcessedCommand().hasArguments() || parser.getProcessedCommand().hasArgument()) {
            ProcessedOption arg = parser.getProcessedCommand().hasArguments()
                    ? parser.getProcessedCommand().getArguments()
                    : parser.getProcessedCommand().getArgument();
            if (arg.isTypeAssignableByResourcesOrFile()) {
                out.append("    if [[ \"$cur\" != -* ]]; then").append(NL);
                out.append("        _filedir").append(NL);
                out.append("        return").append(NL);
                out.append("    fi").append(NL);
                out.append(NL);
            }
        }

        out.append("    if [[ \"$cur\" == -* ]]; then").append(NL);
        out.append("        COMPREPLY=( $(compgen -W \"$no_value_opts $value_opts\" -- \"$cur\") )").append(NL);
        out.append("    else").append(NL);
        if (childNames.length() > 0) {
            out.append("        COMPREPLY=( $(compgen -W \"$subcmds\" -- \"$cur\") )").append(NL);
        } else if (hasFileOption) {
            out.append("        _filedir").append(NL);
        } else {
            out.append("        COMPREPLY=( $(compgen -W \"$no_value_opts $value_opts\" -- \"$cur\") )").append(NL);
        }
        out.append("    fi").append(NL);
        out.append("}").append(NL).append(NL);
    }

    @Override
    public String generateDynamic(CommandLineParser<? extends CommandInvocation> parser, String programName) {
        return "#!/usr/bin/env bash" + NL +
                NL +
                "# Dynamic bash completion for " + programName + " — generated by Aesh." + NL +
                "# Source this file or place it in /etc/bash_completion.d/" + NL +
                NL +
                "_complete_" + programName + "() {" + NL +
                "    local cur=\"${COMP_WORDS[COMP_CWORD]}\"" + NL +
                "    local IFS=$'\\n'" + NL +
                "    COMPREPLY=( $(compgen -W \"$(" + NL +
                "        " + programName + " --aesh-complete -- \"${COMP_WORDS[@]:1}\"" + NL +
                "    )\" -- \"$cur\") )" + NL +
                "}" + NL +
                "complete -o default -F _complete_" + programName + " " + programName + NL;
    }

    static boolean isBooleanType(ProcessedOption option) {
        return option.type() == Boolean.class || option.type() == boolean.class;
    }

    private String generateHeader(String programName) {
        return "#!/usr/bin/env bash" + NL +
                NL +
                "# Bash completion for " + programName + " — generated by Aesh." + NL +
                "# Source this file or place it in /etc/bash_completion.d/" + NL +
                NL +
                "# Fallback if bash-completion is not installed" + NL +
                "type _init_completion &>/dev/null || _init_completion() {" + NL +
                "    COMPREPLY=()" + NL +
                "    cur=\"${COMP_WORDS[COMP_CWORD]}\"" + NL +
                "    prev=\"${COMP_WORDS[COMP_CWORD-1]}\"" + NL +
                "    words=(\"${COMP_WORDS[@]}\")" + NL +
                "    cword=$COMP_CWORD" + NL +
                "}" + NL +
                NL +
                "# Fallback if _filedir is not available" + NL +
                "type _filedir &>/dev/null || _filedir() { COMPREPLY=( $(compgen -f -- \"$cur\") ); }" + NL +
                NL;
    }

    private String generateFooter(String programName) {
        return "complete -o default -F _complete_" + programName + " " + programName + NL;
    }
}
