package com.text;

import java.net.InetAddress;

public class Client {

    private String name;
    private InetAddress address;
    private int port;
    private int attempts = 0;

    public Client(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public String getName() {
        if(name == null) {
            return address.getHostAddress();
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }
}
