package controller.commands;

import model.Game;
import model.NetModel;
import snakes.SnakesProto.*;

import java.io.IOException;

public class Left implements Command{
    final private Game game;
    final private NetModel netModel;

    public Left(Game game, NetModel netModel){
        this.game = game;
        this.netModel = netModel;
    }

    public void execute(){
        if(game.getRole() == NodeRole.MASTER) {
            game.setDirectionMaster(Direction.LEFT);
        }
        if(game.getRole() == NodeRole.NORMAL || game.getRole() == NodeRole.DEPUTY){
            try {
                netModel.getClient().sendSteerMsg(Direction.LEFT);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
