package org.jboss.aesh.parser;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public enum ParserStatus {
    OK,
    UNCLOSED_QUOTE,
    DOUBLE_UNCLOSED_QUOTE,
    REQUIRED_OPTION_MISSING,
    ARGUMENTS_GIVE_NONE_DEFINED,

}
