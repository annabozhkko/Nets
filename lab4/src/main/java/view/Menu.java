package view;

import controller.Controller;
import model.NetModel;

import javax.swing.*;
import java.io.IOException;

public class Menu extends JMenu {
    public Menu(Controller controller, NetModel netModel){
        super("Menu");
        JMenuItem newGameItem = new JMenuItem("New game");
        newGameItem.addActionListener(e -> controller.execute("NEW_GAME"));
        this.add(newGameItem);

        JMenuItem joinGameItem = new JMenuItem("Join game");
        joinGameItem.addActionListener(e -> {
            try {
                netModel.startClient();
            }catch (IOException exp){
                exp.printStackTrace();
                return;
            }
            new ListGamesFrame(netModel, controller);
        });
        this.add(joinGameItem);

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> controller.execute("EXIT"));
        this.add(exitItem);
    }
}
