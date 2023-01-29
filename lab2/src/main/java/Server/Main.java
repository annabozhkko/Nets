package Server;

import java.io.IOException;

public class  Main {
    public static void main(String[] args){
        if (args.length < 1) {
            System.err.println("Not enough parameters");
            return;
        }
        Server server = new Server(Integer.parseInt(args[0]));
        try {
            server.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}