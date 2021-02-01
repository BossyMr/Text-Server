package com.text;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server implements Runnable {

    public List<Thread> clients = new ArrayList<>();

    private int port;
    private ServerSocket serverSocket;

    private Thread server;
    private boolean isRunning;

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        isRunning = true;
        System.out.println("Connected @ " + port);
        server = new Thread(this, "Server @ " + port);
        server.start();
    }

    public void run() {
        while(isRunning) {
            try {
                Socket socket = serverSocket.accept();
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                Thread client = new Client(socket, inputStream, outputStream);
                clients.add(client);
                client.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}