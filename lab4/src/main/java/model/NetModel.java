package model;

import client.Client;
import common.Publisher;
import common.Subscriber;
import server.Server;
import snakes.SnakesProto.*;

import java.io.IOException;
import java.net.InetSocketAddress;

public class NetModel extends Publisher implements Subscriber{
    private Client client;
    private Server server;
    private Game game;
    private String playerName;
    private InetSocketAddress serverAddress;
    private GameAnnouncement currentGame;

    public NetModel(Game game, String playerName){
        this.game = game;
        this.playerName = playerName;
    }

    public void startClient() throws IOException{
        client = new Client(game, playerName);
        client.addSubscriber(this);
    }

    public void joinGame(GameAnnouncement gameAnnouncement, InetSocketAddress address) throws IOException {
        client.joinGame(gameAnnouncement, address);
    }

    public void createNewGame() throws IOException{
        if(server == null)
            server = new Server(game, playerName);
        else
            server.endGame();

        server.startNewGame();
    }

    public Client getClient() {
        return client;
    }

    public void setCurrentGame(GameAnnouncement currentGame) {
        this.currentGame = currentGame;
    }

    public void setServerAddress(InetSocketAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    public GameAnnouncement getCurrentGame() {
        return currentGame;
    }

    public InetSocketAddress getServerAddress() {
        return serverAddress;
    }

    @Override
    public void update() {
        if(client.isStop()){
            try {
                server = new Server(game, playerName);
            }catch (IOException e){
                e.printStackTrace();
                return;
            }
            server.startGame();
        }
        notifySubscribers();
    }
}
