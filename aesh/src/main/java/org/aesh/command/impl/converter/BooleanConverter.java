package org.aesh.command.impl.converter;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.validator.OptionValidatorException;

/**
 * Converts string input to Boolean with strict validation.
 * <p>
 * Accepted values (case-insensitive):
 * <ul>
 * <li>True: {@code true}, {@code yes}, {@code 1}</li>
 * <li>False: {@code false}, {@code no}, {@code 0}</li>
 * </ul>
 * Any other value throws {@link OptionValidatorException}.
 */
public class BooleanConverter implements Converter<Boolean, ConverterInvocation> {
    @Override
    public Boolean convert(ConverterInvocation input) throws OptionValidatorException {
        String value = input.getInput().trim().toLowerCase();
        switch (value) {
            case "true":
            case "yes":
            case "1":
                return Boolean.TRUE;
            case "false":
            case "no":
            case "0":
                return Boolean.FALSE;
            default:
                throw new OptionValidatorException(
                        "Invalid boolean value: '" + input.getInput()
                                + "'. Expected: true/false, yes/no, 1/0");
        }
    }
}
