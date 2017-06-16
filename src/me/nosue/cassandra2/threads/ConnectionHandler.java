package me.nosue.cassandra2.threads;

import me.nosue.cassandra2.io.OsuWriter;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;

public class ConnectionHandler extends Thread {
    private Socket client;

    public ConnectionHandler(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        // This handles the connection off of the different web handlers, HTTP and HTTPS.
        // We will need to parse the HTTP headers first, we will start by reading text before going the whole binary
        // reading route, this would probably be better with a HTTP server library

        HashMap<String, String> requestHeaders = new HashMap<>();

        try {
            InputStreamReader inputStreamReader = new InputStreamReader(client.getInputStream());
            String temp = "";
            while (true) {
                char character = (char) inputStreamReader.read();
                if (character == '\r') {
                    // Windows line endings are \r\n, for our purposes \r is just noise so we'll ignore it
                    continue;
                }
                if (character == '\n') {
                    if (temp.length() > 0) {
                        if (requestHeaders.size() == 0) {
                            // First element, let's assume this is the HTTP request bit
                            // Let's store in case we want to use it in the future
                            String[] data = temp.split(" "); // POST / HTTP/1.1
                            requestHeaders.put("RequestType", data[0]); // POST
                            requestHeaders.put("RequestLocation", data[1]); // /
                            requestHeaders.put("HttpVersion", data[2]); // HTTP/1.1
                        } else {
                            String[] headerData = temp.split(":");
                            String key = headerData[0].trim();
                            String value = headerData[1].trim();

                            requestHeaders.put(key, value);
                        }

                        temp = "";
                    } else {
                        // Double new line, should be end of HTTP header
                        break;
                    }
                } else {
                    // No control characters so probably just part of the header we're reading, let's keep it
                    temp += character;
                }
            }

            ByteArrayOutputStream outputBody = new ByteArrayOutputStream();
            HashMap<String, String> outputHeaders = new HashMap<>();
            outputHeaders.put("connection", "close");
            outputHeaders.put("server", "Cassandra2");

            OsuWriter writer = new OsuWriter(outputBody);

            if (requestHeaders.getOrDefault("User-Agent", "").equals("osu!")) {
                // Do osu! server magic here

                if (requestHeaders.containsKey("osu-token")) {
                    // We have a token, that means we're probably authenticated
                } else {
                    // We don't have a token, probably a login request
                    // Format:
                    // Username
                    // Password (in MD5)
                    // version|utc offset|display city location (1 or 0)|machine info|block nonfriend pms (1 or 0)
                    // Machine info is
                    // exe hash:MAC addresses separated by . or "runningunderwine":MD5 of previous value:uninstallID MD5 hashed twice:disk signature MD5 hashed

                    // OK, let's read this
                    String[] loginData = new String[3];
                    temp = "";
                    for (int i = 0; i < 2; i++) {
                        // Basically read string character by character again
                        char character = (char) inputStreamReader.read();
                        if (character == '\r') {
                            continue;
                        }

                        if (character == '\n') {
                            loginData[i] = temp;
                            temp = "";
                        } else {
                            temp += character;
                        }
                    }

                    // TODO real auth


                }
            } else {
                // Web interface, might show something here but for the mean time let's be basic
                writer.writeStringBytes("Cassandra2 Bancho <3");
            }

            DataOutputStream outputStream = new DataOutputStream(client.getOutputStream());

            // HTTP header stuff
            outputStream.writeBytes("HTTP/1.1 200 OK"); // Everything's probably fine
            for (String header : outputHeaders.keySet()) {
                outputStream.writeBytes(String.format("%s: %s\r\n", header, outputHeaders.get(header)));
            }

            outputStream.writeBytes("\r\n"); // Double new line to indicate header end

            // Close handle on outputBody first
            outputBody.close();

            // Output the body
            outputStream.write(outputBody.toByteArray());

            // Close stuff
            outputStream.close();
            inputStreamReader.close();


        } catch (IOException e){
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException e){
                // The world hates us
                e.printStackTrace();
            }
        }
    }
}
