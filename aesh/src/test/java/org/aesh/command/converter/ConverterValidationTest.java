package org.aesh.command.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.aesh.command.impl.converter.AeshConverterInvocation;
import org.aesh.command.impl.converter.BooleanConverter;
import org.aesh.command.impl.converter.ByteConverter;
import org.aesh.command.impl.converter.DoubleConverter;
import org.aesh.command.impl.converter.FloatConverter;
import org.aesh.command.impl.converter.IntegerConverter;
import org.aesh.command.impl.converter.LongConverter;
import org.aesh.command.impl.converter.ShortConverter;
import org.aesh.command.validator.OptionValidatorException;
import org.junit.Test;

/**
 * Tests that all built-in converters validate input and throw
 * {@link OptionValidatorException} instead of RuntimeException
 * for invalid values.
 */
public class ConverterValidationTest {

    // --- BooleanConverter ---

    @Test
    public void testBooleanTrue() throws OptionValidatorException {
        BooleanConverter conv = new BooleanConverter();
        assertTrue(conv.convert(inv("true")));
        assertTrue(conv.convert(inv("TRUE")));
        assertTrue(conv.convert(inv("True")));
        assertTrue(conv.convert(inv("yes")));
        assertTrue(conv.convert(inv("YES")));
        assertTrue(conv.convert(inv("1")));
    }

    @Test
    public void testBooleanFalse() throws OptionValidatorException {
        BooleanConverter conv = new BooleanConverter();
        assertFalse(conv.convert(inv("false")));
        assertFalse(conv.convert(inv("FALSE")));
        assertFalse(conv.convert(inv("False")));
        assertFalse(conv.convert(inv("no")));
        assertFalse(conv.convert(inv("NO")));
        assertFalse(conv.convert(inv("0")));
    }

    @Test
    public void testBooleanInvalidThrows() {
        BooleanConverter conv = new BooleanConverter();
        assertInvalid(conv, "foobar");
        assertInvalid(conv, "tru"); // typo
        assertInvalid(conv, "flase"); // typo
        assertInvalid(conv, "2");
        assertInvalid(conv, "");
        assertInvalid(conv, "yep");
        assertInvalid(conv, "nope");
    }

    @Test
    public void testBooleanWithWhitespace() throws OptionValidatorException {
        BooleanConverter conv = new BooleanConverter();
        assertTrue(conv.convert(inv(" true ")));
        assertFalse(conv.convert(inv(" false ")));
    }

    // --- IntegerConverter ---

    @Test
    public void testIntegerValid() throws OptionValidatorException {
        IntegerConverter conv = new IntegerConverter();
        assertEquals(Integer.valueOf(42), conv.convert(inv("42")));
        assertEquals(Integer.valueOf(-1), conv.convert(inv("-1")));
        assertEquals(Integer.valueOf(0), conv.convert(inv("0")));
    }

    @Test(expected = OptionValidatorException.class)
    public void testIntegerInvalid() throws OptionValidatorException {
        new IntegerConverter().convert(inv("abc"));
    }

    @Test(expected = OptionValidatorException.class)
    public void testIntegerEmpty() throws OptionValidatorException {
        new IntegerConverter().convert(inv(""));
    }

    @Test(expected = OptionValidatorException.class)
    public void testIntegerFloat() throws OptionValidatorException {
        new IntegerConverter().convert(inv("3.14"));
    }

    // --- LongConverter ---

    @Test
    public void testLongValid() throws OptionValidatorException {
        LongConverter conv = new LongConverter();
        assertEquals(Long.valueOf(9999999999L), conv.convert(inv("9999999999")));
    }

    @Test(expected = OptionValidatorException.class)
    public void testLongInvalid() throws OptionValidatorException {
        new LongConverter().convert(inv("not-a-number"));
    }

    // --- ShortConverter ---

    @Test
    public void testShortValid() throws OptionValidatorException {
        assertEquals(Short.valueOf((short) 100), new ShortConverter().convert(inv("100")));
    }

    @Test(expected = OptionValidatorException.class)
    public void testShortInvalid() throws OptionValidatorException {
        new ShortConverter().convert(inv("xyz"));
    }

    // --- FloatConverter ---

    @Test
    public void testFloatValid() throws OptionValidatorException {
        assertEquals(3.14f, new FloatConverter().convert(inv("3.14")), 0.001);
    }

    @Test(expected = OptionValidatorException.class)
    public void testFloatInvalid() throws OptionValidatorException {
        new FloatConverter().convert(inv("not-float"));
    }

    // --- DoubleConverter ---

    @Test
    public void testDoubleValid() throws OptionValidatorException {
        assertEquals(2.718, new DoubleConverter().convert(inv("2.718")), 0.001);
    }

    @Test(expected = OptionValidatorException.class)
    public void testDoubleInvalid() throws OptionValidatorException {
        new DoubleConverter().convert(inv("not-double"));
    }

    // --- ByteConverter ---

    @Test
    public void testByteValid() throws OptionValidatorException {
        assertEquals(Byte.valueOf((byte) 127), new ByteConverter().convert(inv("127")));
    }

    @Test(expected = OptionValidatorException.class)
    public void testByteInvalid() throws OptionValidatorException {
        new ByteConverter().convert(inv("not-byte"));
    }

    @Test(expected = OptionValidatorException.class)
    public void testByteOverflow() throws OptionValidatorException {
        new ByteConverter().convert(inv("999"));
    }

    // --- Helpers ---

    private static ConverterInvocation inv(String value) {
        return new AeshConverterInvocation(value, null);
    }

    private static void assertInvalid(BooleanConverter conv, String value) {
        try {
            conv.convert(inv(value));
            fail("Should throw OptionValidatorException for: '" + value + "'");
        } catch (OptionValidatorException e) {
            assertTrue("Error message should mention the invalid value",
                    e.getMessage().contains(value));
        }
    }
}
