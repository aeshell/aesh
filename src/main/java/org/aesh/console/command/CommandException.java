/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aesh.console.command;

/**
 *
 * An exception thrown by Commands when exception occurs
 *
 * @author jdenise@redhat.com
 */
public final class CommandException extends Exception {

    private static final long serialVersionUID = -1098155769714455108L;

    public CommandException() {
        super();
    }

    public CommandException(String message) {
        super(message);
    }

    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandException(Throwable cause) {
        super(cause);
    }

}
