package controller.commands;

import model.NetModel;

import java.io.IOException;

public class JoinGame implements Command{
    final private NetModel netModel;

    public JoinGame(NetModel netModel){
        this.netModel = netModel;
    }

    public void execute(){
        try {
            netModel.joinGame(netModel.getCurrentGame(), netModel.getServerAddress());
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
