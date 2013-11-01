package org.jboss.aesh.console.man;

import org.jboss.aesh.console.man.parser.ManPageLoader;
import org.jboss.aesh.util.ANSI;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ManPageLoaderTester {
    @Test
    public void testParser() {
        ManPageLoader parser = new ManPageLoader();
        try {
            parser.setFile("src/test/resources/asciitest1.txt");
            parser.loadPage(80);

            //assertEquals(8, parser.getSections().size());

            assertEquals("NAME", parser.getSections().get(0).getName());
            assertEquals("SYNOPSIS", parser.getSections().get(1).getName());
            assertEquals("DESCRIPTION", parser.getSections().get(2).getName());
            assertEquals("OPTIONS", parser.getSections().get(3).getName());

            assertEquals(2, parser.getSections().get(3).getParameters().size());

            assertEquals("ASCIIDOC(1)", parser.getName());

            //System.out.println(parser.print());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testParser2() {
        ManPageLoader parser = new ManPageLoader();
        try {
            parser.setFile("src/test/resources/asciitest2.txt");
            parser.loadPage(80);

            assertEquals(10, parser.getSections().size());

            assertEquals("NAME", parser.getSections().get(0).getName());

            List<String> out = parser.getAsList();
            assertEquals(ANSI.getBold()+"NAME"+ ANSI.defaultText(), out.get(0));

//            for(String s : parser.getAsList())
//                System.out.println(s);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
