package server;

import model.Game;
import model.MessageWrapper;
import snakes.SnakesProto.*;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class Server {
    private Game game;
    private GameConfig gameConfig;
    private String gameName = "Game";
    private InetAddress addressGroup;
    private TimerTask announcementSender;
    private TimerTask updateGame;
    private ReceiverMsg receiverMsg;
    private AckHandler ackHandler;
    private PingHandler pingHandler;
    private Map<Integer, MessageWrapper> sentMessages = new HashMap<>();  // id player, message
    private Map<Integer, Long> recvMessages = new HashMap<>(); // id player, time
    final private int port = 9192;
    private int countPlayers = 0;
    private String playerName;
    private int playerId;
    private int seq = 0;
    private int stateDelay;
    //private long lastTimeSend;

    private DatagramSocket datagramSocket;
    private final List<DatagramPacket> steerMsgs = new ArrayList<>();

    public Server(Game game, String playerName) throws IOException{
        this.game = game;
        this.playerName = playerName;
        addressGroup = InetAddress.getByName("239.192.0.4");
        datagramSocket = new DatagramSocket(0);
    }

    public void startNewGame(){
        createGameConfig();
        playerId = countPlayers++;
        GamePlayer player = GamePlayer.newBuilder().setScore(0).setName(gameName).setId(playerId)
                .setRole(NodeRole.MASTER).setType(PlayerType.HUMAN).build();
        game.initGame(gameConfig, player);
        createAnnouncementSender();

        receiverMsg = new ReceiverMsg();
        Thread receiverMsgThread = new Thread(receiverMsg);
        receiverMsgThread.start();

        ackHandler = new AckHandler();
        Thread ackHandlerThread = new Thread(ackHandler);
        ackHandlerThread.start();

        pingHandler = new PingHandler();
        Thread pingHandlerThread = new Thread(pingHandler);
        pingHandlerThread.start();

        startStateSender();
    }

    //для DEPUTY когда он стал мастером
    public void startGame(){
        playerId = game.getIdPlayer();
        gameConfig = game.getGameConfig();
        stateDelay = gameConfig.getStateDelayMs();
        seq = game.getStateOrder();

        for(GamePlayer player : game.getPlayers().getPlayersList()){
            if(player.getId() == playerId)
                continue;
            synchronized (sentMessages) {
                try {
                    sentMessages.put(player.getId(), new MessageWrapper(null,
                            InetAddress.getByName(player.getIpAddress()), player.getPort()));
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            synchronized (recvMessages){
                recvMessages.put(player.getId(), System.currentTimeMillis());
            }
        }

        createAnnouncementSender();

        receiverMsg = new ReceiverMsg();
        Thread receiverMsgThread = new Thread(receiverMsg);
        receiverMsgThread.start();

        ackHandler = new AckHandler();
        Thread ackHandlerThread = new Thread(ackHandler);
        ackHandlerThread.start();

        pingHandler = new PingHandler();
        Thread pingHandlerThread = new Thread(pingHandler);
        pingHandlerThread.start();

        startStateSender();
    }

    public void endGame(){
        announcementSender.cancel();
        updateGame.cancel();
        receiverMsg.stop();
        ackHandler.stop();
        pingHandler.stop();
        game.endGame();
    }

    private void createGameConfig(){
        stateDelay = 600;
        gameConfig = GameConfig.newBuilder().setFoodStatic(3).setHeight(10)
                .setWidth(10).setStateDelayMs(stateDelay).build();
    }

    private void createAnnouncementSender(){
        announcementSender = new TimerTask() {
            @Override
            public void run() {
                GameAnnouncement gameAnnouncement = GameAnnouncement.newBuilder()
                        .setCanJoin(true).setConfig(gameConfig).setPlayers(game.getPlayers())
                        .setGameName(gameName).build();

                GameMessage.AnnouncementMsg announcementMsg = GameMessage.AnnouncementMsg.newBuilder()
                        .addGames(gameAnnouncement).build();

                GameMessage gameMessage = GameMessage.newBuilder().setAnnouncement(announcementMsg).setMsgSeq(seq).build();

                byte[] messageBytes = gameMessage.toByteArray();
                DatagramPacket datagramPacket = new DatagramPacket(messageBytes, messageBytes.length, addressGroup, port);
                try {
                    datagramSocket.send(datagramPacket);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        };

        Timer timer = new Timer();
        timer.schedule(announcementSender, 0, 1000);
    }

    private class ReceiverMsg implements Runnable{
        private boolean isRun = true;

        public void stop(){
            isRun = false;
        }

        @Override
        public void run() {
            while (isRun) {
                byte[] message = new byte[1024];
                DatagramPacket packet = new DatagramPacket(message, message.length);

                GameMessage gameMessage;
                try {
                    datagramSocket.receive(packet);
                    gameMessage = GameMessage.parseFrom(Arrays.copyOf(packet.getData(),packet.getLength()));
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }

                if (gameMessage.hasSteer()) {
                    try {
                        sendAck(packet.getAddress(), packet.getPort(), gameMessage.getMsgSeq(), gameMessage.getSenderId());
                    }catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    synchronized (recvMessages) {
                        recvMessages.put(gameMessage.getSenderId(), System.currentTimeMillis());
                    }
                    synchronized (steerMsgs) {
                        steerMsgs.add(packet);
                    }
                }

                if (gameMessage.hasJoin()) {
                    int newId = countPlayers++;
                    synchronized (sentMessages) {
                        sentMessages.put(newId, new MessageWrapper(gameMessage, packet.getAddress(), packet.getPort()));
                    }
                    try {
                        sendAck(packet.getAddress(), packet.getPort(), gameMessage.getMsgSeq(), newId);
                    }catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    synchronized (recvMessages) {
                        recvMessages.put(newId, System.currentTimeMillis());
                    }
                    GameMessage.JoinMsg joinMsg = gameMessage.getJoin();
                    NodeRole role = NodeRole.NORMAL;
                    if(countPlayers == 2){
                        role = NodeRole.DEPUTY;
                        try {
                            sendDeputyRole(packet.getAddress(), packet.getPort(), newId);
                        }catch (IOException e){
                            e.printStackTrace();
                            continue;
                        }
                    }
                    GamePlayer player = GamePlayer.newBuilder().setName(joinMsg.getPlayerName())
                            .setId(newId).setIpAddress(packet.getAddress().getHostAddress())
                            .setPort(packet.getPort()).setRole(role)
                            .setType(joinMsg.getPlayerType()).setScore(0).build();

                    game.joinGame(player);
                }

                if (gameMessage.hasPing()) {
                    synchronized (recvMessages) {
                        recvMessages.put(gameMessage.getSenderId(), System.currentTimeMillis());
                    }
                }

                if (gameMessage.hasAck()) {
                    synchronized (recvMessages) {
                        recvMessages.put(gameMessage.getSenderId(), System.currentTimeMillis());
                    }
                    /*
                    synchronized (sentMessages) {
                        sentMessages.removeIf(sentMessage ->
                                sentMessage.getIdPlayer() == gameMessage.getSenderId() &&
                                        sentMessage.getGameMessage().getMsgSeq() <= gameMessage.getMsgSeq());
                    }

                     */
                    synchronized (sentMessages) {
                        sentMessages.get(gameMessage.getSenderId()).setRecvAck(true);
                    }
                }
            }
        }
    }

    private void startStateSender(){
        updateGame = new TimerTask() {
            @Override
            public void run() {
                try {
                    synchronized (steerMsgs) {
                        game.updateGame(steerMsgs);
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
                synchronized (steerMsgs) {
                    steerMsgs.clear();
                }

                GameState.Builder gameState = GameState.newBuilder().setStateOrder(game.getStateOrder())
                        .setPlayers(game.getPlayers());

                for(int i = 0; i < game.getCountSnakes(); ++i){
                    gameState.addSnakes(game.getSnakes().get(i));
                }

                for(GameState.Coord food : game.getFood()){
                    gameState.addFoods(food);
                }

                GameMessage.StateMsg stateMsg = GameMessage.StateMsg.newBuilder().setState(gameState).build();
                GameMessage gameMessage = GameMessage.newBuilder().setState(stateMsg).setMsgSeq(game.getStateOrder()).build();

                byte[] messageBytes = gameMessage.toByteArray();
                synchronized (game.getPlayers().getPlayersList()) {
                    for (GamePlayer player : game.getPlayers().getPlayersList()) {
                        if (player.getId() == playerId)
                            continue;
                        try {
                            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length,
                                    InetAddress.getByName(player.getIpAddress()), player.getPort());
                            datagramSocket.send(packet);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        synchronized (sentMessages) {
                            sentMessages.get(player.getId()).setLastTimeSend(System.currentTimeMillis());
                            sentMessages.get(player.getId()).setRecvAck(false);
                            sentMessages.get(player.getId()).setGameMessage(gameMessage);
                        }
                    }
                }
                seq++;
            }
        };

        Timer timer = new Timer();
        timer.schedule(updateGame, 0, stateDelay);
    }

    private void sendAck(InetAddress address, int port, long ms, int id) throws IOException{
        GameMessage.AckMsg ackMsg = GameMessage.AckMsg.newBuilder().build();
        GameMessage gameMessage = GameMessage.newBuilder().setAck(ackMsg).setMsgSeq(ms).setReceiverId(id).build();
        byte[] messageBytes = gameMessage.toByteArray();
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length,
                address, port);
        datagramSocket.send(packet);
        synchronized (sentMessages) {
            sentMessages.get(id).setLastTimeSend(System.currentTimeMillis());
        }
    }

    private class AckHandler implements Runnable{
        private boolean isRun = true;

        public void stop(){
            isRun = false;
        }

        @Override
        public void run(){
            while (isRun){
                Map<Integer, MessageWrapper> newSentMessages = new HashMap<>();
                synchronized (sentMessages) {
                    for(Map.Entry<Integer, MessageWrapper> entry : sentMessages.entrySet()){
                        newSentMessages.put(entry.getKey(), entry.getValue());
                        if(System.currentTimeMillis() - entry.getValue().getLastTimeSend() > stateDelay / 10){
                            // отправляем ping
                            GameMessage.PingMsg pingMsg = GameMessage.PingMsg.newBuilder().build();
                            GameMessage gameMessage = GameMessage.newBuilder().setPing(pingMsg).setMsgSeq(game.getStateOrder())
                                    .setSenderId(playerId).build();
                            byte[] messageBytes = gameMessage.toByteArray();
                            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length,
                                    entry.getValue().getAddress(), entry.getValue().getPort());
                            try {
                                datagramSocket.send(packet);
                            }catch (IOException e){
                                e.printStackTrace();
                            }

                            // переотправляем сообщение если без ack
                            if(!entry.getValue().isRecvAck()){
                                resendMessage(entry.getValue());
                            }
                            newSentMessages.get(entry.getKey()).setLastTimeSend(System.currentTimeMillis());
                        }
                    }
                    sentMessages.clear();
                    sentMessages.putAll(newSentMessages);
                }

            }
        }
    }

    private class PingHandler implements Runnable{
        private boolean isRun = true;

        public void stop(){
            isRun = false;
        }

        @Override
        public void run(){
            while (isRun) {
                Map<Integer, Long> newRecvMessages = new HashMap<>();
                synchronized (recvMessages) {
                    for (Map.Entry<Integer, Long> entry : recvMessages.entrySet()) {
                        if (System.currentTimeMillis() - entry.getValue() > 0.8 * stateDelay) {
                            if (game.getPlayer(entry.getKey()).getRole() == NodeRole.DEPUTY) {
                                addNewDeputy();
                            }
                            game.removePlayer(entry.getKey());
                            countPlayers--;
                            synchronized (sentMessages) {
                                sentMessages.remove(entry.getKey());
                            }
                        }
                        else{
                            newRecvMessages.put(entry.getKey(), entry.getValue());
                        }
                    }
                    recvMessages.clear();
                    recvMessages.putAll(newRecvMessages);
                }
            }
        }
    }

    private void resendMessage(MessageWrapper messageWrapper){
        byte[] messageBytes = messageWrapper.getGameMessage().toByteArray();
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length,
                messageWrapper.getAddress(), messageWrapper.getPort());
        try {
            datagramSocket.send(packet);
        }catch (IOException e){
            e.printStackTrace();
        }
        //messageWrapper.setLastTimeSend(System.currentTimeMillis());
    }

    void addNewDeputy(){
        for(GamePlayer player : game.getPlayers().getPlayersList()){
            if(player.getId() != playerId){
                GamePlayer newPlayer = GamePlayer.newBuilder().setName(player.getName())
                        .setId(player.getId()).setIpAddress(player.getIpAddress())
                        .setPort(player.getPort()).setRole(NodeRole.DEPUTY)
                        .setType(player.getType()).setScore(0).build();
                game.removePlayer(player.getId());
                game.addPlayer(newPlayer);
                try {
                    sendDeputyRole(InetAddress.getByName(player.getIpAddress()), player.getPort(), player.getId());
                }catch (IOException e){
                    e.printStackTrace();

                }
            }
        }
    }

    void sendDeputyRole(InetAddress address, int port, int id) throws IOException{
        GameMessage.RoleChangeMsg roleChangeMsg = GameMessage.RoleChangeMsg.newBuilder()
                .setReceiverRole(NodeRole.DEPUTY).build();
        GameMessage gameMessage = GameMessage.newBuilder().setRoleChange(roleChangeMsg).setReceiverId(id).setMsgSeq(seq).build();
        byte[] messageBytes = gameMessage.toByteArray();
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, address, port);
        datagramSocket.send(packet);
        synchronized (sentMessages) {
            //sentMessages.add((new MessageWrapper(gameMessage, address, port, id)));
            sentMessages.get(id).setLastTimeSend(System.currentTimeMillis());
            sentMessages.get(id).setRecvAck(false);
            sentMessages.get(id).setGameMessage(gameMessage);
        }
    }
}
