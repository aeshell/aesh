package org.aesh.command.impl.converter;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;

public class ShortConverter implements Converter<Short, ConverterInvocation> {
    @Override
    public Short convert(ConverterInvocation input) {
        return Short.valueOf(input.getInput());
    }
}
