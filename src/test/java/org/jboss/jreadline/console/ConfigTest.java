package org.jboss.jreadline.console;

import junit.framework.TestCase;

import java.io.IOException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConfigTest extends TestCase {

    public ConfigTest(String name) {
        super(name);
    }


    public void testParseInputrc() throws IOException {
        Config config = new Config();
        Settings settings = new Settings();

        config.parseInputrc("src/test/resources/inputrc1", settings);
    }
}
