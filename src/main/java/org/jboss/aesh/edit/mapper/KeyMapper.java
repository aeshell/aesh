/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit.mapper;

import org.jboss.aesh.edit.KeyOperation;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;

import java.util.regex.Pattern;

/**
 * Map key bindings to specified operation. Used when reading inputrc files.
 * Created to map bindings like:
 * "\M-[D":        backward-char
 * Meta-Control-h: backward-kill-word
 * C-q: quoted-insert
 * ... etc
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class KeyMapper {

    private static Pattern quotePattern = Pattern.compile("^\"");
    private static Pattern metaPattern = Pattern.compile("^(\\\\M|M|Meta)-"); // "M-
    private static Pattern controlPattern = Pattern.compile("^(\\\\C|C|Control)-"); // "M-

    /**
     * Parse key mapping lines that start with "
     *
     * @param keys that need mapping
     * @param operation is is specified for
     * @return proper KeyOperation
     */
    public static KeyOperation mapQuoteKeys(String keys, Operation operation) {
        return new KeyOperation(Key.getKey(mapKeys(quotePattern.split(keys)[1])), operation);
    }

    /**
     * Parse key mapping lines that start with "
     *
     * @param keys that need mapping
     * @param operation it is specified for
     * @return proper KeyOperation
     */
    public static KeyOperation mapQuoteKeys(String keys, String operation) {
        //return new KeyOperation(Key.getKey(mapKeys(quotePattern.split(keys)[1])),
        return new KeyOperation(Key.getKey(mapKeys(keys.substring(1))),
                OperationMapper.mapToFunction(operation));
    }

    /**
     * Parse lines that contain mapping like: C-q, Control-q, M-a, Meta-a, Meta-Control-b....
     *
     * @param keys that need mapping
     * @param operation it is specified for
     * @return proper KeyOperation
     */
    public static KeyOperation mapKeys(String keys, Operation operation) {
        return new KeyOperation(Key.getKey(mapKeys(keys)), operation);
    }

    /**
     * Parse lines that contain mapping like: C-q, Control-q, M-a, Meta-a, Meta-Control-b....
     *
     * @param keys that need mapping
     * @param operation it is specified for
     * @return proper KeyOperation
     */
    public static KeyOperation mapKeys(String keys, String operation) {
        return new KeyOperation(Key.getKey(mapKeys(keys)),
                OperationMapper.mapToFunction(operation));
    }

    private static int[] mapKeys(String keys) {
        boolean meta = false;
        boolean control = false;
        String randomKeys = null;
        String rest = keys;

        //find control/meta
        while(rest != null) {
            if(metaPattern.matcher(rest).find()) {
                meta = true;
                String[] split = metaPattern.split(rest);
                if(split.length > 1)
                    rest = split[1];
                else
                    rest = null;
                continue;
            }

            if(controlPattern.matcher(rest).find()) {
                control = true;
                String[] split = controlPattern.split(rest);
                if(split.length > 1)
                    rest = split[1];
                else
                    rest = null;
                continue;
            }

            randomKeys = rest;
            rest = null;
        }

        return mapRandomKeys(randomKeys, control, meta);
    }

    /**
     * Map all random keys after meta/control to its proper int value.
     * - yes its a bad method name....
     *
     * @param randomKeys keys after meta/control
     * @param control true or false
     * @param meta true or false
     * @return int mapping based on randomKeys + control/meta
     */
    private static int[] mapRandomKeys(String randomKeys, boolean control, boolean meta) {
        if(randomKeys == null)
            throw new RuntimeException("ERROR Aesh didn't find any keys after meta/control.Check your inputrc.");

        //parse the rest after control/meta
        int[] out;
        int pos = 0;
        if(meta) {
            out = new int[randomKeys.length()+1];
            out[0] = 27;
            pos = 1;
        }
        else
            out = new int[randomKeys.length()];

        int[] random;
        if(control)
            random = convertRandomControlKeys(randomKeys);
        else
            random = convertRandomKeys(randomKeys);

        for(int i=0; i < random.length; i++,pos++)
            out[pos] = random[i];

        return out;
    }

    private static int[] convertRandomKeys(String random) {
        int[] converted = new int[random.length()];
        for(int i=0; i < random.length(); i++)
            converted[i] = (int) random.charAt(i);

        return converted;
    }

    private static int[] convertRandomControlKeys(String random) {
        int[] converted = new int[random.length()];
        for(int i=0; i < random.length(); i++) {
            converted[i] = lookupControlKey(Character.toLowerCase(random.charAt(i)));
            if(converted[i] == -1)
                throw new RuntimeException("ERROR parsing "+random+" keys to aesh. Check your inputrc.");
        }

        return converted;
    }

    private static int lookupControlKey(char c) {
        switch (c) {
            case '@' : return 0;
            case 'a' : return 1;
            case 'b' : return 2;
            case 'c' : return 3;
            case 'd' : return 4;
            case 'e' : return 5;
            case 'f' : return 6;
            case 'g' : return 7;
            case 'h' : return 8;
            case 'i' : return 9;
            case 'j' : return 10;
            case 'k' : return 11;
            case 'l' : return 12;
            case 'm' : return 13;
            case 'n' : return 14;
            case 'o' : return 15;
            case 'p' : return 16;
            case 'q' : return 17;
            case 'r' : return 18;
            case 's' : return 19;
            case 't' : return 20;
            case 'u' : return 21;
            case 'v' : return 22;
            case 'w' : return 23;
            case 'x' : return 24;
            case 'y' : return 25;
            case 'z' : return 26;
            case '[' : return 27;
        }

        return -1;
    }
}
