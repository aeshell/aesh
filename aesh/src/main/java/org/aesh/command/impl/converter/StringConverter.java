package org.aesh.command.impl.converter;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;

public class StringConverter implements Converter<String, ConverterInvocation> {
    @Override
    public String convert(ConverterInvocation input) {
        return input.getInput();
    }
}
