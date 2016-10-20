package slather.g9;

import slather.sim.Cell;
import slather.sim.GridObject;
import slather.sim.Point;
import slather.sim.Move;
import slather.sim.Pherome;
import java.util.*;

// find largest free angle by sort, escape from self cells and pheromes

public class Player implements slather.sim.Player {

	private Random gen;

	private static double EARLY_STAGE = 1. / 2.;
	private static double LATE_STAGE = 4. / 5.;
	private static double COMF_RANGE = 1;

    private static int EXPLORER = 0;
    private static int DEFENDER = 1;
    private static int ANGLE_INCREMENTS = 3;
    private static double SCALE_THRESHOLD = 0.1;

	private static double ratio = 1.0;
	private static double sight = 3;

    class ScoredObject implements Comparable<ScoredObject> {
        public GridObject object;
        public int angle;

        public ScoredObject(GridObject object, int angle) {
            this.object = object;
            this.angle = angle;
        }

        public int compareTo(ScoredObject o) {
            return this.angle - o.angle;
        }
    }

	public void init(double d, int t, int side_length) {
		gen = new Random();
	}

    private byte packByteExplorer(int angle) {
        byte memory = (byte) 0;
        memory = (byte) (memory << 7);
        memory = (byte) angle;

        return memory;
    }

    private byte packByteDefender() {
        byte memory = (byte) 1;
        memory = (byte) (memory << 7);

        return memory;
    }

    private int unpackRole(byte memory) {
        memory = (byte) (memory >> 7);
        return memory & 0x1;
    }

    private int unpackAngleExplorer(byte memory) {
        return memory & 0x7F;
    }

    private ArrayList<GridObject> getNearbyObstacles(Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        ArrayList<GridObject> nearby_obstacles = new ArrayList<GridObject>();
        ArrayList<GridObject> nearby_same = new ArrayList<GridObject>();
        ArrayList<GridObject> nearby_different = new ArrayList<GridObject>();

        for (Cell cell : nearby_cells) {
            if (player_cell.getPosition().distance(cell.getPosition()) < sight * player_cell.getDiameter()){
                if(cell.player == player_cell.player){
                    nearby_same.add(cell);
                }else{
                    nearby_different.add(cell);
                }
                nearby_obstacles.add(cell);
            } 
        }

		for (Pherome pherome : nearby_pheromes) {
			if (player_cell.getPosition().distance(pherome.getPosition()) < sight * player_cell.getDiameter()) {
				nearby_obstacles.add(pherome);
            }
		}

        if(nearby_same.size() * ratio > nearby_different.size()){
            return nearby_same;
        }

        return nearby_obstacles;
    }

	private ArrayList<GridObject> getNearbyObstaclesEscapeSelf(Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		ArrayList<GridObject> nearby_obstacles = new ArrayList<GridObject>();
        ArrayList<GridObject> nearby_same = new ArrayList<GridObject>();        
        ArrayList<GridObject> nearby_different = new ArrayList<GridObject>();

		for (Cell cell : nearby_cells) {
			if (player_cell.getPosition().distance(cell.getPosition()) < sight * player_cell.getDiameter()){
                if(cell.player == player_cell.player){
                    nearby_same.add(cell);
                }else{
                    nearby_different.add(cell);
                }
                nearby_obstacles.add(cell);
            } 
        }

		for (Pherome pherome : nearby_pheromes) {
			if (player_cell.getPosition().distance(pherome.getPosition()) < sight * player_cell.getDiameter()  && pherome.player != player_cell.player) {
                nearby_obstacles.add(pherome);
            }
		}

        if(nearby_same.size() * ratio > nearby_different.size()){
            return nearby_same;
        }

		return nearby_obstacles;
	}

    private Move getDefaultMove(Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		/*
		if (player_cell.getDiameter() < 1.2) {
			int coin = gen.nextInt(2);
			if (coin == 0) return new Move(new Point(0, 0), (byte)0);
		}
		*/

        // if no previous direction specified or if there was a collision, try random directions to go in until one doesn't collide
        for (double scale = 1.0; scale > SCALE_THRESHOLD; scale -= 0.1) {
            int angle = gen.nextInt(360 / ANGLE_INCREMENTS) + 1;
            Point vector = extractVectorFromAngle(angle, scale);
            if (!collides(player_cell, vector, nearby_cells, nearby_pheromes)) {
                return new Move(vector, packByteExplorer(angle));
            }
        }

        // if all tries fail, just chill in place
        return new Move(new Point(0,0), (byte)0);
    }

    private int getAngleFrom(Cell player_cell, GridObject obstacle) {
        Point src = player_cell.getPosition();
        Point dest = obstacle.getPosition();

        double angle = Math.toDegrees(Math.atan2(dest.y - src.y, dest.x - src.x));
        if (angle < 0.0) {
            angle += 360.0;
        }

        return (int) (angle / ANGLE_INCREMENTS);
    }

    private int getLargestFreeAngle(Cell player_cell, ArrayList<GridObject> nearby_obstacles) {
        ArrayList<ScoredObject> scored_obstacles = new ArrayList<ScoredObject>();

        for (GridObject obstacle : nearby_obstacles) {
            ScoredObject scored_obstacle = new ScoredObject(obstacle, getAngleFrom(player_cell, obstacle));
            scored_obstacles.add(scored_obstacle);
        }

        Collections.sort(scored_obstacles);
        int largest_free_angle = 0;
        int ending_angle = -1;
        int target_angle = -1;

        for (int i = 1; i < scored_obstacles.size(); i++) {
            int free_angle = scored_obstacles.get(i).angle - scored_obstacles.get(i-1).angle;
            if (free_angle > largest_free_angle) {
                largest_free_angle = free_angle;
                ending_angle = scored_obstacles.get(i).angle;
                target_angle = ending_angle - (largest_free_angle / 2);
            }
        }

        // Account for the wraparound effect
        int first_angle = scored_obstacles.get(0).angle;
        int last_angle = scored_obstacles.get(scored_obstacles.size() - 1).angle;
        int wraparound_free_angle = first_angle + (360 / ANGLE_INCREMENTS) - last_angle;
        if (wraparound_free_angle > largest_free_angle) {
            largest_free_angle = wraparound_free_angle;
            ending_angle = first_angle;
            target_angle = ending_angle - (largest_free_angle / 2);

            if (target_angle < 0) {
                target_angle += 360 / ANGLE_INCREMENTS;
            }
        }

        return target_angle;
    }

    private Move getExplorerMove(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        ArrayList<GridObject> nearby_obstacles = getNearbyObstaclesEscapeSelf(player_cell, nearby_cells, nearby_pheromes);
        if (nearby_obstacles.size() > 0) {
            // Move in the direction of the largest free angle
            if (nearby_obstacles.size() == 1) {
                int angle = getAngleFrom(player_cell, nearby_obstacles.get(0));
                int target_angle = angle + (180 / ANGLE_INCREMENTS);
                if (target_angle > (360 / ANGLE_INCREMENTS)) {
                    target_angle -= 360 / ANGLE_INCREMENTS;
                }

                for (double scale = 1.0; scale > SCALE_THRESHOLD; scale -= 0.1) {
                    Point vector = extractVectorFromAngle(target_angle, scale);
                    if (!collides(player_cell, vector, nearby_cells, nearby_pheromes)) {
                        return new Move(vector, packByteExplorer(target_angle));
                    }
                }
            } else {
                int target_angle = getLargestFreeAngle(player_cell, nearby_obstacles);
                for (double scale = 1.0; scale > SCALE_THRESHOLD; scale -= 0.1) {
                    Point vector = extractVectorFromAngle(target_angle, scale);
                    if (!collides(player_cell, vector, nearby_cells, nearby_pheromes)) {
                        return new Move(vector, packByteExplorer(target_angle));
                    }
                }
            }
        } else {
            // Move in the same direction
            Point previous_direction = extractVectorFromAngle(unpackAngleExplorer(memory), 1.0);
            if (!collides(player_cell, previous_direction, nearby_cells, nearby_pheromes)) {
                return new Move(previous_direction, memory);
            }
        }

		nearby_obstacles = getNearbyObstacles(player_cell, nearby_cells, nearby_pheromes);
		if (nearby_obstacles.size() > 0) {
			// Move in the direction of the largest free angle
			if (nearby_obstacles.size() == 1) {
				int angle = getAngleFrom(player_cell, nearby_obstacles.get(0));
				int target_angle = angle + (180 / ANGLE_INCREMENTS);
                if (target_angle > (360 / ANGLE_INCREMENTS)) {
                    target_angle -= 360 / ANGLE_INCREMENTS;
                }

                for (double scale = 1.0; scale > SCALE_THRESHOLD; scale -= 0.1) {
                    Point vector = extractVectorFromAngle(target_angle, scale);
                    if (!collides(player_cell, vector, nearby_cells, nearby_pheromes)) {
                        return new Move(vector, packByteExplorer(target_angle));
                    }
                }
            } else {
                int target_angle = getLargestFreeAngle(player_cell, nearby_obstacles);
                for (double scale = 1.0; scale > SCALE_THRESHOLD; scale -= 0.1) {
                    Point vector = extractVectorFromAngle(target_angle, scale);
                    if (!collides(player_cell, vector, nearby_cells, nearby_pheromes)) {
                        return new Move(vector, packByteExplorer(target_angle));
                    }
                }
            }
        } else {
            // Move in the same direction
            Point previous_direction = extractVectorFromAngle(unpackAngleExplorer(memory), 1.0);
            if (!collides(player_cell, previous_direction, nearby_cells, nearby_pheromes)) {
                return new Move(previous_direction, memory);
            }
        }

        return getDefaultMove(player_cell, nearby_cells, nearby_pheromes);
    }

    public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        if (player_cell.getDiameter() >= 2) {
            byte first_byte = packByteExplorer(gen.nextInt(360/ANGLE_INCREMENTS) + 1);
            byte second_byte = packByteExplorer(gen.nextInt(360/ANGLE_INCREMENTS) + 1);
            return new Move(true, first_byte, second_byte);
        }

        if (unpackRole(memory) == EXPLORER) {
            return getExplorerMove(player_cell, memory, nearby_cells, nearby_pheromes);
        } else {
            return new Move(new Point(0,0), (byte)0);
        }
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

	// convert an angle (in ANGLE_INCREMENTS increments) to a vector with magnitude Cell.move_dist (max allowed movement distance)
	private Point extractVectorFromAngle(int arg, double scale) {
		double theta = Math.toRadians(ANGLE_INCREMENTS * (double)arg);
		double dx = Cell.move_dist * Math.cos(theta) * scale;
		double dy = Cell.move_dist * Math.sin(theta) * scale;
		return new Point(dx, dy);
	}

}
