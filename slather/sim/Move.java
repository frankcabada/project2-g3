package slather.sim;

public class Move {

    public final Point vector;
    public final boolean reproduce;
    public final byte memory;
    public final byte daughter_memory;

    public Move(boolean reproduce, byte memory, byte daughter_memory) {
	this.reproduce = reproduce;
	this.vector = new Point(0,0);
	this.memory = memory;
	this.daughter_memory = daughter_memory;
    }

    public Move(Point vector, byte memory) {
	this.vector = vector;
	this.reproduce = false;
	this.memory = memory;
	this.daughter_memory = 0;
    }
    
}
