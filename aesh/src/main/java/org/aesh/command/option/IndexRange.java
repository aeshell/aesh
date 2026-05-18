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
package org.aesh.command.option;

/**
 * Positional index range for {@code @Argument} and {@code @Arguments}.
 *
 * Supports:
 * <ul>
 * <li>single index: {@code 0}</li>
 * <li>bounded range: {@code 1..3}</li>
 * <li>open range: {@code 2..*}</li>
 * </ul>
 */
public final class IndexRange {

    private final int min;
    private final int max;

    private IndexRange(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public boolean contains(int index) {
        return index >= min && index <= max;
    }

    public boolean overlaps(IndexRange other) {
        return this.min <= other.max && other.min <= this.max;
    }

    public static IndexRange parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Index range cannot be empty");
        }
        String trimmed = value.trim();
        int sep = trimmed.indexOf("..");
        if (sep < 0) {
            int single = parseNonNegative(trimmed, "index");
            return new IndexRange(single, single);
        }
        String left = trimmed.substring(0, sep).trim();
        String right = trimmed.substring(sep + 2).trim();
        if (left.isEmpty() || right.isEmpty()) {
            throw new IllegalArgumentException("Invalid index range: '" + value + "'");
        }
        int min = parseNonNegative(left, "index range minimum");
        int max = "*".equals(right) ? Integer.MAX_VALUE : parseNonNegative(right, "index range maximum");
        if (max < min) {
            throw new IllegalArgumentException(
                    "Invalid index range: minimum cannot be greater than maximum in '" + value + "'");
        }
        return new IndexRange(min, max);
    }

    private static int parseNonNegative(String value, String what) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw new IllegalArgumentException(what + " must be non-negative: '" + value + "'");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + what + ": '" + value + "'", e);
        }
    }
}
