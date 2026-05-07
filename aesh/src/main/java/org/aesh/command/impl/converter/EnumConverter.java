package org.aesh.command.impl.converter;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class EnumConverter implements Converter<Enum, ConverterInvocation> {

    private final Class<? extends Enum> enumType;

    public EnumConverter(Class<? extends Enum> enumType) {
        this.enumType = enumType;
    }

    @Override
    public Enum convert(ConverterInvocation input) {
        String value = input.getInput();
        for (Enum constant : enumType.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value)) {
                return constant;
            }
        }
        StringBuilder valid = new StringBuilder();
        for (Enum constant : enumType.getEnumConstants()) {
            if (valid.length() > 0)
                valid.append(", ");
            valid.append(constant.name().toLowerCase());
        }
        throw new IllegalArgumentException("Invalid value '" + value
                + "'. Valid values: " + valid);
    }
}
