package com.text;

import com.text.commands.Command;
import com.text.commands.Field;

import java.io.*;
import java.net.*;

public class Client extends Thread {

    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Socket socket;
    private boolean isRunning;

    public Client(Socket socket, DataInputStream inputStream, DataOutputStream outputStream) {
        System.out.println("Client @ " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + " connected.");
        this.socket = socket;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.isRunning = true;
    }

    public void run() {
        while(isRunning) {
            try {
                String message = inputStream.readUTF();
                process(message);
            } catch (IOException e) {
                disconnect();
            }
        }
    }

    public void process(String message) {
        Command command;
        if(message.startsWith(".")) {
            message = message.substring(1);
            String name = message.split(" ")[0];
            command = new Command(name);
            if (message.split(" ").length > 0) {
                if(message.contains(" -")) {
                    Field value = new Field("value", message.substring(name.length()).split(" -")[0]);
                    command.addField(value);
                    String[] fields = message.split(" -");
                    for (int i = 1; i < fields.length; i++) {
                        name = fields[i].split(" ")[0];
                        Field field = new Field(name, fields[i].substring(name.length()));
                        command.addField(field);
                    }
                } else {
                    command.addField(new Field("value", message.substring(name.length())));
                }
            }
        } else {
            command = new Command("message");
            Field field = new Field("value", message);
            command.addField(field);
        }
        process(command);
    }

    public void process(Command command) {
        dump(command);
        switch(command.getName()) {
            case "message":
                send(command.getField("value").getValue());
                break;
        }
    }

    public void send(String message) {
        try {
            outputStream.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        System.out.println("Client @ " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + " disconnected.");
        isRunning = false;
        try {
            this.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void dump(Command command) {
        System.out.println("----------------------------------------");
        System.out.println("               Command               ");
        System.out.println("----------------------------------------");
        System.out.println("Name: " + command.getName());
        System.out.println("Field Count: " + command.getFields().size());
        System.out.println();
        for (Field field : command.getFields()) {
            System.out.println("\tField:");
            System.out.println("\tName: " + field.getName());
            System.out.println("\tValue: " + field.getValue());
            System.out.println();
        }
        System.out.println("----------------------------------------");
    }

}
