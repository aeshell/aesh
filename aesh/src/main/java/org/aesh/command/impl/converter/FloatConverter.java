package org.aesh.command.impl.converter;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.validator.OptionValidatorException;

public class FloatConverter implements Converter<Float, ConverterInvocation> {
    @Override
    public Float convert(ConverterInvocation input) throws OptionValidatorException {
        try {
            return Float.parseFloat(input.getInput().trim());
        } catch (NumberFormatException e) {
            throw new OptionValidatorException(
                    "Invalid float value: '" + input.getInput() + "'");
        }
    }
}
