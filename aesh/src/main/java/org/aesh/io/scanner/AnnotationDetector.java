/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.io.scanner;


import java.io.DataInput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@code AnnotationDetector} reads Java Class File (".class") files and reports the
 * encountered annotations via a simple, developer friendly API.
 * <p>
 * A Java Class File consists of a stream of 8-bit bytes. All 16-bit, 32-bit, and 64-bit
 * quantities are constructed by reading in two, four, and eight consecutive 8-bit
 * bytes, respectively. Multi byte data items are always stored in big-endian order,
 * where the high bytes come first. In the Java and Java 2 platforms, this format is
 * supported by interfaces {@link java.io.DataInput} and {@link java.io.DataOutput}.
 * <p>
 * A class file consists of a single ClassFile structure:
 * <pre>
 * ClassFile {
 *   u4 magic;
 *   u2 minor_version;
 *   u2 major_version;
 *   u2 constant_pool_count;
 *   cp_info constant_pool[constant_pool_count-1];
 *   u2 access_flags;
 *   u2 this_class;
 *   u2 super_class;
 *   u2 interfaces_count;
 *   u2 interfaces[interfaces_count];
 *   u2 fields_count;
 *   field_info fields[fields_count];
 *   u2 methods_count;
 *   method_info methods[methods_count];
 *   u2 attributes_count;
 *   attribute_info attributes[attributes_count];
 * }
 *
 * Where:
 * u1 unsigned byte {@link java.io.DataInput#readUnsignedByte()}
 * u2 unsigned short {@link java.io.DataInput#readUnsignedShort()}
 * u4 unsigned int {@link java.io.DataInput#readInt()}
 *
 * Annotations are stored as Attributes (i.e. "RuntimeVisibleAnnotations" and
 * "RuntimeInvisibleAnnotations").
 * </pre>
 * References:
 * <ul>
 * <li><a href="http://en.wikipedia.org/wiki/Java_class_file">Java class file (Wikipedia)</a>
 * (Gentle Introduction);
 * <li><a href="http://download.oracle.com/otndocs/jcp/jcfsu-1.0-fr-eval-oth-JSpec/">Class
 * File Format Specification</a> (Java 6 version) and the
 * <a href="http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html">Java
 * VM Specification (Chapter 4)</a> for the real work.
 * <li><a href="http://stackoverflow.com/questions/259140">scanning java annotations at
 * runtime</a>.
 * </ul>
 * <p>
 * Similar projects / libraries:
 * <ul>
 * <li><a href="http://community.jboss.org/wiki/MCScanninglib">JBoss MC Scanning lib</a>;
 * <li><a href="http://code.google.com/p/reflections/">Google Reflections</a>, in fact an
 * improved version of <a href="http://scannotation.sourceforge.net/">scannotation</a>;
 * <li><a herf="https://github.com/ngocdaothanh/annovention">annovention</a>, improved version
 * of the <a href="http://code.google.com/p/annovention">original Annovention</a> project.
 * Available from maven: {@code tv.cntt:annovention:1.2};
 * <li>If using the Spring Framework, use {@code ClassPathScanningCandidateComponentProvider}
 * </ul>
 * <p>
 * All above mentioned projects make use of a byte code manipulation library (like BCEL,
 * ASM or Javassist).
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@SuppressWarnings("unchecked")
public final class AnnotationDetector {


    /**
     * A {@code Reporter} for type annotations.
     */
    public interface TypeReporter extends Reporter {

        /**
         * This call back method is used to report an type level {@code Annotation}.
         * Only {@code Annotation}s, specified by {@link #annotations()} are reported!
         */
        void reportTypeAnnotation(Class<? extends Annotation> annotation, String className);

    }

    /**
     * A {@code Reporter} for field annotations.
     */
    public interface FieldReporter extends Reporter {

        /**
         * This call back method is used to report an field level {@code Annotation}.
         * Only {@code Annotation}s, specified by {@link #annotations()} are reported!
         */
        void reportFieldAnnotation(Class<? extends Annotation> annotation, String className,
                                   String fieldName);

    }

    /**
     * A {@code Reporter} for method annotations.
     */
    public interface MethodReporter extends Reporter {

        /**
         * This call back method is used to report an method level {@code Annotation}.
         * Only {@code Annotation}s, specified by {@link #annotations()} are reported!
         */
        void reportMethodAnnotation(Class<? extends Annotation> annotation, String className,
                                    String methodName);

    }

    // Only used during development. If set to "true" debug messages are displayed.
    private static final boolean DEBUG = false;

    // Constant Pool type tags
    private static final int CP_UTF8 = 1;
    private static final int CP_INTEGER = 3;
    private static final int CP_FLOAT = 4;
    private static final int CP_LONG = 5;
    private static final int CP_DOUBLE = 6;
    private static final int CP_CLASS = 7;
    private static final int CP_STRING = 8;
    private static final int CP_REF_FIELD = 9;
    private static final int CP_REF_METHOD = 10;
    private static final int CP_REF_INTERFACE = 11;
    private static final int CP_NAME_AND_TYPE = 12;
    private static final int CP_METHOD_HANDLE = 15;
    private static final int CP_METHOD_TYPE = 16;
    private static final int CP_INVOKE_DYNAMIC = 18;

    // AnnotationElementValue
    private static final int BYTE = 'B';
    private static final int CHAR = 'C';
    private static final int DOUBLE = 'D';
    private static final int FLOAT = 'F';
    private static final int INT = 'I';
    private static final int LONG = 'J';
    private static final int SHORT = 'S';
    private static final int BOOLEAN = 'Z';
    // used for AnnotationElement only
    private static final int STRING = 's';
    private static final int ENUM = 'e';
    private static final int CLASS = 'c';
    private static final int ANNOTATION = '@';
    private static final int ARRAY = '[';

    // The buffer is reused during the life cycle of this AnnotationDetector instance
    private final ClassFileBuffer cpBuffer = new ClassFileBuffer();
    // the annotation types to report, see {@link #annotations()}
    private final Map<String, Class<? extends Annotation>> annotations;

    private TypeReporter typeReporter;
    private FieldReporter fieldReporter;
    private MethodReporter methodReporter;

    // the 'raw' name of this interface or class (using '/' instead of '.' in package name)
    private String typeName;
    // Reusing the constantPool is not needed for better performance
    private Object[] constantPool;
    private String memberName;

    /**
     * Create a new {@code AnnotationDetector}, reporting the detected annotations
     * to the specified {@code Reporter}.
     */
    public AnnotationDetector(final Reporter reporter) {
        final Class<? extends Annotation>[] a = reporter.annotations();
        annotations = new HashMap<>(a.length);
        // map "raw" type names to Class object
        for (int i = 0; i < a.length; ++i) {
            annotations.put("L" + a[i].getName().replace('.', '/') + ";", a[i]);
        }
        if (reporter instanceof TypeReporter) {
            typeReporter = (TypeReporter)reporter;
        }
        if (reporter instanceof FieldReporter) {
            fieldReporter = (FieldReporter)reporter;
        }
        if (reporter instanceof MethodReporter) {
            methodReporter = (MethodReporter)reporter;
        }
        if (typeReporter == null && fieldReporter == null && methodReporter == null) {
            throw new AssertionError("No reporter defined");
        }
    }

    /**
     * Report all Java ClassFile files available on the class path.
     *
     * @see #detect(File...)
     */
    public void detect() throws IOException {
        detect(new ClassFileIterator());
    }

    /**
     * Report all Java ClassFile files available on the class path within
     * the specified packages and sub packages.
     *
     * @see #detect(File...)
     */
    public void detect(final String... packageNames) throws IOException {
        final String[] pkgNameFilter = new String[packageNames.length];
        for (int i = 0; i < pkgNameFilter.length; ++i) {
            pkgNameFilter[i] = packageNames[i].replace('.', '/');
            if (!pkgNameFilter[i].endsWith("/")) {
                pkgNameFilter[i] = pkgNameFilter[i].concat("/");
            }
        }
        final Set<File> files = new HashSet<>();
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (final String packageName : pkgNameFilter) {
            final Enumeration<URL> resourceEnum = loader.getResources(packageName);
            while (resourceEnum.hasMoreElements()) {
                final URL url = resourceEnum.nextElement();
                if ("file".equals(url.getProtocol())) {
                    final File dir = toFile(url);
                    if (dir.isDirectory()) {
                        files.add(dir);
                    } else {
                        throw new AssertionError("Not a recognized file URL: " + url);
                    }
                } else {
                    final File jarFile = toFile(openJarURLConnection(url).getJarFileURL());
                    if (jarFile.isFile()) {
                        files.add(jarFile);
                    } else {
                        throw new AssertionError("Not a File: " + jarFile);
                    }
                }
            }
        }
        if (DEBUG) {
            print("Files to scan: %s", files);
        }
        if (!files.isEmpty()) {
            // see http://shipilev.net/blog/2016/arrays-wisdom-ancients/#_conclusion
            detect(new ClassFileIterator(files.toArray(new File[0]), pkgNameFilter));
        }
    }

    /**
     * Scan all Java ClassFile ({@code *.class}) files available in the specified files
     * and/or directories.
     * <p>
     * In Java, the
     * <a href="http://docs.oracle.com/javase/8/docs/technotes/tools/unix/classpath.html">
     * Class path</a> contains directories (top level directory as package root) and/or
     * jar files (including zip files).
     * <p>
     * Note that non-class files (files, not starting with the magic number
     * {@code CAFEBABE} are silently ignored.
     *
     * @param filesOrDirectories Valid files are: jar files, Java *.class files (all other
     * files are silently ignored) and directories which are package root directories
     */
    public void detect(final File... filesOrDirectories) throws IOException {
        if (DEBUG) {
            print("detectFilesOrDirectories: %s", (Object)filesOrDirectories);
        }
        detect(new ClassFileIterator(filesOrDirectories, null));
    }

    // private

    private File toFile(final URL url) {
        // only correct way to convert the URL to a File object, also see issue #16
        // Do not use URLDecoder
        try {
            return new File(url.toURI());
        } catch (URISyntaxException ex) {
            // we do not expect an URISyntaxException here
            throw new AssertionError("Unable to convert URI to File: " + url);
        }
    }

    private JarURLConnection openJarURLConnection(final URL url) throws IOException {
        final URL checkedUrl;
        if ("zip".equals(url.getProtocol())) {
            // WebLogic returns URL with "zip" protocol, returning a
            // weblogic.utils.zip.ZipURLConnection when opened
            // Easy fix is to convert this URL to jar URL
            checkedUrl = new URL(url.toExternalForm().replace("zip:/", "jar:file:/"));
        } else {
            checkedUrl = url;
        }
        URLConnection urlConnection = checkedUrl.openConnection();
        // GlassFish 4.1.1 is providing a URLConnection of type:
        // http://svn.apache.org/viewvc/felix/trunk/framework/src/main/java/org/
        // apache/felix/framework/URLHandlersBundleURLConnection.java?view=markup
        // Which does _not_ extend JarURLConnection.
        // This bit of reflection allows us to call the getLocalURL method which
        // actually returns a URL to a jar file.
        if (checkedUrl.getProtocol().startsWith("bundle")) {
            try {
                final Method m = urlConnection.getClass().getDeclaredMethod("getLocalURL");
                if (!m.isAccessible()) {
                    m.setAccessible(true);
                }
                final URL jarUrl = (URL)m.invoke(urlConnection);
                urlConnection = jarUrl.openConnection();
            } catch (Exception ex) {
                throw new AssertionError("Couldn't read jar file URL from bundle: " + ex);
            }
        }
        if (urlConnection instanceof JarURLConnection) {
            return (JarURLConnection)urlConnection;
        } else {
            throw new AssertionError(
                    "Unknown URLConnection type: " + urlConnection.getClass().getName());
        }
    }

    @SuppressWarnings("illegalcatch")
    private void detect(final ResourceIterator iterator) throws IOException {
        InputStream stream;
        while ((stream = iterator.next()) != null) {
            try {
                cpBuffer.readFrom(stream);
                if (hasCafebabe(cpBuffer)) {
                    detect(cpBuffer);
                } // else ignore
            } catch (Throwable t) {
                // catch all errors
                if (!(stream instanceof FileInputStream)) {
                    // in case of an error we close the ZIP File here
                    stream.close();
                }
            } finally {
                // closing InputStream from ZIP Entry is handled by ZipFileIterator
                if (stream instanceof FileInputStream) {
                    stream.close();
                }
            }
        }
    }

    private boolean hasCafebabe(final ClassFileBuffer buffer) throws IOException {
        return buffer.size() > 4 &&  buffer.readInt() == 0xCAFEBABE;
    }

    /**
     * Inspect the given (Java) class file in streaming mode.
     */
    private void detect(final DataInput di) throws IOException {
        readVersion(di);
        readConstantPoolEntries(di);
        readAccessFlags(di);
        readThisClass(di);
        readSuperClass(di);
        readInterfaces(di);
        readFields(di);
        readMethods(di);
        readAttributes(di, 'T', typeReporter == null);
    }

    private void readVersion(final DataInput di) throws IOException {
        // sequence: minor version, major version (argument_index is 1-based)
        if (DEBUG) {
            print("Java Class version %2$d.%1$d",
                    di.readUnsignedShort(), di.readUnsignedShort());
        } else {
            di.skipBytes(4);
        }
    }

    private void readConstantPoolEntries(final DataInput di) throws IOException {
        final int count = di.readUnsignedShort();
        constantPool = new Object[count];
        for (int i = 1; i < count; ++i) {
            if (readConstantPoolEntry(di, i)) {
                // double slot
                ++i;
            }
        }
    }

    /**
     * Return {@code true} if a double slot is read (in case of Double or Long constant).
     */
    private boolean readConstantPoolEntry(final DataInput di, final int index)
            throws IOException {

        final int tag = di.readUnsignedByte();
        switch (tag) {
            case CP_METHOD_TYPE:
                di.skipBytes(2);  // readUnsignedShort()
                return false;
            case CP_METHOD_HANDLE:
                di.skipBytes(3);
                return false;
            case CP_INTEGER:
            case CP_FLOAT:
            case CP_REF_FIELD:
            case CP_REF_METHOD:
            case CP_REF_INTERFACE:
            case CP_NAME_AND_TYPE:
            case CP_INVOKE_DYNAMIC:
                di.skipBytes(4); // readInt() / readFloat() / readUnsignedShort() * 2
                return false;
            case CP_LONG:
            case CP_DOUBLE:
                di.skipBytes(8); // readLong() / readDouble()
                return true;
            case CP_UTF8:
                constantPool[index] = di.readUTF();
                return false;
            case CP_CLASS:
            case CP_STRING:
                // reference to CP_UTF8 entry. The referenced index can have a higher number!
                constantPool[index] = di.readUnsignedShort();
                return false;
            default:
                throw new ClassFormatError(
                        "Unkown tag value for constant pool entry: " + tag);
        }
    }

    private void readAccessFlags(final DataInput di) throws IOException {
        di.skipBytes(2); // u2
    }

    private void readThisClass(final DataInput di) throws IOException {
        typeName = resolveUtf8(di);
        if (DEBUG) {
            print("read type '%s'", typeName);
        }
    }

    private void readSuperClass(final DataInput di) throws IOException {
        di.skipBytes(2); // u2
    }

    private void readInterfaces(final DataInput di) throws IOException {
        final int count = di.readUnsignedShort();
        di.skipBytes(count * 2); // count * u2
    }

    private void readFields(final DataInput di) throws IOException {
        final int count = di.readUnsignedShort();
        if (DEBUG) {
            print("field count = %d", count);
        }
        for (int i = 0; i < count; ++i) {
            readAccessFlags(di);
            memberName = resolveUtf8(di);
            final String descriptor = resolveUtf8(di);
            readAttributes(di, 'F', fieldReporter == null);
            if (DEBUG) {
                print("Field: %s, descriptor: %s", memberName, descriptor);
            }
        }
    }

    private void readMethods(final DataInput di) throws IOException {
        final int count = di.readUnsignedShort();
        if (DEBUG) {
            print("method count = %d", count);
        }
        for (int i = 0; i < count; ++i) {
            readAccessFlags(di);
            memberName = resolveUtf8(di);
            final String descriptor = resolveUtf8(di);
            readAttributes(di, 'M', methodReporter == null);
            if (DEBUG) {
                print("Method: %s, descriptor: %s", memberName, descriptor);
            }
        }
    }

    private void readAttributes(final DataInput di, final char reporterType,
                                final boolean skipReporting) throws IOException {

        final int count = di.readUnsignedShort();
        if (DEBUG) {
            print("attribute count (%s) = %d", reporterType, count);
        }
        for (int i = 0; i < count; ++i) {
            final String name = resolveUtf8(di);
            // in bytes, use this to skip the attribute info block
            final int length = di.readInt();
            if (!skipReporting &&
                    ("RuntimeVisibleAnnotations".equals(name) ||
                            "RuntimeInvisibleAnnotations".equals(name))) {
                readAnnotations(di, reporterType);
            } else {
                if (DEBUG) {
                    print("skip attribute %s", name);
                }
                di.skipBytes(length);
            }
        }
    }

    private void readAnnotations(final DataInput di, final char reporterType)
            throws IOException {

        // the number of Runtime(In)VisibleAnnotations
        final int count = di.readUnsignedShort();
        if (DEBUG) {
            print("annotation count (%s) = %d", reporterType, count);
        }
        for (int i = 0; i < count; ++i) {
            final String rawTypeName = readAnnotation(di);
            final Class<? extends Annotation> type = annotations.get(rawTypeName);
            if (type == null) {
                continue;
            }
            final String externalTypeName = typeName.replace('/', '.');
            switch (reporterType) {
                case 'T':
                    typeReporter.reportTypeAnnotation(type, externalTypeName);
                    break;
                case 'F':
                    fieldReporter.reportFieldAnnotation(type, externalTypeName, memberName);
                    break;
                case 'M':
                    methodReporter.reportMethodAnnotation(type, externalTypeName, memberName);
                    break;
                default:
                    throw new AssertionError("reporterType=" + reporterType);
            }
        }
    }

    private String readAnnotation(final DataInput di) throws IOException {
        final String rawTypeName = resolveUtf8(di);
        // num_element_value_pairs
        final int count = di.readUnsignedShort();
        if (DEBUG) {
            print("annotation elements count: %d", count);
        }
        for (int i = 0; i < count; ++i) {
            if (DEBUG) {
                print("element '%s'", resolveUtf8(di));
            } else {
                di.skipBytes(2);
            }
            readAnnotationElementValue(di);
        }
        return rawTypeName;
    }


    private void readAnnotationElementValue(final DataInput di) throws IOException {
        final int tag = di.readUnsignedByte();
        if (DEBUG) {
            print("tag='%c'", (char)tag);
        }
        switch (tag) {
            case BYTE:
            case CHAR:
            case DOUBLE:
            case FLOAT:
            case INT:
            case LONG:
            case SHORT:
            case BOOLEAN:
            case STRING:
                di.skipBytes(2);
                break;
            case ENUM:
                di.skipBytes(4); // 2 * u2
                break;
            case CLASS:
                di.skipBytes(2);
                break;
            case ANNOTATION:
                readAnnotation(di);
                break;
            case ARRAY:
                final int count = di.readUnsignedShort();
                for (int i = 0; i < count; ++i) {
                    readAnnotationElementValue(di);
                }
                break;
            default:
                throw new ClassFormatError("Not a valid annotation element type tag: 0x" +
                        Integer.toHexString(tag));
        }
    }

    /**
     * Look up the String value, identified by the u2 index value from constant pool
     * (direct or indirect).
     */
    private String resolveUtf8(final DataInput di) throws IOException {
        final int index = di.readUnsignedShort();
        final Object value = constantPool[index];
        final String s;
        if (value instanceof Integer) {
            s = (String)constantPool[(Integer)value];
            if (DEBUG) {
                print("resolveUtf8(%d): %d --> %s", index, value, s);
            }
        } else {
            s = (String)value;
            if (DEBUG) {
                print("resolveUtf8(%d): %s", index, s);
            }
        }

        return s;
    }

    /**
     * Helper method for simple (debug) logging.
     */
    @SuppressWarnings("regexpsinglelinejava")
    private static void print(final String message, final Object... args) {
        if (DEBUG) {
            final String logMessage;
            if (args.length == 0) {
                logMessage = message;
            } else {
                for (int i = 0; i < args.length; ++i) {
                    // arguments may be null
                    if (args[i] == null) {
                        continue;
                    }
                    if (args[i].getClass().isArray()) {
                        // cast back to array! Note that primitive arrays are not supported
                        args[i] = Arrays.toString((Object[])args[i]);
                    } else if (args[i] == Class.class) {
                        args[i] = ((Class<?>)args[i]).getName();
                    }
                }
                logMessage = String.format(message, args);
            }
            System.out.println(logMessage);
        }
    }

}
