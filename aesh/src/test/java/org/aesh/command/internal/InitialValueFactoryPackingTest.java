/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.aesh.command.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.function.Supplier;

import org.aesh.command.impl.internal.CollectionFactory;
import org.aesh.command.impl.internal.FieldAccessor;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedOption;
import org.junit.Test;

/**
 * Pins the initial-value factory packing (#797): a {@link CollectionFactory}
 * constant is stored in the option's flag bits without allocating the
 * {@code OptionExtras} holder, while a custom {@link Supplier} falls back to
 * the holder. Both restore correctly through
 * {@code GeneratedProcessedOption.restoreInitialValue}.
 */
public class InitialValueFactoryPackingTest {

    /** Mutable slot the test accessor writes into. */
    private static final class Slot {
        Object value;
    }

    private static ProcessedOption listOption(Slot slot) {
        return ProcessedOption.createDirect(null, "tags", "", java.util.List.class, "tags",
                OptionType.LIST, null, new FieldAccessor() {
                    @Override
                    public void set(Object instance, Object value) {
                        slot.value = value;
                    }

                    @Override
                    public Object get(Object instance) {
                        return slot.value;
                    }
                });
    }

    private static Object extrasOf(ProcessedOption option) throws Exception {
        Field extrasField = ProcessedOption.class.getDeclaredField("extras");
        extrasField.setAccessible(true);
        return extrasField.get(option);
    }

    @Test
    public void testCollectionFactoryConstantNeedsNoExtrasHolder() throws Exception {
        Slot slot = new Slot();
        ProcessedOption option = listOption(slot);
        option.setInitialValueFactory(CollectionFactory.LINKED_HASH_SET);
        // Packed into the flag bits: no OptionExtras allocated (#797)
        assertNull("CollectionFactory constant must not allocate OptionExtras", extrasOf(option));

        option.setInitialValue(new ArrayList<>(java.util.Arrays.asList("a", "b")));
        option.resetField(null);
        // The factory's contract: a fresh empty collection of the declared
        // field type, so executions never share mutable state (#697/#704)
        assertTrue("restore must mint a fresh collection from the packed factory",
                slot.value instanceof LinkedHashSet);
        assertEquals(0, ((LinkedHashSet<?>) slot.value).size());
    }

    @Test
    public void testEveryCollectionFactoryConstantRestoresItsType() throws Exception {
        for (CollectionFactory factory : CollectionFactory.values()) {
            Slot slot = new Slot();
            ProcessedOption option = listOption(slot);
            option.setInitialValueFactory(factory);
            assertNull("constant " + factory + " must not allocate OptionExtras", extrasOf(option));

            option.setInitialValue(new ArrayList<>());
            option.resetField(null);
            assertNotNull("restore must call the packed factory " + factory, slot.value);
            assertEquals("factory " + factory + " must mint its own collection type",
                    expectedType(factory), slot.value.getClass());
        }
    }

    @Test
    public void testFactoryRestoreIsAFreshInstancePerReset() throws Exception {
        Slot slot = new Slot();
        ProcessedOption option = listOption(slot);
        option.setInitialValueFactory(CollectionFactory.ARRAY_LIST);
        option.setInitialValue(new ArrayList<>());
        option.resetField(null);
        Object first = slot.value;
        option.resetField(null);
        assertTrue(first instanceof ArrayList);
        assertTrue(slot.value instanceof ArrayList);
        org.junit.Assert.assertNotSame("each reset must mint a fresh collection", first, slot.value);
    }

    @Test
    public void testCustomSupplierFallsBackToHolder() throws Exception {
        Slot slot = new Slot();
        ProcessedOption option = listOption(slot);
        Object custom = new java.util.TreeSet<String>();
        Supplier<Object> supplier = () -> custom;
        option.setInitialValueFactory(supplier);
        assertNotNull("custom supplier must allocate the OptionExtras holder", extrasOf(option));

        option.setInitialValue(new ArrayList<>());
        option.resetField(null);
        assertSame("restore must use the custom supplier's instance", custom, slot.value);
    }

    @Test
    public void testCustomSupplierThenConstantReplaces() throws Exception {
        Slot slot = new Slot();
        ProcessedOption option = listOption(slot);
        option.setInitialValueFactory(() -> new java.util.TreeSet<String>());
        option.setInitialValueFactory(CollectionFactory.HASH_SET);
        // The constant overwrites the packed code; the stale custom supplier
        // in the holder is never read again
        option.setInitialValue(new ArrayList<>());
        option.resetField(null);
        assertEquals(java.util.HashSet.class, slot.value.getClass());
    }

    private static Class<?> expectedType(CollectionFactory factory) {
        switch (factory) {
            case ARRAY_LIST:
                return ArrayList.class;
            case LINKED_LIST:
                return java.util.LinkedList.class;
            case HASH_SET:
                return java.util.HashSet.class;
            case LINKED_HASH_SET:
                return LinkedHashSet.class;
            case TREE_SET:
                return java.util.TreeSet.class;
            case HASH_MAP:
                return java.util.HashMap.class;
            case LINKED_HASH_MAP:
                return java.util.LinkedHashMap.class;
            case TREE_MAP:
                return java.util.TreeMap.class;
            default:
                throw new AssertionError(factory);
        }
    }
}
