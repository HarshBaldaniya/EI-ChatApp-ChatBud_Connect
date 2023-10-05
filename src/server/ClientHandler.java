package server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable, Observer {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;
    private String chatRoom;
    private Subject server;

    public ClientHandler(Socket socket, String chatRoom, String username, Subject server) {
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = username;
            this.chatRoom = chatRoom;
            this.server = server;

            List<ClientHandler> clientsInRoom = Server.chatRoomClients.getOrDefault(chatRoom, new ArrayList<>());
            clientsInRoom.add(this);
            Server.chatRoomClients.put(chatRoom, clientsInRoom);

            broadcastMessage("SERVER: " + username + " has joined the chat!");
            sendChatHistory();
        } catch (IOException e) {
            closeEverything();
        }
    }

    private void sendChatHistory() {
        List<String> history = Server.chatRoomHistory.getOrDefault(chatRoom, new ArrayList<>());
        for (String message : history) {
            try {
                bufferedWriter.write(message);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } catch (IOException e) {
                closeEverything();
            }
        }
    }

    @Override
    public void update(String message) {
        try {
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        String messageFromClient;
        try {
            while ((messageFromClient = bufferedReader.readLine()) != null) {
                if (messageFromClient.startsWith("/pm")) {
                    handlePrivateMessage(messageFromClient);
                } else if (messageFromClient.equals("/list")) {
                    sendActiveUsersList();
                } else {
                    broadcastMessage(messageFromClient);
                }
            }
        } catch (IOException e) {
            closeEverything();
        } finally {
            broadcastMessage("SERVER: " + username + " has left the chat!");
            removeClientFromActiveList();
        }
    }

    public void sendActiveUsersList() {
        List<ClientHandler> clientsInRoom = Server.chatRoomClients.get(chatRoom);
        StringBuilder activeUsers = new StringBuilder("Active users in " + chatRoom + ": ");
        for (ClientHandler clientHandler : clientsInRoom) {
            activeUsers.append(clientHandler.username).append(", ");
        }
        if (activeUsers.length() > 0) {
            activeUsers.setLength(activeUsers.length() - 2);
        }
        try {
            bufferedWriter.write(activeUsers.toString());
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything();
        }
    }

    public void removeClientFromActiveList() {
        List<ClientHandler> clientsInRoom = Server.chatRoomClients.get(chatRoom);
        if (clientsInRoom != null) {
            clientsInRoom.remove(this);
            if (clientsInRoom.isEmpty()) {
                Server.chatRoomClients.remove(chatRoom);
            } else {
                Server.chatRoomClients.put(chatRoom, clientsInRoom);
            }
        }
    }

    public void handlePrivateMessage(String message) {
        String[] parts = message.split(" ", 3);
        if (parts.length < 3) {
            try {
                bufferedWriter.write("SERVER: Invalid private message format. Use /pm <username> <message>");
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } catch (IOException e) {
                closeEverything();
            }
            return;
        }

        String targetUsername = parts[1];
        String privateMessage = parts[2];

        List<ClientHandler> clientsInRoom = Server.chatRoomClients.get(chatRoom);
        for (ClientHandler clientHandler : clientsInRoom) {
            if (clientHandler.username.equals(targetUsername)) {
                try {
                    clientHandler.bufferedWriter.write("(Private Msg) " + username + ": " + privateMessage);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                    return;
                } catch (IOException e) {
                    closeEverything();
                }
            }
        }

        try {
            bufferedWriter.write("SERVER: " + targetUsername + " not found in this chat room!");
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything();
        }
    }

    private static final int MAX_HISTORY_SIZE = 100;

    public void broadcastMessage(String messageToSend) {
        List<ClientHandler> clientsInRoom = Server.chatRoomClients.get(chatRoom);
        List<String> history = Server.chatRoomHistory.getOrDefault(chatRoom, new ArrayList<>());
        history.add(messageToSend);
        Server.chatRoomHistory.put(chatRoom, history);

        if (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }

        for (ClientHandler clientHandler : clientsInRoom) {
            if (!clientHandler.username.equals(this.username)) {
                try {
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                } catch (IOException e) {
                    closeEverything();
                }
            }
        }
    }

    public void closeEverything() {
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (bufferedWriter != null) bufferedWriter.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
