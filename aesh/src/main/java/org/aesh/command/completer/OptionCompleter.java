/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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

package org.aesh.command.completer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Complete the given input for an option value.
 *
 * @author Aesh team
 */
public interface OptionCompleter<T extends CompleterInvocation> {

    /**
     * Complete the given input for an option value.
     * The current value to be completed is completerInvocation.getGivenCompleteValue()
     */
    void complete(T completerInvocation);

    /**
     * All values this completer can offer, for help text.
     * Used to resolve the {@code ${COMPLETION-CANDIDATES}} placeholder in
     * option descriptions, alongside the option's {@code allowedValues}.
     * <p>
     * The default implementation collects the values when this completer is
     * itself {@link Iterable} (e.g. ported picocli completion candidates),
     * and returns an empty list otherwise. Override it when the full set can
     * only be computed on demand.
     * <p>
     * Must be cheap and side-effect-free: it runs every time help is
     * rendered. A throwing implementation is ignored and only the
     * {@code allowedValues} are listed.
     *
     * @return the enumerable candidates, or an empty list if there are none
     */
    default List<String> candidates() {
        if (this instanceof Iterable) {
            List<String> all = new ArrayList<>();
            for (Object candidate : (Iterable<?>) this) {
                if (candidate != null)
                    all.add(candidate.toString());
            }
            return all;
        }
        return Collections.emptyList();
    }
}
