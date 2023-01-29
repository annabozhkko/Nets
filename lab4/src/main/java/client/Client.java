package client;

import common.Publisher;
import model.Game;
import model.MessageWrapper;
import snakes.SnakesProto.*;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class Client extends Publisher {
    private Game game;
    private MulticastSocket multicastSocket;
    private DatagramSocket datagramSocket;
    private InetAddress addressGroup;
    private String playerName;
    private Map<InetSocketAddress, GameHost> gameHosts = new HashMap<>();
    private Map<InetSocketAddress, GameAnnouncement> games = new HashMap<>(); // список игр для отображения
    private InetSocketAddress serverAddress;
    private TimerTask updateListGames;
    private List<MessageWrapper> sentMessages = new ArrayList<>();
    private ReceiverMsg receiverMsg;
    private AckHandler ackHandler;
    private PingSender pingSender;
    private AnnouncementReceiver announcementReceiver;
    private int stateDelay;
    private NodeRole role = NodeRole.NORMAL;
    private boolean isStop = false;
    private int playerId = 0;
    private long lastTimeSend;

    private final int MAX_TIME_DELAY = 10000;

    public Client(Game game, String playerName) throws IOException {
        this.game = game;
        this.playerName = playerName;
        joinMulticastGroup();

        announcementReceiver = new AnnouncementReceiver();
        Thread announcementReceiverThread = new Thread(announcementReceiver);
        announcementReceiverThread.start();

        updateListGames = new TimerTask() {
            @Override
            public void run() {
                games.clear();
                for(Map.Entry<InetSocketAddress, GameHost> entry : gameHosts.entrySet()){
                    GameHost gameHost = entry.getValue();
                    if(System.currentTimeMillis() - gameHost.getTime() < MAX_TIME_DELAY){
                        for(GameAnnouncement gameAnnouncement : gameHost.getAnnouncementMsg().getGamesList()){
                            games.put(entry.getKey(), gameAnnouncement);
                        }
                    }
                }
                notifySubscribers();
            }
        };

        Timer timer = new Timer();
        timer.schedule(updateListGames, 0, 3000);
    }

    private void endGame(){
        announcementReceiver.stop();
        receiverMsg.stop();
        pingSender.stop();
        ackHandler.stop();
        updateListGames.cancel();
    }

    public void joinGame(GameAnnouncement gameAnnouncement, InetSocketAddress address) throws IOException{
        stateDelay = gameAnnouncement.getConfig().getStateDelayMs();
        game.joinGame(gameAnnouncement);
        serverAddress  = address;
        sendJoinMsg(gameAnnouncement.getGameName());

        receiverMsg = new ReceiverMsg();
        Thread receiverMsgThread = new Thread(receiverMsg);
        receiverMsgThread.start();

        ackHandler = new AckHandler();
        Thread ackHandlerThread = new Thread(ackHandler);
        ackHandlerThread.start();

        pingSender = new PingSender();
        Thread pingSenderThread = new Thread(pingSender);
        pingSenderThread.start();
    }

    private void joinMulticastGroup() throws IOException {
        final String host = "10.9.77.14";
        SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(host), 8080);
        datagramSocket = new DatagramSocket(socketAddress);

        NetworkInterface netIf = NetworkInterface.getByInetAddress(InetAddress.getByName(host));
        addressGroup = InetAddress.getByName("239.192.0.4");
        InetSocketAddress inetSocketAddress = new InetSocketAddress(addressGroup, 9192);
        multicastSocket = new MulticastSocket(9192);
        multicastSocket.joinGroup(inetSocketAddress, netIf);
    }

    private void sendJoinMsg(String gameName) throws IOException{
        GameMessage.JoinMsg joinMsg = GameMessage.JoinMsg.newBuilder().setPlayerType(PlayerType.HUMAN)
                .setPlayerName(playerName).setGameName(gameName).setRequestedRole(NodeRole.NORMAL).build();

        GameMessage gameMessage = GameMessage.newBuilder().setJoin(joinMsg).setMsgSeq(game.getStateOrder()).build();
        byte[] messageBytes = gameMessage.toByteArray();
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, serverAddress.getAddress(),
                serverAddress.getPort());
        datagramSocket.send(packet);
        lastTimeSend = System.currentTimeMillis();
        sentMessages.add(new MessageWrapper(gameMessage, serverAddress.getAddress(), serverAddress.getPort()));
    }

    private class AnnouncementReceiver implements Runnable{
        private boolean isRun = true;

        public void stop(){
            isRun = false;
        }

        @Override
        public void run(){
            while(isRun){
                byte[] message = new byte[1024];
                DatagramPacket packet = new DatagramPacket(message, message.length);

                GameMessage gameMessage;
                try {
                    multicastSocket.receive(packet);
                    gameMessage = GameMessage.parseFrom(Arrays.copyOf(packet.getData(), packet.getLength()));
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }

                if(gameMessage.hasAnnouncement()){
                    for(int i = 0; i < gameMessage.getAnnouncement().getGamesCount(); ++i) {
                        GameHost gameHost = new GameHost(packet.getAddress(), packet.getPort(), gameMessage.getAnnouncement());
                        gameHosts.put(new InetSocketAddress(packet.getAddress(), packet.getPort()), gameHost);
                    }
                }

            }
        }
    }

    private class ReceiverMsg implements Runnable{
        private boolean isRun = true;

        public void stop(){
            isRun = false;
        }

        @Override
        public void run(){
            while(isRun) {
                byte[] message = new byte[1024];
                DatagramPacket packet = new DatagramPacket(message, message.length);

                try {
                    datagramSocket.setSoTimeout((int)(0.8 * stateDelay));
                }catch (IOException e){
                    continue;
                }

                GameMessage gameMessage;
                try {
                    datagramSocket.receive(packet);
                    gameMessage = GameMessage.parseFrom(Arrays.copyOf(packet.getData(), packet.getLength()));
                } catch (SocketTimeoutException e) {
                    changeServer();
                    continue;
                }catch (IOException e){
                    e.printStackTrace();
                    continue;
                }

                if(gameMessage.hasState()){
                    try {
                        sendAck(packet.getAddress(), packet.getPort(), gameMessage.getMsgSeq());
                    }catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    game.updateGame(gameMessage.getState().getState());
                }

                if(gameMessage.hasAck()){
                    if(playerId == 0){
                        playerId = gameMessage.getReceiverId();
                        game.setIdPlayer(playerId);
                    }
                    synchronized (sentMessages) {
                        sentMessages.removeIf(sentMessage ->
                                sentMessage.getGameMessage().getMsgSeq() <= gameMessage.getMsgSeq());
                    }
                }

                if(gameMessage.hasRoleChange()){
                    try {
                        sendAck(packet.getAddress(), packet.getPort(), gameMessage.getMsgSeq());
                    }catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if(gameMessage.getRoleChange().getReceiverRole() == NodeRole.DEPUTY){
                        role = NodeRole.DEPUTY;
                    }
                }
            }
        }
    }

    public void sendSteerMsg(Direction direction) throws IOException{
        GameMessage.SteerMsg steerMsg = GameMessage.SteerMsg.newBuilder().setDirection(direction).build();
        GameMessage gameMessage = GameMessage.newBuilder().setSteer(steerMsg).setMsgSeq(game.getStateOrder())
                .setSenderId(playerId).build();
        byte[] messageBytes = gameMessage.toByteArray();
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, serverAddress.getAddress(),
                serverAddress.getPort());
        datagramSocket.send(packet);
        lastTimeSend = System.currentTimeMillis();
        synchronized (sentMessages) {
            sentMessages.add(new MessageWrapper(gameMessage, serverAddress.getAddress(), serverAddress.getPort()));
        }
    }

    private void sendAck(InetAddress address, int port, long ms) throws IOException{
        GameMessage.AckMsg ackMsg = GameMessage.AckMsg.newBuilder().build();
        GameMessage gameMessage = GameMessage.newBuilder().setAck(ackMsg).setMsgSeq(ms).setSenderId(playerId).build();
        byte[] messageBytes = gameMessage.toByteArray();
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length,
                address, port);
        datagramSocket.send(packet);
        lastTimeSend = System.currentTimeMillis();
    }

    public Map<InetSocketAddress,GameAnnouncement> getGames() {
        return games;
    }

    private class AckHandler implements Runnable{
        private boolean isRun = true;

        public void stop(){
            isRun = false;
        }

        @Override
        public void run(){
            while (isRun){
                List<MessageWrapper> newSentMessages = new ArrayList<>();
                synchronized (sentMessages) {
                    for (MessageWrapper sentMessage : sentMessages) {
                        if (System.currentTimeMillis() - sentMessage.getLastTimeSend() > stateDelay / 10) {
                            newSentMessages.add(resendMessage(sentMessage));
                        } else {
                            newSentMessages.add(sentMessage);
                        }
                    }
                    sentMessages.clear();
                    sentMessages.addAll(newSentMessages);
                }
            }
        }
    }

    private class PingSender implements Runnable{
        private boolean isRun = true;

        public void stop(){
            isRun = false;
        }

        @Override
        public void run(){
            while (isRun){
                if(System.currentTimeMillis() - lastTimeSend >= stateDelay / 10){
                    GameMessage.PingMsg pingMsg = GameMessage.PingMsg.newBuilder().build();
                    GameMessage gameMessage = GameMessage.newBuilder().setPing(pingMsg).setMsgSeq(game.getStateOrder())
                            .setSenderId(playerId).build();
                    byte[] messageBytes = gameMessage.toByteArray();
                    DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length,
                            serverAddress.getAddress(), serverAddress.getPort());
                    try {
                        datagramSocket.send(packet);
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                    lastTimeSend = System.currentTimeMillis();
                }
            }
        }
    }

    private MessageWrapper resendMessage(MessageWrapper messageHandler){
        byte[] messageBytes = messageHandler.getGameMessage().toByteArray();
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length,
                messageHandler.getAddress(), messageHandler.getPort());
        try {
            datagramSocket.send(packet);
        }catch (IOException e){
            e.printStackTrace();
        }
        lastTimeSend = System.currentTimeMillis();
        return new MessageWrapper(messageHandler.getGameMessage(),
                messageHandler.getAddress(), messageHandler.getPort());
    }

    private void changeServer(){
        for(GamePlayer player : game.getPlayers().getPlayersList()){
            if(player.getRole() == NodeRole.MASTER){
                game.removePlayer(player.getId());
            }
            if(player.getRole() == NodeRole.DEPUTY) {
                serverAddress = new InetSocketAddress(player.getIpAddress(), player.getPort());
            }
        }
        if(role == NodeRole.DEPUTY){
            game.setRole(NodeRole.MASTER);
            endGame();
            isStop = true;
            notifySubscribers();
        }
    }

    public boolean isStop() {
        return isStop;
    }
}
