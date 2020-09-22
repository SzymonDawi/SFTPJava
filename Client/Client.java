package Client;// File Name GreetingClient.java
import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Client {

    public static void main(String [] args) {
        String serverName = "localhost";
        boolean running = true;
        int port = 115;
        try {
            Socket client = new Socket(serverName, port);

            OutputStream outToServer = client.getOutputStream();
            DataOutputStream out = new DataOutputStream(outToServer);

            InputStream inFromServer = client.getInputStream();
            DataInputStream in = new DataInputStream(inFromServer);

            System.out.println("Server says " + in.readUTF());

            BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));

            while(true){
                //system loop asks for commands and prints out what the server is sending
                System.out.print("Command: ");
                String userInput = userIn.readLine() + "\n";

                out.writeUTF(userInput);

                System.out.println("Server says " + in.readUTF());

                if(userInput.equals("DONE\n")){
                    //stops the client when the user types DONE
                    client.close();
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}