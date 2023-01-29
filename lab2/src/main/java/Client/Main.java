package Client;

public class Main {
    public static void main(String[] args){
        if(args.length < 3){
            System.err.println("Not enough parameters");
            return;
        }
        Client client = new Client(args[0], Integer.parseInt(args[1]), args[2]);
        client.execute();
    }
}
