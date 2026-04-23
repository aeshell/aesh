package org.aesh.command.impl.converter;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;

public class IntegerConverter implements Converter<Integer, ConverterInvocation> {
    @Override
    public Integer convert(ConverterInvocation input) {
        return Integer.parseInt(input.getInput());
    }
}
