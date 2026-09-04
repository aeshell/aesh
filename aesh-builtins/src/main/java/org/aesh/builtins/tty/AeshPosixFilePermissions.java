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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.builtins.tty;

import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static java.nio.file.attribute.PosixFilePermission.*;
/**
 * Copy of PosixFilePermission to also write out file type attribute
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshPosixFilePermissions {
    private AeshPosixFilePermissions() { }

    // Write string representation of permission bits to {@code sb}.
    private static void writeBits(StringBuilder sb, boolean r, boolean w, boolean x) {
        if (r) {
            sb.append('r');
        } else {
            sb.append('-');
        }
        if (w) {
            sb.append('w');
        } else {
            sb.append('-');
        }
        if (x) {
            sb.append('x');
        } else {
            sb.append('-');
        }
    }

    /**
     * @return  the string representation of the file
     */
    public static String toString(PosixFileAttributes attr) {
        Set<PosixFilePermission> perms = attr.permissions();
        StringBuilder sb = new StringBuilder(10);
        if(attr.isDirectory())
            sb.append('d');
        else if(attr.isSymbolicLink())
            sb.append('l');
        else
            sb.append('-');

        writeBits(sb, perms.contains(OWNER_READ), perms.contains(OWNER_WRITE),
          perms.contains(OWNER_EXECUTE));
        writeBits(sb, perms.contains(GROUP_READ), perms.contains(GROUP_WRITE),
          perms.contains(GROUP_EXECUTE));
        writeBits(sb, perms.contains(OTHERS_READ), perms.contains(OTHERS_WRITE),
          perms.contains(OTHERS_EXECUTE));
        return sb.toString();
    }
}
