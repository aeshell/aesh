/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import org.jboss.aesh.util.LoggerUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Run infocmp and parse the output. Only usable on POSIX systems.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class InfocmpHandler {

    private Map<String, String> values;
    private static final String ESCAPE = "\\E";
    private static final String ESC_AS_STRING = "\u001B";

    private static final Logger LOGGER = LoggerUtil.getLogger(InfocmpHandler.class.getName());

    private static class InfocmpHolder {
        static final InfocmpHandler INSTANCE = new InfocmpHandler();
    }

    public static InfocmpHandler getInstance() {
        return InfocmpHolder.INSTANCE;
    }

    private InfocmpHandler() {
        if(values == null)
            parseInfocmp();
    }


    private void parseInfocmp() {
        values = new HashMap<>();
        fetchInfocmp();
    }

    private void fetchInfocmp() {
        InputStream in = null;
        ByteArrayOutputStream out = null;
        try {
            ProcessBuilder builder = new ProcessBuilder("infocmp");
            builder.redirectErrorStream(true);
            Process process = builder.start();

            in = process.getInputStream();
            out = new ByteArrayOutputStream();
            int c;
            while((c = in.read()) != -1)
                out.write(c);

            for(String keyValue : new String(out.toByteArray()).split(",")) {
                String[] valuePair = keyValue.split("=");
                if(valuePair.length == 2)
                    values.put(valuePair[0].trim(), valuePair[1]);
            }

            process.waitFor();
        }
        catch (IOException | InterruptedException e) {
            LOGGER.warning("Failed to execute infocmp, using default values: "+e.getMessage());
        }
        finally {
            try {
                if(in != null)
                    in.close();
                if(out != null)
                    out.close();
            }
            catch (IOException e) {
                LOGGER.warning("Failed to close streams: "+e.getMessage());
            }
        }
    }

    public int[] getAsInts(String key) {
        if(values.containsKey(key))
            return convertStringToInts(values.get(key));
        else
            return new int[0];
    }

    private int[] convertStringToInts(String input) {
        if(input.startsWith(ESCAPE)) {
            int[] out = new int[input.length()-1];
            out[0] = 27;
            int counter = 1;
            for(char c : input.substring(2).toCharArray()) {
                out[counter] = (int) c;
                counter++;
            }

            return out;
        }
        else {
            int[] out = new int[input.length()];
            int counter=0;
            for(char c : input.toCharArray()) {
                out[counter] = (int) c;
                counter++;
            }
            return out;
        }
    }

    public String get(String key) {
       if(values.containsKey(key)) {
           return values.get(key).replaceAll("\\\\E", ESC_AS_STRING);
       }
        else
           return "";
    }

}
