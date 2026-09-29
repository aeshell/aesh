package org.aesh.io.scanner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.aesh.command.CommandDefinition;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AnnotationDetectorTest {

    @Test
    public void testAnnotationDetector() throws IOException {
        AnnotationReporter reporter = new AnnotationReporter();
        AnnotationDetector detector = new AnnotationDetector(reporter);
        detector.detect("org.aesh.command.foo");
        assertFalse(reporter.foundManCommand);

        detector.detect("org.aesh.command.man");
        assertTrue(reporter.foundManCommand);

    }

    public static class AnnotationReporter implements AnnotationDetector.TypeReporter {

        private boolean foundManCommand = false;

        @Override
        public void reportTypeAnnotation(Class<? extends Annotation> annotation, String className) {
            if (className.equals("org.aesh.command.man.Man"))
                foundManCommand = true;
        }

        @Override
        public Class[] annotations() {
            try {
                return new Class[] { Class.forName(CommandDefinition.class.getCanonicalName()) };
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
    }

    @Rule
    public final TemporaryFolder tempDir = new TemporaryFolder();

    /**
     * Minimal valid module-info.class for {@code module review.probe {}},
     * assembled by hand: compiling a real descriptor would require a
     * post-8 release the module still builds at (#654). Layout follows
     * JVMS (this_class {@code CONSTANT_Class}, module name via
     * {@code CONSTANT_Module}) and is verified with {@code javap -v}.
     */
    private static byte[] moduleInfoBytes() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(0xCAFEBABE);
        out.writeShort(0); // minor version
        out.writeShort(53); // major version, Java 9
        out.writeShort(6); // constant pool count
        out.writeByte(7); // CONSTANT_Class -> #2
        out.writeShort(2);
        out.writeByte(1); // CONSTANT_Utf8 "module-info"
        out.writeUTF("module-info");
        out.writeByte(19); // CONSTANT_Module -> #4
        out.writeShort(4);
        out.writeByte(1); // CONSTANT_Utf8 "review/probe"
        out.writeUTF("review/probe");
        out.writeByte(1); // CONSTANT_Utf8 "Module"
        out.writeUTF("Module");
        out.writeShort(0x8000); // access flags: ACC_MODULE
        out.writeShort(1); // this class
        out.writeShort(0); // super class (none)
        out.writeShort(0); // interfaces
        out.writeShort(0); // fields
        out.writeShort(0); // methods
        out.writeShort(1); // attributes
        out.writeShort(5); // "Module"
        out.writeInt(16); // attribute length
        out.writeShort(3); // module name
        out.writeShort(0); // module flags
        out.writeShort(0); // module version
        out.writeShort(0); // requires
        out.writeShort(0); // exports
        out.writeShort(0); // opens
        out.writeShort(0); // uses
        out.writeShort(0); // provides
        out.flush();
        return bytes.toByteArray();
    }

    /**
     * Class bytes with an unused constant-pool tag in place of real content.
     */
    private static byte[] unknownTagBytes() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(0xCAFEBABE);
        out.writeShort(0);
        out.writeShort(52);
        out.writeShort(2); // constant pool count
        out.writeByte(13); // unused tag: no valid class uses it
        out.flush();
        return bytes.toByteArray();
    }

    private static byte[] resourceBytes(String path) throws IOException {
        try (InputStream in = AnnotationDetectorTest.class.getResourceAsStream(path)) {
            if (in == null)
                throw new IOException("Test resource missing: " + path);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1)
                bytes.write(buffer, 0, n);
            return bytes.toByteArray();
        }
    }

    @Test
    public void testModuleInfoDoesNotAbortDirectoryScan() throws Exception {
        File dir = tempDir.newFolder("modscan");
        Files.write(new File(dir, "module-info.class").toPath(), moduleInfoBytes());
        Files.write(new File(dir, "Man.class").toPath(),
                resourceBytes("/org/aesh/command/man/Man.class"));

        AnnotationReporter reporter = new AnnotationReporter();
        new AnnotationDetector(reporter).detect(dir);

        assertTrue("Ordinary annotated commands must still be discovered past module-info.class",
                reporter.foundManCommand);
    }

    @Test
    public void testMalformedEntriesDoNotAbortScan() throws Exception {
        File dir = tempDir.newFolder("badscan");
        // Truncated magic header (EOFException path).
        Files.write(new File(dir, "Truncated.class").toPath(),
                new byte[] { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0, 1 });
        // Unknown constant-pool tag (ClassFormatError path).
        Files.write(new File(dir, "UnknownTag.class").toPath(), unknownTagBytes());
        Files.write(new File(dir, "Man.class").toPath(),
                resourceBytes("/org/aesh/command/man/Man.class"));

        AnnotationReporter reporter = new AnnotationReporter();
        new AnnotationDetector(reporter).detect(dir);

        assertTrue("Malformed entries must be skipped, not fatal",
                reporter.foundManCommand);
    }

    @Test
    public void testMixedJarScanFindsAnnotatedCommands() throws Exception {
        File jar = new File(tempDir.getRoot(), "mixed.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar.toPath()))) {
            out.putNextEntry(new JarEntry("module-info.class"));
            out.write(moduleInfoBytes());
            out.closeEntry();
            out.putNextEntry(new JarEntry("UnknownTag.class"));
            out.write(unknownTagBytes());
            out.closeEntry();
            out.putNextEntry(new JarEntry("org/aesh/command/man/Man.class"));
            out.write(resourceBytes("/org/aesh/command/man/Man.class"));
            out.closeEntry();
        }

        AnnotationReporter reporter = new AnnotationReporter();
        new AnnotationDetector(reporter).detect(jar);

        assertTrue("Mixed-version JAR discovery must continue past descriptors",
                reporter.foundManCommand);
    }
}
