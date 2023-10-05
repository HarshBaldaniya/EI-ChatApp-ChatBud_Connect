package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server implements Subject {
    private ServerSocket serverSocket;
    public static Map<String, List<ClientHandler>> chatRoomClients = new HashMap<>();
    public static Map<String, List<String>> chatRoomHistory = new HashMap<>();
    private List<Observer> observers = new ArrayList<>();

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public void addObserver(Observer o) {
        observers.add(o);
    }

    @Override
    public void removeObserver(Observer o) {
        observers.remove(o);
    }

    @Override
    public void notifyObservers(String message) {
        for (Observer observer : observers) {
            observer.update(message);
        }
    }

    public void startServer() {
        System.out.println("Server start at PORT 8080");
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String chatRoom = bufferedReader.readLine(); // First line is chat room
                String username = bufferedReader.readLine(); // Second line is username

                System.out.printf(username + " has joined the chat room: " + chatRoom + "\n");
                ClientHandler clientHandler = new ClientHandler(socket, chatRoom, username, this);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closedServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        Server server = new Server(serverSocket);
        server.startServer();
    }
}

