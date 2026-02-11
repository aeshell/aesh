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
package org.aesh.util.table;

import static org.aesh.util.table.TableCharacters.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Predefined table border styles.
 */
public enum TableStyle {

    /**
     * Simple ASCII style without outside borders: {@code |}, {@code -}, {@code +}
     */
    POSTGRES {
        @Override
        public Map<String, String> characters() {
            Map<String, String> map = new HashMap<>();
            map.put(VERTICAL, "|");
            map.put(HORIZONTAL, "-");
            map.put(INTERSECT, "+");
            return Collections.unmodifiableMap(map);
        }
    },

    /**
     * ASCII style with full borders: {@code |}, {@code -}, {@code +}
     */
    SQLITE {
        @Override
        public Map<String, String> characters() {
            Map<String, String> base = new HashMap<>();
            base.put(VERTICAL, "|");
            base.put(HORIZONTAL, "-");
            base.put(INTERSECT, "+");
            return Collections.unmodifiableMap(convertToFullNames(base, true));
        }
    },

    /**
     * Unicode box-drawing style with outside borders.
     */
    DUCKDB {
        @Override
        public Map<String, String> characters() {
            Map<String, String> base = new HashMap<>();
            // top of outside border
            base.put(HEADER_TOP_LEFT, "\u250c");
            base.put(HEADER_TOP_RIGHT, "\u2510");
            base.put(HEADER_TOP_INTERSECT, "\u252c");
            // bottom of outside border
            base.put(TABLE_BOTTOM_LEFT, "\u2514");
            base.put(TABLE_BOTTOM_RIGHT, "\u2518");
            base.put(TABLE_BOTTOM_INTERSECT, "\u2534");
            // left and right of outside border
            base.put(TABLE_TOP_LEFT, "\u251c");
            base.put(TABLE_TOP_RIGHT, "\u2524");
            // inside crosses
            base.put(VERTICAL, "\u2502");
            base.put(HORIZONTAL, "\u2500");
            base.put(INTERSECT, "\u253c");
            return Collections.unmodifiableMap(convertToFullNames(base, true));
        }
    },

    /**
     * Double-line box-drawing style with outside borders and row separators.
     */
    DOUBLE {
        @Override
        public Map<String, String> characters() {
            String template = "\u2554\u2550\u2566\u2550\u2557" + System.lineSeparator() +
                    "\u2551h\u2551h\u2551" + System.lineSeparator() +
                    "\u2560\u2550\u256c\u2550\u2563" + System.lineSeparator() +
                    "\u2551v\u2551v\u2551" + System.lineSeparator() +
                    "\u255f\u2500\u256b\u2500\u2563" + System.lineSeparator() +
                    "\u255a\u2550\u2569\u2550\u255d";
            return Collections.unmodifiableMap(templateToMap(template, DUCKDB.characters()));
        }
    };

    /**
     * Returns the character map for this style.
     */
    public abstract Map<String, String> characters();
}
