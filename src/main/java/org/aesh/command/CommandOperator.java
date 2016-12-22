/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aesh.command;

import org.aesh.console.command.invocation.CommandInvocation;

/**
 * TODO
 *
 * @author jdenise@redhat.com
 */
public interface CommandOperator<T extends CommandInvocation> extends Executable<T> {
}
