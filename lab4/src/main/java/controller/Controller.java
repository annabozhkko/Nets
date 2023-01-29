package controller;

import controller.commands.*;
import model.Game;
import model.NetModel;

import java.util.HashMap;
import java.util.Map;

public class Controller {
    private Game game;
    private NetModel netModel;
    final private Map<String, Command> commands = new HashMap<>();

    public Controller(Game game, NetModel netModel){
        this.game = game;
        this.netModel = netModel;
        createCommands();
    }

    private void createCommands(){
        commands.put("UP", new Up(game, netModel));
        commands.put("DOWN", new Down(game, netModel));
        commands.put("RIGHT", new Right(game, netModel));
        commands.put("LEFT", new Left(game, netModel));
        commands.put("NEW_GAME", new NewGame(netModel));
        commands.put("JOIN_GAME", new JoinGame(netModel));
        commands.put("EXIT", new Exit());
    }

    public void execute(String command){
        commands.get(command).execute();
    }
}
