package client.protocols;

import java.io.*;
import java.net.Socket;
import client.ClientProtocolAdapter;

public class HTTPAdapter implements ClientProtocolAdapter {
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;

    public HTTPAdapter(Socket socket) throws IOException {
        this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    @Override
    public void send(String message) {
        try {
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String receive() throws IOException {
        return bufferedReader.readLine();
    }
}
