/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.aesh.console.settings;

/**
 * <code>FileAccessPermission</code> defines file access permission like readable, writable.
 * 
 * <i>readable</i> and <i>writable</i> will be true by default.
 * 
 * @author Lin Gao <lgao@redhat.com>
 *
 */
public class FileAccessPermission {
    
    private boolean readable = true;
    
    private boolean readableOwnerOnly;
    
    private boolean writable = true;
    
    private boolean writableOwnerOnly;
    
    private boolean executable = false;
    
    private boolean executableOwnerOnly;
    
    /**
     * Default constructor
     */
    public FileAccessPermission() {
        super();
    }
    
    /**
     * @return the executable
     */
    public boolean isExecutable() {
        return executable;
    }

    /**
     * @param executable the executable to set
     */
    public void setExecutable(boolean executable) {
        this.executable = executable;
    }

    /**
     * @return the executableOwnerOnly
     */
    public boolean isExecutableOwnerOnly() {
        return executableOwnerOnly;
    }

    /**
     * @param executableOwnerOnly the executableOwnerOnly to set
     */
    public void setExecutableOwnerOnly(boolean executableOwnerOnly) {
        this.executableOwnerOnly = executableOwnerOnly;
    }

    /**
     * @return the readable
     */
    public boolean isReadable() {
        return readable;
    }

    /**
     * @param readable the readable to set
     */
    public void setReadable(boolean readable) {
        this.readable = readable;
    }

    /**
     * @return the readableOwnerOnly
     */
    public boolean isReadableOwnerOnly() {
        return readableOwnerOnly;
    }

    /**
     * @param readableOwnerOnly the readableOwnerOnly to set
     */
    public void setReadableOwnerOnly(boolean readableOwnerOnly) {
        this.readableOwnerOnly = readableOwnerOnly;
    }

    /**
     * @return the writable
     */
    public boolean isWritable() {
        return writable;
    }

    /**
     * @param writable the writable to set
     */
    public void setWritable(boolean writable) {
        this.writable = writable;
    }

    /**
     * @return the writableOwnerOnly
     */
    public boolean isWritableOwnerOnly() {
        return writableOwnerOnly;
    }

    /**
     * @param writableOwnerOnly the writableOwnerOnly to set
     */
    public void setWritableOwnerOnly(boolean writableOwnerOnly) {
        this.writableOwnerOnly = writableOwnerOnly;
    }
    
}
