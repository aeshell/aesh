/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.readline;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
class ActionEvent implements KeyEvent {

  private final String name;
  private final int[] seq;

  public ActionEvent(String name, int[] seq) {
    this.name = name;
    this.seq = seq;
  }

  /**
   * @return the name of the action to apply.
   */
  String name() {
    return name;
  }

  @Override
  public int getCodePointAt(int index) throws IndexOutOfBoundsException {
    if (index < 0 || index > seq.length) {
      throw new IndexOutOfBoundsException("Wrong index: " + index);
    }
    return seq[index];
  }

  @Override
  public int length() {
    return seq.length;
  }
}
