package slather.sim;

import java.util.*;

public abstract class GridObject {
    public final int player;
    protected Point position;


    protected GridObject(Point position, int player) {
	this.position = position;
	this.player = player;
    }
    
    protected double distance(GridObject other) {
	if (other instanceof Pherome)
	    return position.distance(other.getPosition());
	else if (other instanceof Cell) {
	    Cell casted = (Cell) other;
	    return position.distance(other.getPosition()) - casted.getDiameter()/2;
	}
	else {
	    return 0;
	}
    }

    public Point getPosition() {
	return position;
    }
    
}
