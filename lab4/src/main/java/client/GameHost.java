package client;

import snakes.SnakesProto.*;

import java.net.InetAddress;

public class GameHost {
    private InetAddress address;
    private int port;
    private long time;
    private GameMessage.AnnouncementMsg announcementMsg;

    public GameHost(InetAddress address, int port, GameMessage.AnnouncementMsg announcementMsg){
        this.address = address;
        this.port = port;
        this.announcementMsg = announcementMsg;
        time = System.currentTimeMillis();
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public long getTime() {
        return time;
    }

    public GameMessage.AnnouncementMsg getAnnouncementMsg() {
        return announcementMsg;
    }

    public void updateTime(){
        time = System.currentTimeMillis();
    }
}
