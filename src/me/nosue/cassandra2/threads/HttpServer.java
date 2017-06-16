package me.nosue.cassandra2.threads;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer extends Thread{
    private int port = 80;

    @Override
    public void run() {
        try {
            ServerSocket server = new ServerSocket(port);
            while (true) {
                // Take connections and dump to ConnectionHandler
                Socket client = server.accept();
                new Thread(new ConnectionHandler(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
