package org.aesh.command.impl.converter;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.validator.OptionValidatorException;

public class ShortConverter implements Converter<Short, ConverterInvocation> {
    @Override
    public Short convert(ConverterInvocation input) throws OptionValidatorException {
        try {
            return Short.parseShort(input.getInput().trim());
        } catch (NumberFormatException e) {
            throw new OptionValidatorException(
                    "Invalid short value: '" + input.getInput() + "'");
        }
    }
}
