package org.jboss.jreadline.history;

import org.jboss.jreadline.console.Config;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Read the history file at init and write to it at shutdown
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class FileHistory extends InMemoryHistory {

    private String historyFile;

    public FileHistory(String fileName, int maxSize) throws IOException {
        super(maxSize);
        historyFile = fileName;

        readFile();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void start() {
                try {
                    writeFile();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Read specified history file to history buffer
     *
     * @throws IOException io
     */
    private void readFile() throws IOException {

        if(new File(historyFile).exists()) {

            BufferedReader reader =
                    new BufferedReader(new FileReader(historyFile));

            String line;
            while((line = reader.readLine()) != null)
                push(new StringBuilder(line));

            reader.close();
        }
    }

    /**
     * Write the content of the history buffer to file
     *
     * @throws IOException io
     */
    private void writeFile() throws IOException {
        new File(historyFile).delete();

        FileWriter fw = new FileWriter(historyFile);

        for(int i=0; i < size();i++)
            fw.write(get(i).append(Config.getLineSeparator()).toString());

        fw.flush();
        fw.close();
    }

}
