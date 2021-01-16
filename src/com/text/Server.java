package com.text;

import com.text.commands.Command;
import com.text.commands.Field;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Server implements Runnable{

    public final List<Client> clients = new ArrayList<>();

    private final List<InetAddress> responses = new ArrayList<>();
    private final int MAX_ATTEMPTS = 120;

    private Console console;

    private final int port;
    private DatagramSocket socket;

    private Thread server, receive;
    private boolean isRunning = false;
    public boolean isDebug = false;

    public Server(int port) {
        console = new Console();
        this.port = port;
        try {
            socket = new DatagramSocket(this.port);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }
        isRunning = true;

        server = new Thread(this, "Server: " + port);
        server.start();
        receive = new Thread(this::receive, "Server/Receive");
        receive.start();
    }

    public void run() {
        Console.log("Started Text/Server on port: " + port);
        long lastTime = System.nanoTime();
        final double ns = 1000000000.0 / 20;
        double delta = 0;
        while (isRunning) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while (delta >= 1) {
                update();
                delta--;
            }
            input(console.update());
        }
    }

    public void update() {
        Console.log("UPDATING...");
        for(int i = 0; i < clients.size(); i++) {
            clients.get(i).setAttempts(clients.get(i).getAttempts() + 1);
            for(int ii = 0; ii < responses.size(); ii++) {
                if(clients.get(i).getAddress().equals(responses.get(ii))) {
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
        Command command = new Command("ACTIVE");
        sendAll(Command.deserialize(command).getBytes());
    }

    public void input(Command command) {
        if(isDebug) Console.dump(command);
        switch(command.getName()) {
            case "message":
                // REQUIRED: -n [NAME]
                Command message = new Command("MESSAGE");
                message.addField(new Field("NAME", command.getField("n").getValue()));
                message.addField(new Field("MESSAGE", command.getField("VALUE").getValue()));
                sendAll(Command.deserialize(message).getBytes());
                break;
            case "clients":
                System.out.print("Clients: ");
                for (int i = 0; i < clients.size(); i++) {
                    System.out.print(clients.get(i).getName() + ", ");
                }
                System.out.println();
                break;
            case "debug":
                isDebug = !isDebug;
                break;
            case "disconnect":
                // REQUIRED: -u [USER]
                for(int i = 0; i < clients.size(); i++) {
                    if(clients.get(i).getName().equals(command.getField("VALUE").getValue())) {
                        disconnect(clients.get(i));
                        break;
                    }
                }
                break;
            case "stop":
                stop();
                break;
        }
    }

    public void receive() {
        Console.log("Started Text/Receive");
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
        if(isDebug) Console.dump(packet);
        if(new String(data, 0,4).equals("TEXT")) {
            Command command = Command.serialize(text);
            if(isDebug) Console.dump(command);
            if(command.getName().equals("MESSAGE")) {
                Command message = new Command("MESSAGE");
                message.addField(new Field("MESSAGE", command.getField("MESSAGE").getValue()));
                for(Client client : clients) {
                    if (client.getAddress().equals(address)) {
                        message.addField(new Field("NAME", client.getName()));
                        sendAll(Command.deserialize(message).getBytes());
                        DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm:ss");
                        String output = "[" + LocalTime.now().format(format) + " - " + client.getName() + "]: " + command.getField("MESSAGE").getValue();
                        System.out.println(output);
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
                        Console.log("Client @ " + address.getHostAddress() + ":" + port + " connected.");
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
        Console.log("Stopped Text/Server");
        for (Client client : clients) {
            disconnect(client);
        }
        isRunning = false;
        socket.close();
    }

    public void disconnect(Client client) {
        clients.remove(client);
        String message = "Client " + client.getName() + " @ " + client.getAddress().getHostAddress() + ":" + client.getPort() + " disconnected.";
        Console.log(message);
    }
}