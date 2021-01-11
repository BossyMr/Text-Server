package com.text;

import com.text.commands.*;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class Server implements Runnable{
    Scanner scanner = new Scanner(System.in);

    private final List<Client> clients = new ArrayList<>();

    private final List<InetAddress> responses = new ArrayList<>();
    private final int MAX_ATTEMPTS = 5;

    private final int port;
    private DatagramSocket socket;

    private Thread server, receive;
    private boolean isRunning;

    public Server(int port) {
        this.port = port;
        try {
            socket = new DatagramSocket(this.port);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }
        isRunning = true;
        System.out.println("Started Text/Server on port: " + port);

        server = new Thread(this, "Server: " + port);
        server.start();
        receive = new Thread(this::receive, "Server/Listen");
        receive.start();
    }

    public void run() {
        long lastTime = System.nanoTime();
        long timer = System.currentTimeMillis();
        final double ns = 1000000000.0 / 60.0;
        double delta = 0;
        while (isRunning) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while (delta >= 1) {
                update();
                delta--;
            }
            check();
        }
    }

    public void update() {
        Command command = new Command("STATUS");
        for(Client client : clients) {
            command.addField(new Field("NAME", client.getName()));
        }
        sendAll(Command.deserialize(command).getBytes());
        for(Client client : clients) {
            for(InetAddress address : responses) {
                if(client.getAddress().equals(address)) {
                    client.setAttempts(0);
                }
            }
            client.setAttempts(client.getAttempts() - 1);
            if(client.getAttempts() >= MAX_ATTEMPTS) {
                disconnect(client);
            }
        }
    }

    public void check() {
        String line = scanner.nextLine();
        if(line.trim().isEmpty()) return;
        if (!line.startsWith("/")) {
            Command message = new Command("MESSAGE");
            message.addField(new Field("NAME", "SERVER"));
            message.addField(new Field("MESSAGE", line));
            sendAll(Command.deserialize(message).getBytes());
        }
        line = line.substring(1);
        switch(line.split(" ")[0]) {
            case "clients":
                System.out.print("Clients: ");
                for (int i = 0; i < clients.size() - 1; i++) {
                    System.out.print(clients.get(i).getName() + ", ");
                }
                System.out.println(clients.get(clients.size() - 1).getName());
                break;
            case "stop":
                stop();
                break;
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
        if(new String(data, 0,4).equals("TEXT")) {
            Command command = Command.serialize(text);
            assert command != null;
            if(command.getName().equals("MESSAGE")) {
                Command message = new Command("MESSAGE");
                message.addField(new Field("MESSAGE", command.getField("MESSAGE").getValue()));
                for(Client client : clients) {
                    if (client.getAddress().equals(address)) {
                        message.addField(new Field("NAME", client.getName()));
                        sendAll(Command.deserialize(message).getBytes());
                    }
                }
            } else {
                switch(command.getName()) {
                    case "CONNECT":
                        for(Client client : clients) {
                            if(client.getAddress().equals(address)) {
                                break;
                            }
                        }
                        clients.add(new Client(address, port));
                        System.out.println("Client @ " + address.getHostAddress() + ":" + port + " connected.");
                        break;
                    case "DISCONNECT":
                        for(Client client : clients) {
                            if(client.getAddress().equals(address)) {
                                disconnect(client);
                                break;
                            }
                        }
                        break;
                    case "NAME":
                        for(Client client : clients) {
                            if(client.getAddress().equals(address)) {
                                client.setName(command.getField("NAME").getValue());
                            }
                        }
                        break;
                    case "ACTIVE":
                        for(Client client : clients) {
                            if(client.getAddress().equals(address)) {
                                responses.add(client.getAddress());
                            }
                        }
                        break;
                }
            }
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

    public void sendAll(byte[] data) {
        for (Client client : clients) {
            send(data, client.getAddress(), client.getPort());
        }
    }

    public void stop() {
        System.out.println("Stopped Text/Server");
        for (Client client : clients) {
            disconnect(client);
        }
        isRunning = false;
        socket.close();
    }

    public void disconnect(Client client) {
        clients.remove(client);
        String message = "Client " + client.getName() + " @ " + client.getAddress().getHostAddress() + ":" + client.getPort() + " disconnected.";
        System.out.println(message);
    }
}