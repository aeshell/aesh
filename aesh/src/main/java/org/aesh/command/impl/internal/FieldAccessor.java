package org.aesh.command.impl.internal;

public interface FieldAccessor {
    void set(Object instance, Object value);

    Object get(Object instance);
}
