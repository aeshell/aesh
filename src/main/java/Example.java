import org.jboss.jreadline.console.Reader;

import javax.naming.event.NamingExceptionEvent;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Example {

    public static void main(String[] args) {

        try {
            Reader reader = new Reader(System.in, new OutputStreamWriter(System.out));
            //Reader reader = new Reader();

            PrintWriter out = new PrintWriter(System.out);

            String line;
            while ((line = reader.read("prompt> ")) != null) {
                out.println("======>\"" + line + "\"");
                out.flush();

                if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }
}
