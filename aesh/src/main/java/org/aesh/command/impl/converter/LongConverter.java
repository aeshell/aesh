package org.aesh.command.impl.converter;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.validator.OptionValidatorException;

public class LongConverter implements Converter<Long, ConverterInvocation> {
    @Override
    public Long convert(ConverterInvocation input) throws OptionValidatorException {
        try {
            return Long.parseLong(input.getInput().trim());
        } catch (NumberFormatException e) {
            throw new OptionValidatorException(
                    "Invalid long value: '" + input.getInput() + "'");
        }
    }
}
