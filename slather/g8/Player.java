package slather.g8;

import slather.sim.Cell;
import slather.sim.Point;
import slather.sim.Move;
import slather.sim.Pherome;
import java.util.*;


public class Player implements slather.sim.Player {

    private Random gen;
    private double d;
    private int t;

    private static final int NUMDIRECTIONS = 4;   // CONSTANTS - number of directions
    private static final int MAX_SHAPE_MEM = 4;   // CONSTANTS - maximum shape size (in bits)
    private static final int MIN_SHAPE_MEM = 2;   // CONSTANTS - minimum shape size (in bits)
    private static final int DURATION_CAP = 4;   // CONSTANTS - cap on traveling
    private static final int AVOID_DIST = 5; // CONSTANTS - for random walker: turn away from friendlies that are in this radius
    private static final double MAX_MOVEMENT = 1; // CONSTANTS - maximum movement rate (assumed to be 1mm?)
    private int SHAPE_MEM_USAGE; // CONSTANTS - calculate this based on t
    private int EFFECTIVE_SHAPE_SIZE; // CONSTANTS - actual number of sides to our shape

    
    public void init(double d, int t, int sideLength) {
	gen = new Random();
	this.d = d;
	this.t = t;
        SHAPE_MEM_USAGE = (int) Math.ceil( Math.log(t)/Math.log(2) );
        SHAPE_MEM_USAGE = Math.max(MIN_SHAPE_MEM, SHAPE_MEM_USAGE);
        SHAPE_MEM_USAGE = Math.min(MAX_SHAPE_MEM, SHAPE_MEM_USAGE);
        EFFECTIVE_SHAPE_SIZE = (int) Math.min( Math.pow(2, SHAPE_MEM_USAGE), t );
        EFFECTIVE_SHAPE_SIZE = (int) Math.max( 4, EFFECTIVE_SHAPE_SIZE);
        // TODO: put minimum for small t?
    }

    /*
      Memory byte setup:
      1 bit strategy (currently either square walker or random walker)

      Memory byte for square walker:
      1 bit strategy
      3 bits empty for now
      0-4 bits shape

      Memory byte for random walker:
      1 bit strategy
      7 bits previous direction

      Directions:
      0 = North
      1 = East
      2 = South
      3 = West
     */
     public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {

         Point currentPosition  = player_cell.getPosition();
         int strategy = getStrategy(memory);
         Move nextMove = null;
         String s = String.format("%8s", Integer.toBinaryString(memory & 0xFF)).replace(' ','0');
         System.out.println("Memory byte: " + s);




         if (player_cell.getDiameter() >= 2) // reproduce whenever possible
            return new Move(true, (byte) 0b10000000, (byte) 0);




        /*

            Clustering ALgorithm - we are going to take a leaf out of group 1's
            and start clustering after a certain point

        */
         if (strategy == 1) {

             // get the average vector for all cells, take the inverse of the vector
             // and then use that to move a bit. add a little bit of aggressiveness
             // if there are several cells nearby

             double vectx = 0.0;
             double vecty = 0.0;
             for (Cell nc : nearby_cells) {
                 double weight = 1.0;
                 if (nc.player == player_cell.player)
                    weight = 0.9;
                 else
                    weight = 1.1;
                 Point ncp = nc.getPosition();
                 Point pcp = player_cell.getPosition();
                 vectx = vectx + weight*(ncp.x - pcp.x);
                 vecty = vecty + weight*(ncp.y - pcp.y);
             }
             double avg_x = vectx / Math.max(nearby_cells.size(), 1);
             double avg_y = vecty / Math.max(nearby_cells.size(), 1);

             double hyp = Math.sqrt(Math.pow(avg_x, 2.0) + Math.pow(avg_y, 2.0));
             if (hyp > Cell.move_dist) {
                 avg_x = avg_x * (Cell.move_dist / hyp);
                 avg_y = avg_y * (Cell.move_dist / hyp);
             }
             Point nextPoint = new Point(-avg_x, -avg_y);
             int count = 0;
             int maxTries = 100;
             while ((nextPoint.x == 0 && nextPoint.y == 0)
                    || collides(player_cell, nextPoint, nearby_cells, nearby_pheromes)) {
                 // if tried maximum # of times without success, just stay still
                 if (count == maxTries) {
                     nextPoint = new Point(0,0);
                     break;
                 }
                 int angle = gen.nextInt(120);
                 nextPoint = extractVectorFromAngle(angle);
                 count++;
             }
             return new Move(nextPoint, memory);


         } // end square walker strategy





         else {

            /* Random walker strategy:
               Move away from cells that are too close (specified by AVOID_DIST)
               If no closeby friendly cells to avoid, act like default player (move in straight lines)
             */
            int prevAngle = memory & 0b01111111;

            Iterator<Cell> cell_it = nearby_cells.iterator();
            double sumX = 0;
            double sumY = 0;
            int count = 0;
            Point vector;

            // calculate avg position of nearby cells
            while (cell_it.hasNext()) {
                Cell curr = cell_it.next();
                if (player_cell.distance(curr) > AVOID_DIST) // don't worry about far away cells
                    continue;
                Point currPos = curr.getPosition();
                sumX += currPos.x;
                sumY += currPos.y;
                count++;
            }

            // add unfriendly pheromes
            Iterator<Pherome> ph_it = nearby_pheromes.iterator();
            while (ph_it.hasNext()) {
                Pherome curr = ph_it.next();
                if (curr.player == player_cell.player)
                    continue;
                if (player_cell.distance(curr) > AVOID_DIST)
                    continue;
                Point currPos = curr.getPosition();
                sumX += currPos.x;
                sumY += currPos.y;
                count++;
            }

            if (count == 0) { // case: no cells to move away from
                // if had a previous direction, keep going in that direction
                if (prevAngle > 0) {
                    vector = extractVectorFromAngle( (int)prevAngle);
                    if (!collides( player_cell, vector, nearby_cells, nearby_pheromes)) {
                        nextMove = new Move(vector, memory);
                    }
                }

                // if will collide or didn't have a previous direction, pick a random direction generate move
                if (nextMove == null) {
                    int newAngle = gen.nextInt(120);
                    Point newVector = extractVectorFromAngle(newAngle);
                    byte newMemory = (byte) (newAngle);
                    nextMove = new Move(newVector, newMemory);
                }
            } else { // case: cells too close, move in opposite direction
                double avgX = sumX / ((double) count);
                double avgY = sumY / ((double) count);

                double towardsAvgX = avgX - currentPosition.x;
                double towardsAvgY = avgY - currentPosition.y;

                double distanceFromAvg = Math.hypot(towardsAvgX, towardsAvgY);

                double awayX = (-(towardsAvgX)/distanceFromAvg) * Cell.move_dist;
                double awayY = (-(towardsAvgY)/distanceFromAvg) * Cell.move_dist;

                // check if cells are in the direction we are moving toward
                cell_it = nearby_cells.iterator();
                while (cell_it.hasNext()) {
                    Cell curr = cell_it.next();
                    if (player_cell.distance(curr) > AVOID_DIST) // don't worry about far away cells
                        continue;
                    Point currPos = curr.getPosition();
                    Point playerPos = player_cell.getPosition();
                    if (((currPos.x - playerPos.x) / (currPos.y - playerPos.y)) == (awayX / awayY)) {
                        // try orthogonal vector if there are cells in the way
                        boolean newOption = true;
                        Iterator<Cell> test_cell_it = nearby_cells.iterator();
                        while (test_cell_it.hasNext()) {
                            Cell testCell = test_cell_it.next();
                            if (player_cell.distance(testCell) > AVOID_DIST) // don't worry about far away cells
                                continue;
                            Point testPos = testCell.getPosition();
                            if (((testPos.x + playerPos.y) / (testPos.y - playerPos.x)) == (-awayY / awayX)) {
                                newOption = false;
                                break;
                            }
                        }
                        if (newOption) {
                            double savedX = awayX;
                            awayX = -awayY;
                            awayY = savedX;
                            break;
                        }
                    }
                }

                // clear the previous vector bits
                int newAngle = 0;
                Point newVector = new Point(awayX, awayY);
                byte newMemory = (byte) (newAngle);
                nextMove = new Move(newVector, newMemory);
            }

            // candidate nextMove written, check for collision

            if (collides(player_cell, nextMove.vector, nearby_cells, nearby_pheromes)) {
                nextMove = null;
                int arg = gen.nextInt(120);
                // try 20 times to avoid collision
                for (int i = 0; i < 20; i++) {
                    Point newVector = extractVectorFromAngle(arg);
                    if (!collides(player_cell, newVector, nearby_cells, nearby_pheromes)) {
                        byte newMemory = (byte) (arg);
                        nextMove = new Move(newVector, newMemory);
                        break;
                    }
                }
                if (nextMove == null) { // if still keeps colliding, stay in place
                    nextMove = new Move(new Point(0,0), (byte) 0);
                }
            } // end check candidate nextMove collision
        } // end random walker

        System.out.println("Next move: " + nextMove.vector.x + ", " + nextMove.vector.y);
        Point estimate = getVector(player_cell, player_cell, nearby_pheromes);
        System.out.println("Estimated last move: " + estimate.x + ", " + estimate.y);
        return nextMove;
    } // end Move()

    // check if moving player_cell by vector collides with any nearby cell or hostile pherome
    private boolean collides(Cell player_cell, Point vector, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
	Iterator<Cell> cell_it = nearby_cells.iterator();
	Point destination = player_cell.getPosition().move(vector);
	while (cell_it.hasNext()) {
	    Cell other = cell_it.next();
	    if ( destination.distance(other.getPosition()) < 0.5*player_cell.getDiameter()*1.01 + 0.5*other.getDiameter() + 0.00011)
		return true;
	}
	Iterator<Pherome> pherome_it = nearby_pheromes.iterator();
	while (pherome_it.hasNext()) {
	    Pherome other = pherome_it.next();
            if (other.player != player_cell.player && destination.distance(other.getPosition()) < 0.5*player_cell.getDiameter()*1.01 + 0.0001)
                return true;
	}
	return false;
    }

    // convert an angle (in 3-deg increments) to a vector with magnitude Cell.move_dist (max allowed movement distance)
    private Point extractVectorFromAngle(int arg) {
	double theta = Math.toRadians( 3* (double)arg );
	double dx = Cell.move_dist * Math.cos(theta);
	double dy = Cell.move_dist * Math.sin(theta);
	return new Point(dx, dy);
    }

    /* Gets vector based on direction (which side of the shape cell will move to)
     */
    private Point getVectorFromDirection(int direction) {
        double theta = Math.toRadians((360 / EFFECTIVE_SHAPE_SIZE) * direction);
        double dx = Cell.move_dist * Math.cos(theta);
        double dy = Cell.move_dist * Math.sin(theta);
        return new Point(dx, dy);
    }

    private int getDirection(byte mem) {
        return (mem & (int) (Math.pow(2.0, SHAPE_MEM_USAGE) - 1) );
    }

    private byte writeDirection(int direction, byte memory) {
        byte mask = (byte) (((int) Math.pow(2.0, 8) - 1) << SHAPE_MEM_USAGE);
        byte mem = (byte)((memory & mask) | direction);
        return mem;
    }
    private byte writeDuration(int duration, byte memory, int maxDuration) {
            int actualDuration = duration % maxDuration;
            byte mem = (byte)((memory & 0b11110011) | (actualDuration << 2));
            return mem;
    }

    private int getMaxDuration(int t, int numdirs) {
            return Math.min((t / numdirs), DURATION_CAP);
    }

    private Point getNewDest(int direction) {

            if (direction == 0) {
                    return new Point(0*Cell.move_dist,-1*Cell.move_dist);

            } else if (direction == 1) {
                    return new Point(1*Cell.move_dist,0*Cell.move_dist);

            } else if (direction == 2) {
                    return new Point(0*Cell.move_dist,1*Cell.move_dist);

            } else if (direction == 3) {
                    return new Point(-1*Cell.move_dist,0*Cell.move_dist);

            } else {
                    return new Point(0,0);
            }

    }

    private int getStrategy(byte memory) {
        int strategy = (memory >> 7) & 1;
        return strategy;
    }

    /* Estimate the last known direction of a cell given pheromes that can be seen
       Returns as a vector (Point object)
       Method: for given cell, find pherome of the same type that is <= MAX_MOVEMENT
         If more than 1 pherome found, movement cannot be determined, return MAX_MOVEMENT+1,MAX_MOVEMENT+1
         If no pheromes found and cell is at max view distance, movement cannot be determined (otherwise
           it legitimately did not move!)
     */
    private Point getVector(Cell player_cell, Cell c, Set<Pherome> nearby_pheromes) {
        Iterator<Pherome> pherome_it = nearby_pheromes.iterator();
        double dX;
        double dY;
        int count = 0;
        Pherome closest = null;
        double cRadius = c.getDiameter()/2;
        Point cPos = c.getPosition();

        while (pherome_it.hasNext()) {
            Pherome curr = pherome_it.next();
            Point currPos = curr.getPosition();
            if (curr.player != c.player || currPos == cPos)
                continue;
            double distance = c.distance(curr) + cRadius;
            if (distance <= MAX_MOVEMENT) {
                count++;
                closest = curr;
            }
        }

        if (count > 1) { // more than one close pherome, vector cannot be determined
            dX = MAX_MOVEMENT + 1;
            dY = MAX_MOVEMENT + 1;
            System.out.println("Found too many pheromes: " + count);
            return new Point(dX, dY);
        }
        else if (count == 0) { // no pheromes deteced closeby
            double distanceCelltoCell = player_cell.distance(c);
            if ( (distanceCelltoCell + player_cell.getDiameter()/2 + c.getDiameter()/2) >= d ) {
                // other cell is at edge of view, cannot determine vector
                dX = MAX_MOVEMENT + 1;
                dY = MAX_MOVEMENT + 1;
                return new Point(dX, dY);
            } else {
                // other cell well within view and no closeby pheromes, so it actually didn't move
                return new Point(0, 0);
            }
        }
        else if (count == 1) { // only 1 pherome detected, can get vector
            Point cPosition = c.getPosition();
            Point pPosition = closest.getPosition();
            dX = cPosition.x - pPosition.x;
            dY = cPosition.y - pPosition.y;
            return new Point(dX, dY);
        }
        else {
            dX = MAX_MOVEMENT + 1;
            dY = MAX_MOVEMENT + 1;
            return new Point(dX, dY);
        }
    }
}
