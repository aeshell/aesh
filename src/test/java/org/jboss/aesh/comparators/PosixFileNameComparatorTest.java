package org.jboss.aesh.comparators;

import org.junit.Test;

import java.util.Comparator;

import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:robert@balent.cz">Robert Balent</a>
 */
public class PosixFileNameComparatorTest {

    private Comparator<String> posixComparator = new PosixFileNameComparator();

    @Test
    public void testSame() {
        String s1 = "abcde";
        String s2 = "abcde";

        assertTrue(s1 + " should equals " + s2, posixComparator.compare(s1, s2) == 0);
    }

    @Test
    public void testDifferentNames() {
        String s1 = "Abcde";
        String s2 = "Bbcde";

        assertTrue(s1 + " should be before " + s2, posixComparator.compare(s1, s2) < 0);

        String s3 = "abcde";
        String s4 = "Bbcde";

        assertTrue(s3 + " should be before " + s4, posixComparator.compare(s3, s4) < 0);

        String s5 = "bbcde";
        String s6 = "Abcde";

        assertTrue(s5 + " should be after " + s6, posixComparator.compare(s5, s6) > 0);
    }

    @Test
    public void testIgnoreCasesDifferentLength() {
        String s1 = "abcde";
        String s2 = "Abc";

        assertTrue(s1 + " should be after " + s2, posixComparator.compare(s1, s2) > 0);

        String s3 = "Abcde";
        String s4 = "abc";

        assertTrue(s3 + " should be after " + s4, posixComparator.compare(s3, s4) > 0);
    }

    @Test
    public void testIgnoreDotsDifferent() {
        String s1 = ".Abcde";
        String s2 = "Bbcde";

        assertTrue(s1 + " should be before " + s2, posixComparator.compare(s1, s2) < 0);

        String s3 = "Abcde";
        String s4 = ".Bbcde";

        assertTrue(s3 + " should be before " + s4, posixComparator.compare(s3, s4) < 0);
    }

    /**
     * When names are same after changed to lower case, the name containing lower case should be first.
     */
    @Test
    public void testLowerCaseBeforeUpperCases() {
        String s1 = "abcde";
        String s2 = "Abcde";

        assertTrue(s1 + " should be before " + s2, posixComparator.compare(s1, s2) < 0);

        String s3 = "AbCde";
        String s4 = "Abcde";

        assertTrue(s3 + " should be after " + s4, posixComparator.compare(s3, s4) > 0);
    }

    /**
     * When name with and without dot are same, the name without dot should be first.
     */
    @Test
    public void testIgnoreDotsSameName() {
        String s1 = ".abcde";
        String s2 = "abcde";

        assertTrue(s1 + " should be after " + s2, posixComparator.compare(s1, s2) > 0);

        String s3 = "abcde";
        String s4 = ".abcde";

        assertTrue(s3 + " should be before " + s4, posixComparator.compare(s3, s4) < 0);
    }
}
