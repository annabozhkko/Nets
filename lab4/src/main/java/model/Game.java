package model;

import common.Publisher;
import snakes.SnakesProto;
import snakes.SnakesProto.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.*;

public class Game extends Publisher {
    private Field field;
    final private List<GameState.Snake> snakes = new ArrayList<>();
    final private List<GamePlayer> players = new ArrayList<>();
    final private List<GameState.Coord> food = new ArrayList<>();
    private GameConfig gameConfig;
    private int width;
    private int height;
    private int foodStatic;
    //private int countFood;
    private NodeRole role;
    private int idPlayer;
    private int stateOrder = 0;
    private String gameName;
    private int Score;

    //server
    public void initGame(GameConfig gameConfig, GamePlayer player){
        this.gameConfig = gameConfig;
        width = gameConfig.getWidth();
        height = gameConfig.getHeight();
        foodStatic = gameConfig.getFoodStatic();
        field = new Field(width, height);
        idPlayer = player.getId();
        createSnake();
        updateFood();
        players.add(player);
        role = NodeRole.MASTER;
    }

    public void endGame(){
        snakes.clear();
        food.clear();
        players.clear();
        //countFood = 0;
        stateOrder = 0;
    }

    //server
    public void joinGame(GamePlayer player){
        synchronized (players) {
            players.add(player);
        }
        List<GameState.Coord> coords = findPlaceForSnake();
        GameState.Snake snake = GameState.Snake.newBuilder().setPlayerId(player.getId())
                .addPoints(coords.get(0)).addPoints(coords.get(1)).setState(GameState.Snake.SnakeState.ALIVE)
                .setHeadDirection(SnakesProto.Direction.UP).build();

        synchronized (snakes) {
            addSnake(snake);
        }
    }

    //client
    public void joinGame(GameAnnouncement gameAnnouncement){
        //players
        players.clear();
        for(GamePlayer player : gameAnnouncement.getPlayers().getPlayersList()){
            players.add(player);
        }

        //game config
        gameConfig = gameAnnouncement.getConfig();
        width = gameConfig.getWidth();
        height = gameConfig.getHeight();
        foodStatic = gameConfig.getFoodStatic();
        field = new Field(width, height);

        //game name
        gameName = gameAnnouncement.getGameName();
        role = NodeRole.NORMAL;
    }

    //server
    public void updateGame(List <DatagramPacket> packets) throws IOException {
        for(DatagramPacket packet : packets){
            String address = packet.getAddress().getHostAddress();
            GameMessage gameMessage = GameMessage.parseFrom(Arrays.copyOf(packet.getData(),packet.getLength()));
            GameMessage.SteerMsg steerMsg = gameMessage.getSteer();

            int idPlayer = 0;
            synchronized (players) {
                for (GamePlayer player : players) {
                    if ((player.getIpAddress().equals(address)) && (player.getPort() == packet.getPort())) {
                        idPlayer = player.getId();
                        break;
                    }
                }
            }

            synchronized (snakes) {
                for (GameState.Snake snake : snakes) {
                    if (snake.getPlayerId() == idPlayer) {
                        GameState.Snake.Builder newSnakeBuilder = GameState.Snake.newBuilder().setState(snake.getState())
                                .setPlayerId(idPlayer).setHeadDirection(steerMsg.getDirection());
                        for (int i = 0; i < snake.getPointsCount(); ++i) {
                            newSnakeBuilder.addPoints(snake.getPoints(i));
                        }
                        snakes.remove(snake);
                        snakes.add(newSnakeBuilder.build());
                        break;
                    }
                }
            }
        }

        List<GameState.Snake> newSnakes = new ArrayList<>();
        synchronized (snakes) {
            for (int i = 0; i < snakes.size(); ++i) {
                GameState.Snake snake = moveSnake(snakes.get(i));
                if (snake != null)
                    newSnakes.add(snake);
            }

            snakes.clear();
            snakes.addAll(newSnakes);
        }

        stateOrder++;
        notifySubscribers();
    }

    private GameState.Snake moveSnake(GameState.Snake snake){
        GameState.Snake.Builder newSnake = GameState.Snake.newBuilder().setState(GameState.Snake.SnakeState.ALIVE)
                .setPlayerId(snake.getPlayerId()).setHeadDirection(snake.getHeadDirection());

        int headX = 0, headY = 0;
        switch (snake.getHeadDirection()){
            case UP:
                headX = snake.getPoints(0).getX();
                headY = (snake.getPoints(0).getY() == 0) ? height - 1 : snake.getPoints(0).getY() - 1;

                break;
            case DOWN:
                headX = snake.getPoints(0).getX();
                headY = (snake.getPoints(0).getY() == height - 1) ? 0 : snake.getPoints(0).getY() + 1;

                break;
            case RIGHT:
                headX = (snake.getPoints(0).getX() == width - 1) ? 0 : snake.getPoints(0).getX() + 1;
                headY = snake.getPoints(0).getY();

                break;
            case LEFT:
                headX = (snake.getPoints(0).getX() == 0) ? width - 1 : snake.getPoints(0).getX() - 1;
                headY = snake.getPoints(0).getY();
        }

        int tailX = snake.getPoints(snake.getPointsCount() - 1).getX();
        int tailY = snake.getPoints(snake.getPointsCount() - 1).getY();

        GameState.Coord headCoord = GameState.Coord.newBuilder().setX(headX).setY(headY).build();
        newSnake.addPoints(headCoord);

        boolean isFood = false;

        if(field.getBlocks()[headX][headY].isFood()){
            isFood = true;
            synchronized (food) {
                removeFood(headX, headY);
                updateFood();
            }
            field.getBlocks()[headX][headY].setSnake();
        }
        else if(field.getBlocks()[headX][headY].isEmpty()){
            field.getBlocks()[headX][headY].setSnake();
            field.getBlocks()[tailX][tailY].removeSnake();
        }
        else {
            removePlayer(snake.getPlayerId());
            removeSnake(snake);
            return null;
        }

        for(int i = 1; i < snake.getPointsCount(); ++i){
            GameState.Coord coord = GameState.Coord.newBuilder().setX(snake.getPoints(i - 1).getX())
                            .setY(snake.getPoints(i - 1).getY()).build();
            newSnake.addPoints(coord);
        }

        if(isFood){
            GameState.Coord coord = GameState.Coord.newBuilder().setX(tailX).setY(tailY).build();
            newSnake.addPoints(coord);
        }

        return newSnake.build();
    }

    //client
    public void updateGame(GameState gameState){
        stateOrder = gameState.getStateOrder();
        field.clear();

        synchronized (snakes) {
            snakes.clear();
            for (int i = 0; i < gameState.getSnakesCount(); ++i) {
                addSnake(gameState.getSnakes(i));
            }
        }

        synchronized (food) {
            food.clear();
            for (int i = 0; i < gameState.getFoodsCount(); ++i) {
                addFood(gameState.getFoods(i));
            }
        }

        synchronized (players) {
            players.clear();
            for (int i = 0; i < gameState.getPlayers().getPlayersCount(); ++i) {
                players.add(gameState.getPlayers().getPlayers(i));
            }
        }

        notifySubscribers();
    }

    private List<GameState.Coord> findPlaceForSnake() {
        List<GameState.Coord> coords = new ArrayList<>();

        int count = 0;
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                for (int i = y; i < y + 5; ++i) {  // y
                    if(i == 10)
                        break;
                    for (int j = x; j < x + 5; ++j) {  // x
                        if(j == 10)
                            break;
                        if (!field.getBlocks()[i][j].isSnake()) {
                            count++;
                            if (count == 25) {
                                GameState.Coord firstCoord = GameState.Coord.newBuilder().setX(x + 2).setY(y + 2).build();
                                GameState.Coord secondCoord = GameState.Coord.newBuilder().setX(x + 2).setY(y + 3).build();
                                coords.add(firstCoord);
                                coords.add(secondCoord);
                                return coords;
                            }
                        } else {
                            count = 0;
                            i = y + 5;
                            j = x + 5;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void addSnake(GameState.Snake snake){
        snakes.add(snake);

        for(int i = 0; i < snake.getPointsCount(); ++i){
            field.getBlocks()[snake.getPoints(i).getX()][snake.getPoints(i).getY()].setSnake();
        }
    }

    private void addFood(GameState.Coord coordFood){
        food.add(coordFood);
        field.getBlocks()[coordFood.getX()][coordFood.getY()].setFood();
    }


    private void removeFood(int x, int y){
        food.removeIf(coordFood -> coordFood.getX() == x && coordFood.getY() == y);
        field.getBlocks()[x][y].removeFood();
    }

    private void updateFood(){
        while(food.size() < foodStatic + snakes.size()){
            int newX = new Random().nextInt(width);
            int newY = new Random().nextInt(height);
            if(field.getBlocks()[newX][newX].isEmpty()){
                field.getBlocks()[newX][newY].setFood();
                GameState.Coord coord = GameState.Coord.newBuilder().setX(newX).setY(newY).build();
                food.add(coord);
            }
        }
    }

    public void createSnake(){
        snakes.clear();
        int x = new Random().nextInt(width);
        int y = new Random().nextInt(height);
        GameState.Coord firstCoord = GameState.Coord.newBuilder().setX(x).setY(y).build();
        GameState.Coord secondCoord = GameState.Coord.newBuilder().setX(x).setY((y == height - 1) ? 0 : y + 1).build();

        GameState.Snake snake = GameState.Snake.newBuilder().setState(GameState.Snake.SnakeState.ALIVE)
                .setPlayerId(idPlayer).addPoints(firstCoord).addPoints(secondCoord)
                .setHeadDirection(SnakesProto.Direction.UP).build();

        snakes.add(snake);

        field.getBlocks()[x][y].setSnake();
        field.getBlocks()[x][(y == height - 1) ? 0 : y + 1].setSnake();
    }

    public void removeSnake(GameState.Snake snake){
        for(int i = 0; i < snake.getPointsCount(); ++i){
            GameState.Coord coord = snake.getPoints(i);
            field.getBlocks()[coord.getX()][coord.getY()].removeSnake();
        }
    }

    public void setDirectionMaster(Direction direction){
        GameState.Snake.Builder snake = GameState.Snake.newBuilder().setHeadDirection(direction).setState(GameState.Snake.SnakeState.ALIVE)
                        .setPlayerId(idPlayer);
        for(GameState.Coord coord : snakes.get(idPlayer).getPointsList()){
            snake.addPoints(coord);
        }

        synchronized (snakes) {
            //snakes.set(0, snake.build());
            for(int i = 0; i < snakes.size(); ++i){
                if(snakes.get(i).getPlayerId() == idPlayer){
                    snakes.set(i, snake.build());
                    break;
                }
            }
        }
    }

    public Field getField(){
        return field;
    }

    public NodeRole getRole() {
        return role;
    }

    public List<GameState.Snake> getSnakes() {
        return snakes;
    }

    public List<GameState.Coord> getFood() {
        return food;
    }

    public GamePlayers getPlayers(){
        GamePlayers.Builder gamePlayers = GamePlayers.newBuilder();
        synchronized (players) {
            for (GamePlayer player : players) {
                gamePlayers.addPlayers(player);
            }
        }
        return gamePlayers.build();
    }

    public GamePlayer getPlayer(int id){
        synchronized (players){
            for(GamePlayer player : players){
                if(player.getId() == id)
                    return player;
            }
        }
        return null;
    }

    public void removePlayer(int idPlayer){
        synchronized (players) {
            players.removeIf(player -> player.getId() == idPlayer);
        }
    }

    public int getCountSnakes() {
        return snakes.size();
    }

    public String getName(){
        return gameName;
    }

    public int getStateOrder() {
        return stateOrder;
    }

    public void addPlayer(GamePlayer player){
        synchronized (players) {
            players.add(player);
        }
    }

    public void setRole(NodeRole role){
        this.role = role;
    }

    public GameConfig getGameConfig() {
        return gameConfig;
    }

    public int getIdPlayer() {
        return idPlayer;
    }

    public void setIdPlayer(int idPlayer) {
        this.idPlayer = idPlayer;
    }
}
