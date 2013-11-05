package org.jboss.aesh.console.man;

import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface FileParser {

    List<String> loadPage(int columns) throws IOException;

    String getName();
}
