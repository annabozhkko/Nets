package controller.commands;

import model.NetModel;

import java.io.IOException;

public class NewGame implements Command{
    final private NetModel netModel;

    public NewGame(NetModel netModel){
        this.netModel = netModel;
    }

    public void execute(){
        try {
            netModel.createNewGame();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
