package com.text;

import java.net.InetAddress;

public class Client {

    public String name;
    public InetAddress address;
    public int port;
    public final int UUID;
    public int attempts = 0;

    public Client(String name, InetAddress address, int port, final int UUID) {
        this.name = name;
        this.address = address;
        this.port = port;
        this.UUID = UUID;
    }

}
