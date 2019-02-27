package org.aesh.io.scanner;

import org.aesh.command.CommandDefinition;
import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.Annotation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AnnotationDetectorTest {

    @Test
    public void testAnnotationDetector() {
        AnnotationReporter reporter = new AnnotationReporter();
        AnnotationDetector detector = new AnnotationDetector(reporter);
        try {
            detector.detect("org.aesh.command.foo");
            assertFalse(reporter.foundManCommand);

            detector.detect("org.aesh.command.man");
            assertTrue(reporter.foundManCommand);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static class AnnotationReporter implements AnnotationDetector.TypeReporter {

        private boolean foundManCommand = false;

        @Override
        public void reportTypeAnnotation(Class<? extends Annotation> annotation, String className) {
            if(className.equals("org.aesh.command.man.Man"))
                foundManCommand = true;
        }

        @Override
        public Class[] annotations() {
            try {
                return new Class[]{ Class.forName(CommandDefinition.class.getCanonicalName())};
            }
            catch (ClassNotFoundException e) {
                return null;
            }
        }
    }
}
