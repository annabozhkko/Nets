package model;

public class Block {
    private boolean isSnake = false;
    private boolean isEmpty = true;
    private boolean isFood = false;

    synchronized public boolean isEmpty() {
        return isEmpty;
    }

    synchronized public boolean isFood() {
        return isFood;
    }

    synchronized public boolean isSnake() {
        return isSnake;
    }

    synchronized public void setFood(){
        isFood = true;
        isEmpty = false;
    }

    synchronized public void removeFood(){
        isFood = false;
        isEmpty = true;
    }

    synchronized public void setSnake(){
        isSnake = true;
        isEmpty = false;
    }

    synchronized public void removeSnake(){
        isSnake = false;
        isEmpty = true;
    }

    synchronized public void setEmpty(){
        isEmpty = true;
        isSnake = false;
        isFood = false;
    }
}
