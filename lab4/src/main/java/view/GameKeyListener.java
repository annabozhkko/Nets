package view;

import controller.Controller;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class GameKeyListener implements KeyListener {
    final private Controller controller;

    public GameKeyListener(Controller controller){
        this.controller = controller;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if(key ==  KeyEvent.VK_UP)
            controller.execute("UP");

        if(key ==  KeyEvent.VK_DOWN)
            controller.execute("DOWN");

        if(key ==  KeyEvent.VK_RIGHT)
            controller.execute("RIGHT");

        if(key ==  KeyEvent.VK_LEFT)
            controller.execute("LEFT");
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
