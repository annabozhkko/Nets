package model;

public class Field {
    final private Block[][] blocks;
    private int width;
    private int height;

    public Field(int width, int height){
        blocks = new Block[width][height];
        for(int x = 0; x < width; ++x){
            for(int y = 0; y < height; ++y)
                blocks[x][y] = new Block();
        }

        this.height = height;
        this.width = width;
    }

    public Block[][] getBlocks(){
        return blocks;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public void clear(){
        for(int x = 0; x < width; ++x){
            for(int y = 0; y < height; ++y)
                blocks[x][y].setEmpty();
        }
    }
}
