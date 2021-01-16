package com.text;

public class Main {

    private final int port;
    private final Server server;

    public Main(int port) {
        System.out.println("Launched: Text/Server");
        this.port = port;
        server = new Server(port);
    }

    public static void main(String[] args) {
        if(args.length != 1) {
            System.out.println("Usage: java -jar Text-Server.jar [port]");
            return;
        }
        new Main(Integer.parseInt(args[0]));
    }

}
