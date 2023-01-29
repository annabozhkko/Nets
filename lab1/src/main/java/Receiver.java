import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Receiver implements Runnable{
    private MulticastSocket multicastSocket;
    private String message = "Hello, it's me!";
    private Map<InetSocketAddress, Long> copies = new HashMap<>();
    private final int MAX_TIME_DELAY = 10000;

    public Receiver(MulticastSocket multicastSocket){
        this.multicastSocket = multicastSocket;
    }

    @Override
    public void run() {
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] buf = new byte[messageBytes.length];
        DatagramPacket recvPacket = new DatagramPacket(buf, buf.length);

        try {
            multicastSocket.setSoTimeout(2000);
        }catch (SocketException e){
            e.printStackTrace();
            return;
        }

        while (true){
            try {
                multicastSocket.receive(recvPacket);
            }catch (IOException e){
                checkCopies();
                continue;
            }

            String recvMessage = new String(buf, StandardCharsets.UTF_8);
            if(recvMessage.equals(message)){
                Long prevTime = copies.put(new InetSocketAddress(recvPacket.getAddress(), recvPacket.getPort()), System.currentTimeMillis());
                if(prevTime == null){
                    printCopies();
                }
            }

            checkCopies();
        }
    }

    private void checkCopies(){
        if (copies.entrySet().removeIf(entry ->
                System.currentTimeMillis() - entry.getValue() > MAX_TIME_DELAY)) {
            printCopies();
        }
    }

    private void printCopies(){
        System.out.println("List copies: ");
        for(InetSocketAddress address: copies.keySet()){
            System.out.println(address);
        }
        System.out.println("\n");
    }

}
