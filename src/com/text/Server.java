package com.text;

import com.text.commands.Command;
import com.text.commands.Field;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Server implements Runnable{

    public final List<Client> clients = new ArrayList<>();

    private final List<InetAddress> responses = new ArrayList<>();
    private final int MAX_ATTEMPTS = 60;

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
        System.out.println("Started Text/Server on port: " + port);

        server = new Thread(this, "Server: " + port);
        server.start();
        receive = new Thread(this::receive, "Server/Receive");
        receive.start();
    }

    public void run() {
        long lastTime = System.nanoTime();
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
            input(console.update());
        }
    }

    public void update() {
        Iterator<Client> clientIterator = clients.iterator();
        while(clientIterator.hasNext()) {
            Client client = clientIterator.next();
            client.setAttempts(client.getAttempts() + 1);
            for(int i = 0; i < responses.size(); i++) {
                if(client.getAddress().equals(responses.get(i))) {
                    client.setAttempts(0);
                    break;
                }
            }
            if(client.getAttempts() >= MAX_ATTEMPTS) {
                clientIterator.remove();
                String message = "Client " + client.getName() + " @ " + client.getAddress().getHostAddress() + ":" + client.getPort() + " timed out.";
                System.out.println(message);
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
                        String output = client.getName() + ": " + command.getField("MESSAGE").getValue();
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