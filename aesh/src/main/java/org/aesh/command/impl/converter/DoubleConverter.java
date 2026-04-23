package org.aesh.command.impl.converter;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;

public class DoubleConverter implements Converter<Double, ConverterInvocation> {
    @Override
    public Double convert(ConverterInvocation input) {
        return Double.parseDouble(input.getInput());
    }
}
