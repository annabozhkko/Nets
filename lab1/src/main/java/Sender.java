import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;

public class Sender implements Runnable{
    private MulticastSocket multicastSocket;
    private InetAddress addressGroup;
    private String message = "Hello, it's me!";

    public Sender(MulticastSocket multicastSocket, InetAddress addressGroup){
        this.multicastSocket = multicastSocket;
        this.addressGroup = addressGroup;
    }

    @Override
    public void run() {
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket sendPacket = new DatagramPacket(messageBytes, messageBytes.length, addressGroup, 8080);
        while (true){
            try {
                multicastSocket.send(sendPacket);
                Thread.sleep(2000);
            }catch (IOException | InterruptedException e){
                e.printStackTrace();
            }
        }
    }
}
