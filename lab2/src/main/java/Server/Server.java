package Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private final int PORT;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final AtomicInteger countClients = new AtomicInteger(0);

    public Server(int PORT){
        this.PORT = PORT;
    }

    public void execute() throws IOException{
        ServerSocket serverSocket = new ServerSocket(PORT);

        while (!serverSocket.isClosed()){
            Socket connectionSocket = serverSocket.accept();
            threadPool.submit(() -> downloadFile(connectionSocket));
        }
    }

    private void downloadFile(Socket connectionSocket){
        try (Socket s = connectionSocket) {
            DataInputStream dataInputStream = new DataInputStream(connectionSocket.getInputStream());
            PrintWriter outputStream = new PrintWriter(connectionSocket.getOutputStream(), true);

            int numberClient = countClients.addAndGet(1);
            long bytes = 0;
            long allBytes = 0;
            long time = System.currentTimeMillis();
            long lastTime = System.currentTimeMillis();

            String filename = dataInputStream.readUTF();
            long sizeFile = dataInputStream.readLong();

            File file = new File("uploads/" + Paths.get(filename).getFileName());
            file.createNewFile();

            byte buffer[] = new byte[1000];
            int bufferSize;
            try(FileOutputStream fileOutputStream = new FileOutputStream(file)){
                while (sizeFile > 0){
                    bufferSize = dataInputStream.read(buffer, 0, (1000 < sizeFile) ? 1000 : (int)sizeFile);
                    fileOutputStream.write(buffer, 0, bufferSize);
                    sizeFile -= bufferSize;
                    bytes += bufferSize;
                    allBytes += bufferSize;
                    if(System.currentTimeMillis() - lastTime >= 3000){
                        System.out.println("Client " + numberClient + ": " + (bytes / (System.currentTimeMillis() - lastTime) * 1000)
                                + " bytes/sec, average speed: " + (allBytes / (System.currentTimeMillis() - time) * 1000) + " bytes/sec");
                        bytes = 0;
                        lastTime = System.currentTimeMillis();
                    }
                }
            }
            System.out.println("Client " + numberClient + ": " + (bytes / (System.currentTimeMillis() - lastTime) * 1000)
                    + " bytes/sec, average speed: " + (allBytes / (System.currentTimeMillis() - time) * 1000) + " bytes/sec");

            outputStream.println("File successfully downloaded");
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
