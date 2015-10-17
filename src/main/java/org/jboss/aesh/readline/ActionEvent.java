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
package org.jboss.aesh.readline;

import java.util.Arrays;

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
  @Override
  public String name() {
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

  @Override
  public boolean equals(Object o) {
    if(this == o) return true;
    if(!(o instanceof ActionEvent)) return false;

    ActionEvent that = (ActionEvent) o;

    if(!name.equals(that.name)) return false;
    return Arrays.equals(seq, that.seq);

  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + Arrays.hashCode(seq);
    return result;
  }
}
