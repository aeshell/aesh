package org.aesh.command.impl.converter;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.validator.OptionValidatorException;

public class DoubleConverter implements Converter<Double, ConverterInvocation> {
    @Override
    public Double convert(ConverterInvocation input) throws OptionValidatorException {
        try {
            return Double.parseDouble(input.getInput().trim());
        } catch (NumberFormatException e) {
            throw new OptionValidatorException(
                    "Invalid double value: '" + input.getInput() + "'");
        }
    }
}
