package me.nosue.cassandra2;

import me.nosue.cassandra2.threads.HttpServer;

public class Cassandra{
    public static void main(String[] args){
        new HttpServer().start();
    }
}
