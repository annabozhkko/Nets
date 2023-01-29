package Client;

import java.io.*;
import java.net.Socket;
import java.nio.file.Paths;

public class Client {
    private final int PORT;
    private final String address;
    private final String filepath;

    public Client(String filepath, int PORT, String address) {
        this.PORT = PORT;
        this.address = address;
        this.filepath = filepath;
    }

    public void execute(){
        try(Socket socket = new Socket(address, PORT)) {
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            File file = new File(filepath);
            long sizeFile = file.length();

            dataOutputStream.writeUTF(String.valueOf(Paths.get(filepath).getFileName()));
            dataOutputStream.writeLong(sizeFile);

            byte buffer[] = new byte[1000];
            int bufferSize;
            try(FileInputStream fileInputStream = new FileInputStream(filepath)) {
                while ((bufferSize = fileInputStream.read(buffer, 0, 1000)) > 0) {
                    dataOutputStream.write(buffer, 0, bufferSize);
                }
            }

            System.out.println(inputStream.readLine());
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
