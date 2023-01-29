package view;

import common.Subscriber;
import model.Field;
import model.Game;

import javax.swing.*;
import java.awt.*;

public class GameFieldPanel extends JPanel implements Subscriber {
    private Game game;
    private Image snake, food;

    public GameFieldPanel(Game game){
        game.addSubscriber(this);
        this.game = game;
        loadImages();
    }

    private void loadImages(){
        snake = new ImageIcon("src/main/resources/snake.png").getImage();
        food = new ImageIcon("src/main/resources/food.png").getImage();
    }

    public void paint(Graphics g){
        Graphics2D graphics2D = (Graphics2D) g;
        Field field = game.getField();
        if(field == null)
            return;

        for(int x = 0; x < field.getWidth(); ++x){
            for(int y = 0; y < field.getHeight(); ++y){
                if(field.getBlocks()[x][y].isFood()){
                    graphics2D.drawImage(food, x * 80, y * 80, 80, 80, this);
                }
                if(field.getBlocks()[x][y].isSnake()){
                    graphics2D.drawImage(snake, x * 80, y * 80, 80, 80, this);
                }
            }
        }
    }

    public void update(){
        repaint();
    }
}
