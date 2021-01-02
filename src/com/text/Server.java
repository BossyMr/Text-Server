package com.text;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Server implements Runnable{
    Scanner scanner = new Scanner(System.in);

    private List<Client> clients = new ArrayList<Client>();
    private List<Integer> responses = new ArrayList<Integer>();
    private int MAX_ATTEMPTS = 5;

    private int port;
    private DatagramSocket socket;

    private Thread server, receive, manage;
    private boolean isRunning;

    public Server(int port) {
        this.port = port;
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }
        server = new Thread(this, "Server: " + port);
        server.start();
    }

    public void run() {
        isRunning = true;
        System.out.println("Started Text/Server on port: " + port);
        receive = new Thread(() -> receive(), "Server/Listen");
        receive.start();
        manage = new Thread(() -> manage(), "Server/Manage");
        manage.start();
        while (isRunning) {
            String line = scanner.nextLine();
            if (!line.startsWith("/")) {
                sendAll("/m/Server: " + line);
            }
            line = line.substring(1);
            if(line.startsWith("clients")) {
                System.out.print("Clients: ");
                for(int i = 0; i < clients.size(); i++) {
                    if(i + 1 < clients.size()) {
                        System.out.print(clients.get(i).name);
                    } else { System.out.print(clients.get(i).name + ", "); }
                }
            }
            if(line.startsWith("stop")) {
                stop();
            }
        }
    }

    public void manage() {
        while(isRunning) {
            sendAll("/ir/");
            sendStatus();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < clients.size(); i++) {
                Client client = clients.get(i);
                if (!clients.contains(client.UUID)) {
                    if (client.attempts >= MAX_ATTEMPTS) {
                        disconnect(client.UUID, false);
                    } else {
                        client.attempts++;
                    }
                } else {
                    responses.remove(new Integer(client.UUID));
                    client.attempts = 0;
                }
            }
        }
    }

    public void receive() {
        while(isRunning) {
            byte[] data = new byte[1024];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            process(packet);
        }
    }

    public void process(DatagramPacket packet) {
        byte[] data = packet.getData();
        InetAddress address = packet.getAddress();
        int port = packet.getPort();
        String text = new String(data);
        if(text.startsWith("/c/")) {
            String name = text.split("/c/")[1];
            int UUID = clients.size(); // TODO: FIX THIS
            clients.add(new Client(name, address, port, UUID));
            System.out.println("UUID: " + UUID + ", Name: " + name + " Connected on: " + address.toString() + ":" + port);
            String message = "/cr/" + UUID;
            send(message.getBytes(), address, port);
            return;
        }
        if(text.startsWith("/d/")) {
            String UUID = text.split("/d/")[1];
            disconnect(Integer.parseInt(UUID), true);
        }
        if(text.startsWith("/m/")) {
            sendAll(text);
            return;
        }
        if(text.startsWith("/i/")) {
            responses.add(Integer.parseInt(text.split("/i/")[1]));
            return;
        }
    }

    public void send(byte[] data, InetAddress address, int port) {
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendStatus() {
        if(clients.size() <= 0) return;
        String users = "/u/";
        for (int i = 0; i < clients.size(); i++) {
            users += clients.get(i).name + ";";
        }
        sendAll(users);
    }

    public void sendAll(String text) {
        if (text.startsWith("/m/")) {
            String message = text.substring(3);
            System.out.println(message);
        }
        for (int i = 0; i < clients.size(); i++) {
            Client client = clients.get(i);
            send(text.getBytes(), client.address, client.port);
        }
    }

    public void stop() {
        for(int i = 0; i < clients.size(); i++) {
            disconnect(clients.get(i).UUID, true);
        }
        isRunning = false;
        socket.close();
    }

    public void disconnect(int UUID, boolean status) {
        Client client = null;
        for(int i = 0; i < clients.size(); i++) {
            if(clients.get(i).UUID == UUID) {
                client = clients.get(i);
                clients.remove(client);
                String message = "Client " + client.name + " (" + client.UUID + ") @ " + client.address.toString() + ":" + client.port;
                if(status) { message += " disconnected."; }
                else { message += " timed out."; }
                return;
            }
        }
    }
}
