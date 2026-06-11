package org.aesh.command.impl.converter;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.validator.OptionValidatorException;

public class IntegerConverter implements Converter<Integer, ConverterInvocation> {
    @Override
    public Integer convert(ConverterInvocation input) throws OptionValidatorException {
        try {
            return Integer.parseInt(input.getInput().trim());
        } catch (NumberFormatException e) {
            throw new OptionValidatorException(
                    "Invalid integer value: '" + input.getInput() + "'");
        }
    }
}
