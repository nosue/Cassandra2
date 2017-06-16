package me.nosue.cassandra2.threads;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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

        HashMap<String, String> headers = new HashMap<>();

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
                        if (headers.size() == 0) {
                            // First element, let's assume this is the HTTP request bit
                            // Let's store in case we want to use it in the future
                            String[] data = temp.split(" "); // POST / HTTP/1.1
                            headers.put("RequestType", data[0]); // POST
                            headers.put("RequestLocation", data[1]); // /
                            headers.put("HttpVersion", data[2]); // HTTP/1.1
                        } else {
                            String[] headerData = temp.split(":");
                            String key = headerData[0].trim();
                            String value = headerData[1].trim();

                            headers.put(key, value);
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

            OutputStreamWriter writer = new OutputStreamWriter(client.getOutputStream());
            for (String header : headers.keySet()) {
                writer.write(String.format("%s: %s\n", header, headers.get(header)));
            }


            writer.close();
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
