package view;

import controller.Controller;
import model.Game;
import model.NetModel;

import javax.swing.*;
import java.awt.*;

public class GUI extends JFrame {
    public GUI(Game game, NetModel netModel, Controller controller){
        this.setTitle("Snake");
        this.setSize(800, 800);
        this.setBackground(Color.gray);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        Menu menu = new Menu(controller, netModel);
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(menu);
        this.setJMenuBar(menuBar);

        GameFieldPanel gameFieldPanel = new GameFieldPanel(game);
        GameKeyListener gameKeyListener = new GameKeyListener(controller);
        this.add(gameFieldPanel);
        this.addKeyListener(gameKeyListener);

        this.setVisible(true);
    }
}
