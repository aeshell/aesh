package org.aesh.io.scanner;

import org.aesh.command.CommandDefinition;
import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.Annotation;

public class AnnotationDetectorTest {

    @Test
    public void testAnnotationDetector() {
        AnnotationDetector detector = new AnnotationDetector(new AnnotationReporter());
        try {
            detector.detect();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static class AnnotationReporter implements AnnotationDetector.TypeReporter {

        private boolean foundManCommand = false;

        public boolean foundManCommand() {
            return foundManCommand;
        }

        @Override
        public void reportTypeAnnotation(Class<? extends Annotation> annotation, String className) {
            if(className.equals("org.aesh.command.man.Man"))
                foundManCommand = true;
        }

        @Override
        public Class<? extends Annotation>[] annotations() {
            CommandDefinition commandDefinition;
            try {
                return new Class[]{ Class.forName("org.aesh.command.CommandDefinition")};
            }
            catch (ClassNotFoundException e) {
                return null;
            }
        }
    }
}
