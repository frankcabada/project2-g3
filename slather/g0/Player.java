package slather.g0;

import slather.sim.Cell;
import slather.sim.Point;
import slather.sim.Move;
import slather.sim.Pherome;
import java.util.*;


public class Player implements slather.sim.Player {
    
    private Random gen;

    public void init(double d, int t) {
	gen = new Random();
    }

    public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
	if (player_cell.getDiameter() >= 2) // reproduce whenever possible
	    return new Move(true, (byte)-1, (byte)-1);
	if (memory > 0) { // follow previous direction unless it would cause a collision
	    Point vector = extractVectorFromAngle( (int)memory);
	    // check for collisions
	    if (!collides( player_cell, vector, nearby_cells, nearby_pheromes))
		return new Move(vector, memory);
	}

	// if no previous direction specified or if there was a collision, try random directions to go in until one doesn't collide
	for (int i=0; i<4; i++) {
	    int arg = gen.nextInt(180)+1;
	    Point vector = extractVectorFromAngle(arg);
	    if (!collides(player_cell, vector, nearby_cells, nearby_pheromes)) 
		return new Move(vector, (byte) arg);
	}

	// if all tries fail, just chill in place
	return new Move(new Point(0,0), (byte)0);
    }

    // check if moving player_cell by vector collides with any nearby cell or hostile pherome
    private boolean collides(Cell player_cell, Point vector, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
	Iterator<Cell> cell_it = nearby_cells.iterator();
	Point destination = player_cell.getPosition().move(vector);
	while (cell_it.hasNext()) {
	    Cell other = cell_it.next();
	    if ( destination.distance(other.getPosition()) < 0.5*player_cell.getDiameter() + 0.5*other.getDiameter() + 0.00011) 
		return true;
	}
	Iterator<Pherome> pherome_it = nearby_pheromes.iterator();
	while (pherome_it.hasNext()) {
	    Pherome other = pherome_it.next();
	    if (other.player != player_cell.player && destination.distance(other.getPosition()) < 0.5*player_cell.getDiameter() + 0.0001) 
		return true;
	}
	return false;
    }

    // convert an angle (in 2-deg increments) to a vector with magnitude Cell.move_dist (max allowed movement distance)
    private Point extractVectorFromAngle(int arg) {
	double theta = Math.toRadians( 2* (double)arg );
	double dx = Cell.move_dist * Math.cos(theta);
	double dy = Cell.move_dist * Math.sin(theta);
	return new Point(dx, dy);
    }

}
