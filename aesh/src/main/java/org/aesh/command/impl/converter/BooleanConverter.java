package org.aesh.command.impl.converter;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;

public class BooleanConverter implements Converter<Boolean, ConverterInvocation> {
    @Override
    public Boolean convert(ConverterInvocation input) {
        return Boolean.parseBoolean(input.getInput());
    }
}
