package com.text;

import com.text.commands.Command;
import com.text.commands.Field;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Scanner;

public class Console {
    Scanner scanner = new Scanner(System.in);

    public Console() {
        System.out.println("Server/Console is started.");
    }

    public Command update() {
        String line = scanner.nextLine();
        Command command = new Command(line.split(" ")[0]);
        String value = line.substring(line.split(" ")[0].length());
        command.addField(new Field("VALUE", value.split(" -")[0]));
        for (int i = 1; i < line.split(" -").length; i++) {
            if (!(i + 1 < line.split(" -").length)) {
                String[] option = line.split(" -")[i].split(" ");
                command.addField(new Field(option[0], option[1]));
                break;
            }
            String[] option = line.split(" -| -")[i].split(" ");
            command.addField(new Field(option[0], option[1]));
        }
        return command;
    }

    public static void dump(DatagramPacket packet) {
        byte[] data = packet.getData();
        InetAddress address = packet.getAddress();
        int port = packet.getPort();

        System.out.println("----------------------------------------");
        System.out.println("PACKET:");
        System.out.println("\t" + address.getHostAddress() + ":" + port);
        System.out.println();
        System.out.println("\tContents:");
        System.out.print("\t\t");

        for (int i = 0; i < packet.getLength(); i++) {
            System.out.printf("%x ", data[i]);
            if ((i + 1) % 16 == 0)
                System.out.print("\n\t\t");
        }

        System.out.println();
        System.out.println("----------------------------------------");
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
