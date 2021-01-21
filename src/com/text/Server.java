package com.text;

import com.text.commands.Command;
import com.text.commands.Field;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class Server implements Runnable{

    public final List<Client> clients = new ArrayList<>();
    private final List<Client> responses = new ArrayList<>();
    private final int MAX_ATTEMPTS = 120;

    private final int port;
    private DatagramSocket socket;

    private Thread server, receive;
    private boolean isRunning = false;

    public Server(int port) {
        this.port = port;
        try {
            socket = new DatagramSocket(this.port);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }
        isRunning = true;

        server = new Thread(this, "Text/Server");
        server.start();
        Console.log("Started Text/Server on port: " + port);

        receive = new Thread(this::receive, "Server/Receive");
        receive.start();
        Console.log("Started Text/Receive");
    }

    public void run() {
        long lastTime = System.nanoTime();
        final double ns = 1000000000.0 / 20.0;
        double delta = 0;
        while (isRunning) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            if (delta >= 1) {
                update();
                delta--;
            }
        }
    }

    public void update() {
        for(int i = 0; i < clients.size(); i++) {
            clients.get(i).setAttempts(clients.get(i).getAttempts() + 1);
            for(int ii = 0; ii < responses.size(); ii++) {
                if(clients.get(i).equals(responses.get(ii))) {
                    clients.get(i).setAttempts(0);
                    break;
                }
            }
            if(clients.get(i).getAttempts() >= MAX_ATTEMPTS) {
                disconnect(clients.get(i));
                break;
            }
        }
        responses.clear();
        send(new Command("active"));
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
            for(Client client : clients) {
                if (client.getAddress().equals(address)) {
                    process(command, client);
                    return;
                }
            }
            switch(command.getName()) {
                case "connect":
                    clients.add(new Client(address, port));
                    Console.log("Client @ " + address.getHostAddress() + ":" + port + " connected.");
                    send(message("Client @ " + address.getHostAddress() + ":" + port + " connected."));
                    break;
            }
        }
    }

    public void process(Command command, Client client) {
        switch(command.getName()) {
            case "active":
                if(!responses.contains(client)) {
                    responses.add(client);
                }
                break;
            case "clients":
                String message = null;
                if(clients.size() > 0) {
                    message = "Clients: ";
                    for(int i = 0; i < clients.size(); i++) {
                        message += clients.get(i).getName() + ", ";
                    }
                }
                send(message(message), client);
                break;
            case "disconnect":
                disconnect(client);
                break;
            case "message":
                send(message(client.getName() + " > " + command.getField("value").getValue()));
                Console.log(client.getName() + " > " + command.getField("value").getValue());
                break;
            case "name":
                client.setName(command.getField("value").getValue());
                break;
            case "stop":
                stop();
                break;
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

    public void send(Command command, Client client) {
        send(Command.deserialize(command).getBytes(), client.getAddress(), client.getPort());
    }

    public void send(Command command) {
        for(int i = 0; i < clients.size(); i++) {
            send(command, clients.get(i));
        }
    }

    public Command message(String message) {
        Command output = new Command("message");
        Field value = new Field("value", message);
        output.addField(value);
        return output;
    }

    public void stop() {
        Console.log("Stopping Text/Server");
        for (int i = 0; i < clients.size(); i++) {
            disconnect(clients.get(i));
        }
        isRunning = false;
        socket.close();
    }

    public void disconnect(Client client) {
        clients.remove(client);
        Console.log("Client " + client.getName() + " @ " + client.getAddress().getHostAddress() + ":" + client.getPort() + " disconnected.");
        send(message("Client " + client.getName() + " @ " + client.getAddress().getHostAddress() + ":" + client.getPort() + " disconnected."));
    }
}