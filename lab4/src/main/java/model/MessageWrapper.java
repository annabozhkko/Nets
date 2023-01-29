package model;

import snakes.SnakesProto.*;

import java.net.InetAddress;

public class MessageWrapper {
    private GameMessage gameMessage;
    private InetAddress address;
    private int port;
    private long lastTimeSend;
    private boolean isRecvAck = false;

    public MessageWrapper(GameMessage gameMessage, InetAddress address, int port){
        this.gameMessage = gameMessage;
        this.address = address;
        this.port = port;
        lastTimeSend = System.currentTimeMillis();
    }

    public long getLastTimeSend() {
        return lastTimeSend;
    }

    public void setLastTimeSend(long lastTimeSend) {
        this.lastTimeSend = lastTimeSend;
    }

    public int getPort() {
        return port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public GameMessage getGameMessage() {
        return gameMessage;
    }

    public void setGameMessage(GameMessage gameMessage) {
        this.gameMessage = gameMessage;
    }

    public void setRecvAck(boolean isRecvAck){
        this.isRecvAck = isRecvAck;
    }

    public boolean isRecvAck() {
        return isRecvAck;
    }
}
