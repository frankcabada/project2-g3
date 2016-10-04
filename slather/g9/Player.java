package slather.g9;

import slather.sim.Cell;
import slather.sim.Point;
import slather.sim.Move;
import slather.sim.Pherome;
import java.util.*;


public class Player implements slather.sim.Player {

	private Random gen;

	//=========parameters==================================
	private static double EARLY_STAGE = 1. / 2.;
	private static double LATE_STAGE = 4. / 5.;
	private static double COMF_RANGE = 1;

	//==========functions==================================
	public void init(double d, int t, int side_length) {
		gen = new Random();
	}

	public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		if (player_cell.getDiameter() >= 2 /*&& (calCrowdInDirection(player_cell, (int)memory, nearby_cells, nearby_pheromes) > COMF_RANGE)*/) // reproduce when it is not very crowd
			return new Move(true, (byte)-1, (byte)-1);

		if (memory > 0) { // follow previous direction in early stage, try least crowd direction in later
			if (calCrowdSurround(player_cell, nearby_cells, nearby_pheromes) < EARLY_STAGE) {
				Point vector = extractVectorFromAngle( (int)memory);
				// check for collisions
				if (!collides( player_cell, vector, nearby_cells, nearby_pheromes))
					return new Move(vector, memory);
			}
		}

		// otherwise, try random directions to go in until one doesn't collide
		double max_range = 0;
		int direction = 0;
		Point vector = new Point(0, 0);
		for (int arg = 1; arg <= 180; arg += 10) {
			Point current_vector = extractVectorFromAngle(arg);
			if (collides(player_cell, current_vector, nearby_cells, nearby_pheromes)) continue;
			double current_range = calCrowdInDirection(player_cell, arg, nearby_cells, nearby_pheromes);
			if (current_range > max_range) {
				max_range = current_range;
				direction = arg;
				vector = current_vector;
			} else
			if (current_range == max_range) {
				int tmp = gen.nextInt(10);
				if (tmp < 1) {
					direction = arg;
					vector = current_vector;
				}
			}
		}
		if (!collides(player_cell, vector, nearby_cells, nearby_pheromes))
			return new Move(vector, (byte) direction);

		// if all tries fail, just chill in place
		return new Move(new Point(0,0), (byte)0);
	}

	double calCrowdSurround(Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		int cnt = 0;
		for (int arg = 1; arg <= 180; arg += 5) {
			Point current_vector = extractVectorFromAngle(arg);
			if (collides(player_cell, current_vector, nearby_cells, nearby_pheromes))
				++cnt;
		}
		return (double)cnt / 36.;
	}

	double calCrowdInDirection(Cell player_cell, int arg, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		if (arg > 0) {
			Point vector = extractVectorFromAngle(arg);
			if (collides(player_cell, vector, nearby_cells, nearby_pheromes)) return 0;

			Point destination = player_cell.getPosition().move(vector);
			if (calCrowdSurround(player_cell, nearby_cells, nearby_pheromes) > LATE_STAGE) return 1;
		}

		for (int delta = 1; arg - delta > 0 && arg + delta <= 180 && delta < 30; ++delta) {
			Point vector1 = extractVectorFromAngle(arg - delta);
			Point vector2 = extractVectorFromAngle(arg + delta);
			if (!collides(player_cell, vector1, nearby_cells, nearby_pheromes) &&
			    !collides(player_cell, vector2, nearby_cells, nearby_pheromes))
				continue;
			return delta;
		}
		return 90;
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
