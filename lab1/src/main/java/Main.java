import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

public class Main {
    public static void main(String[] args){
        if(args.length < 1){
            System.out.println("Not enough parameters\n");
            return;
        }

        MulticastSocket multicastSocket;
        InetAddress addressGroup;

        try {
            NetworkInterface netIf = NetworkInterface.getByInetAddress(InetAddress.getByName(""));
            addressGroup = InetAddress.getByName(args[0]);
            InetSocketAddress inetSocketAddress = new InetSocketAddress(addressGroup, 8080);
            multicastSocket = new MulticastSocket(8000);
            multicastSocket.joinGroup(inetSocketAddress, netIf);
        }catch (IOException e){
            e.printStackTrace();
            return;
        }

        Thread senderThread = new Thread(new Sender(multicastSocket, addressGroup));
        Thread receiverThread = new Thread(new Receiver(multicastSocket));
        senderThread.start();
        receiverThread.start();
    }
}
