package client;

import java.io.IOException;

public interface ClientProtocolAdapter {
    void send(String message);
    String receive() throws IOException;
}

