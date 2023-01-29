package view;

import controller.Controller;
import model.NetModel;
import snakes.SnakesProto.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.net.InetSocketAddress;

public class GameButton extends JButton {
    public GameButton(Controller controller, GameAnnouncement gameAnnouncement, InetSocketAddress address, NetModel netModel){
        super(gameAnnouncement.getGameName());
        addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                netModel.setCurrentGame(gameAnnouncement);
                netModel.setServerAddress(address);
                controller.execute("JOIN_GAME");
            }
        });
    }
}
