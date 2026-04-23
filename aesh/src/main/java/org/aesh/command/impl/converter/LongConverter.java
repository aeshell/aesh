package org.aesh.command.impl.converter;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;

public class LongConverter implements Converter<Long, ConverterInvocation> {
    @Override
    public Long convert(ConverterInvocation input) {
        return Long.parseLong(input.getInput());
    }
}
