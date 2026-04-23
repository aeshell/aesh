package org.aesh.command.impl.converter;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;

public class ByteConverter implements Converter<Byte, ConverterInvocation> {
    @Override
    public Byte convert(ConverterInvocation input) {
        return Byte.valueOf(input.getInput());
    }
}
