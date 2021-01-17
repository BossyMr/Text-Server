package com.text;

import com.text.commands.Command;
import com.text.commands.Field;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Server implements Runnable{
    Scanner scanner = new Scanner(System.in);

    public final List<Client> clients = new ArrayList<>();
    private final List<Client> responses = new ArrayList<>();
    private final int MAX_ATTEMPTS = 10;

    private final int port;
    private DatagramSocket socket;

    private Thread server, receive, console;
    private boolean isRunning = false;
    public boolean isDebug = false;

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

        console = new Thread(this::console, "Server/Console");
        console.start();
        Console.log("Started Text/Console");
    }

    public void run() {
        long lastTime = System.nanoTime();
        final double ns = 1000000000.0 / 1.0;
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
        Command command = new Command("ACTIVE");
        send(command);
    }

    public void console() {
        while(isRunning) {
            if(scanner.hasNextLine()) {
                String line = scanner.nextLine();
                Command command = new Command(line.split(" ")[0]);
                String value = line.substring(line.split(" ")[0].length());
                command.addField(new Field("VALUE", value.split(" -")[0]));
                for (int i = 1; i < line.split(" -").length; i++) {
                    if (!(i + 1 < line.split(" -").length)) {
                        String[] option = line.split(" -")[i].split(" ");
                        String lastOption = String.join(" ", option).substring(option[0].length());
                        command.addField(new Field(option[0], lastOption));
                        break;
                    }
                    String[] option = line.split(" -| -")[i].split(" ");
                    command.addField(new Field(option[0], option[1]));
                }
                InetAddress localHost = null;
                try {
                    localHost = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                Client client = new Client(localHost, port);
                client.setName("CONSOLE");
                process(command, client);
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
        if(new String(data, 0,4).equals("TEXT")) {
            Command command = Command.serialize(text);
            for(Client client : clients) {
                if (client.getAddress().equals(address)) {
                    process(command, client);
                    return;
                }
            }
            switch(command.getName()) {
                case "CONNECT":
                    clients.add(new Client(address, port));
                    Console.log("Client @ " + address.getHostAddress() + ":" + port + " connected.");
                    break;
            }
        }
    }

    public void process(Command command, Client client) {
        Command output;
        if(isDebug) Console.dump(command);
        switch(command.getName()) {
            case "ACTIVE":
                if(!responses.contains(client)) {
                    responses.add(client);
                }
                break;
            case "DISCONNECT":
                disconnect(client);
                break;
            case "MESSAGE":
                output = new Command("MESSAGE");
                Field message = new Field("MESSAGE", command.getField("MESSAGE").getValue());
                output.addField(message);
                Field name = new Field("NAME", client.getName());
                output.addField(name);
                send(output);
                Console.log(client.getName() + " > " + command.getField("MESSAGE").getValue());
                break;
            case "NAME":
                client.setName(command.getField("NAME").getValue());
                break;
            case "STOP":
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

    public void stop() {
        Console.log("Stopping Text/Server");
        for (Client client : clients) {
            disconnect(client);
        }
        isRunning = false;
        socket.close();
    }

    public void disconnect(Client client) {
        clients.remove(client);
        Console.log("Client " + client.getName() + " @ " + client.getAddress().getHostAddress() + ":" + client.getPort() + " disconnected.");
    }
}