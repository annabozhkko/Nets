import controller.Controller;
import model.Game;
import model.NetModel;
import view.GUI;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Game game = new Game();
        NetModel netModel = new NetModel(game, args[0]);
        Controller controller = new Controller(game, netModel);
        GUI gui = new GUI(game, netModel, controller);
    }
}
