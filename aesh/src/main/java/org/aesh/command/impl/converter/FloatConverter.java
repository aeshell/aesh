package org.aesh.command.impl.converter;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;

public class FloatConverter implements Converter<Float, ConverterInvocation> {
    @Override
    public Float convert(ConverterInvocation input) {
        return Float.parseFloat(input.getInput());
    }
}
