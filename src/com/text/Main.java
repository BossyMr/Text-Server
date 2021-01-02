package com.text;

public class Main {

    private int port;
    private Server server;

    public Main(int port) {
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
