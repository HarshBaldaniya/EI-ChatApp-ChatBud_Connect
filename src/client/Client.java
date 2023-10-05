package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import client.protocols.HTTPAdapter;

public class Client {
    private Socket socket;
    private ClientProtocolAdapter adapter;
    private String username;
    private String chatRoom;

    public Client(Socket socket, String username, String chatRoom, ClientProtocolAdapter adapter) {
        this.socket = socket;
        this.adapter = adapter;
        this.username = username;
        this.chatRoom = chatRoom;
    }

    public void sendMessage() {
        adapter.send(chatRoom);
        adapter.send(username);

        Scanner scanner = new Scanner(System.in);
        while (socket.isConnected()) {
            String messageToSend = scanner.nextLine();
            if (messageToSend.startsWith("/pm")) {
                adapter.send(messageToSend);
            } else if (messageToSend.equals("/list")) {
                adapter.send("/list");
            } else {
                String formattedMessage = username + ": " + messageToSend;
                adapter.send(formattedMessage);
            }
        }
    }

    public void listenForMessage() {
        new Thread(() -> {
            String msgFromGroupChat;
            try {
                while ((msgFromGroupChat = adapter.receive()) != null) {
                    System.out.println(msgFromGroupChat);
                }
            } catch (IOException e) {
                System.err.println("Error while receiving message: " + e.getMessage());
            }
        }).start();
    }


    public void closeEverything() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("\t\tWelcome to ChatBud Connect application!");
        System.out.println();
        System.out.println("- First register the user name then create or join the room!");
        System.out.println("- After join the room you have some addition operation like this:- ");
        System.out.println("\t/list -> check active user in the chat room.");
        System.out.println("\t/pm <username> <message> -> write private message.");
        System.out.println();

        Scanner scanner = new Scanner(System.in);
        System.out.printf("Enter username: ");
        String username = scanner.nextLine();

        System.out.printf("Do you want to 'join' or 'create' a chat room? ");
        String action = scanner.nextLine().toLowerCase();

        String chatRoom = "";
        if (action.equals("create") || action.equals("join")) {
            System.out.printf("Enter name of the chat room: ");
            chatRoom = scanner.nextLine();
            System.out.println();
        } else {
            System.out.println("Invalid action. Exiting...");
            return;
        }

        Socket socket = new Socket("localhost", 8080);
        ClientProtocolAdapter adapter = new HTTPAdapter(socket);
        Client client = new Client(socket, username, chatRoom, adapter);
        client.listenForMessage();
        client.sendMessage();
    }
}

