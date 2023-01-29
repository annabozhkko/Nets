package view;

import common.Subscriber;
import controller.Controller;
import model.NetModel;
import snakes.SnakesProto;

import javax.swing.*;
import java.net.InetSocketAddress;
import java.util.Map;

public class ListGamesFrame extends JFrame implements Subscriber {
    private NetModel netModel;
    private Controller controller;
    private JPanel panel = new JPanel();

    public ListGamesFrame(NetModel netModel, Controller controller){
        this.netModel = netModel;
        this.controller = controller;
        netModel.addSubscriber(this);
        setSize(300, 400);
    }

    @Override
    public void update() {
        panel.removeAll();

        Map<InetSocketAddress, SnakesProto.GameAnnouncement> games = netModel.getClient().getGames();
        for(Map.Entry<InetSocketAddress, SnakesProto.GameAnnouncement> entry : games.entrySet()){
            panel.add(new GameButton(controller, entry.getValue(), entry.getKey(), netModel));
        }

        this.add(panel);
        setVisible(true);
    }
}
