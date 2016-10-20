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
    private int sideLength;

    // constants to label strategies
    private static final int CLUSTER = 0;
    private static final int SYNC = 1;
    private static final int BORDER = 2;
    private static final int TCELL = 3;

    private static int AVOID_DIST = 2;
    private static int FRIENDLY_AVOID_DIST = 4;
    private static double NEXT_REDUCE_MOVE = 0.95;
    private static int MAX_TRIES = 30;
    private static int PH_AVOID_DIST = 2;

    // constants for sync strategy
    private static int SYNC_MAX_DIR_COUNT = 16; // determined by t
    private static final int SYNC_LOOK_ARC = 240; // constrained arc to look for the next move
    private static final int SYNC_SIGHT_THRESHOLD = 5; // don't care about cells further than this when calculating widest angle for move
    private static final double SYNC_DIAMETER_TRIGGER = 1.5; // size to consider evolving behavior
    private static int SYNC_ENEMY_COUNT_TRIGGER = 4; // if see this many enemies, evolve behavior
    private static final int SYNC_INSIDE_CROWDED_THRESHOLD = 4; // if see this many friendlies to your inside, consider the group crowded

    
    public void init(double d, int t, int sideLength) {
        gen = new Random();
        this.d = d;
        this.t = t;
        this.sideLength = sideLength;
        SYNC_ENEMY_COUNT_TRIGGER = Math.min((int)d, 4);
        SYNC_MAX_DIR_COUNT = Math.max(t, 8);
        SYNC_MAX_DIR_COUNT = Math.min(SYNC_MAX_DIR_COUNT, 64);
    }

    /*
      Memory byte setup:
      2 bits strategy
      6 bits for strategy's use
     */
     public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
         Point currentPosition  = player_cell.getPosition();
         int strategy = getStrategy(memory);
         Move nextMove = null;
         String s = String.format("%8s", Integer.toBinaryString(memory & 0xFF)).replace(' ','0');
         //System.out.println("Memory byte: " + s);

        if (strategy == 0) {
            nextMove = cluster(player_cell, memory, nearby_cells, nearby_pheromes);
        } else if (strategy == 1) {
            nextMove = sync(player_cell, memory, nearby_cells, nearby_pheromes);
        } else if (strategy == 2) {
            nextMove = scout(player_cell, memory, nearby_cells, nearby_pheromes);
        } else if (strategy == 3) {
            nextMove = tcell(player_cell, memory, nearby_cells, nearby_pheromes);
        } else {
            nextMove = new Move(new Point(0,0), memory);
        }

        return nextMove;
    }


    private Move scout(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        if (player_cell.getDiameter() >= 2) {
           return new Move(true, memory, memory);
        }

        int prevAngle = memory & 0b00001111;
        Point currentPosition = player_cell.getPosition();
        Move nextMove = null;

        double[] bounds = getWidestAngle(player_cell, nearby_cells, nearby_pheromes);

        if (bounds == null) { // case: no cells
            // if had a previous direction, keep going in that direction
            if (prevAngle > 0) {
                Point vector = extractVectorFromAngle( (int)prevAngle);
                if (!collides( player_cell, vector, nearby_cells, nearby_pheromes)) {
                    nextMove = new Move(vector, memory);
                }
            }

            // if will collide or didn't have a previous direction, pick a random direction generate move
            if (nextMove == null) {
                int newAngle = gen.nextInt(12);
                Point newVector = extractVectorFromAngle(newAngle);
                byte angleBits = (byte) (newAngle & 0b00001111);
                byte newMemory = (byte) (memory & 0b11110000);
                newMemory = (byte) (newMemory | angleBits);
                nextMove = new Move(newVector, newMemory);
            }
        } 
        else { // case: there are cells
            // calculate direction to go (aim for the "middle" of the widest angle)
            double lowerAngle = bounds[0];
            if (bounds[0] > bounds[1]) {
                lowerAngle = getNegativeAngle(bounds[0], "d");
            }

            double destAngle = lowerAngle + (bounds[1] - lowerAngle) / 2;

            double destX = Cell.move_dist * Math.cos(destAngle);
            double destY = Cell.move_dist * Math.sin(destAngle);

            // replace the previous vector bits with new direction
            int newAngle = (int) (getPositiveAngle(Math.toDegrees(destAngle), "d") / 30);
            Point newVector = new Point(destX, destY);
            byte angleBits = (byte) (newAngle & 0b00001111);
            byte newMemory = (byte) (memory & 0b11110000);
            newMemory = (byte) (newMemory | angleBits);
            nextMove = new Move(newVector, newMemory);
        }

        // candidate nextMove written, check for collision

        if (collides(player_cell, nextMove.vector, nearby_cells, nearby_pheromes)) {
            nextMove = null;
            int arg = gen.nextInt(12);
            // try 20 times to avoid collision
            for (int i = 0; i < 20; i++) {
                Point newVector = extractVectorFromAngle(arg);
                if (!collides(player_cell, newVector, nearby_cells, nearby_pheromes)) {
                    byte angleBits = (byte) (arg & 0b00001111);
                    byte newMemory = (byte) (memory & 0b11110000);
                    newMemory = (byte) (newMemory | angleBits);
                    nextMove = new Move(newVector, newMemory);
                    break;
                }
            }
            if (nextMove == null) { // if still keeps colliding, stay in place
                nextMove = new Move(new Point(0,0), (byte) memory);
            }
        } // end check candidate nextMove collision
        return nextMove;
    }


    /*
      Memory:
      4 bits strategy
      2 bits count
      1 bit evolve to sync? (0 for evolve, 1 to never evolve)
      3 bits generation count
     */
    private Move cluster(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        // if can reproduce, do so and increment the generation
        int genCount = (byte) memory & 0b00000111;
        int evolveBit = (byte) ((memory >> 3) & 0b00000001);
        if (genCount == 3 && evolveBit == 0) {
            byte nextMemory = (byte) 0b01000000;
            return sync(player_cell, nextMemory, nearby_cells, nearby_pheromes);
        }


        if (player_cell.getDiameter() >= 2) {
            genCount = (genCount + 1) % 16;
            byte nextMemory = (byte) (memory & 0b11110000);
            nextMemory = (byte) (nextMemory | genCount);
            return new Move(true, nextMemory, nextMemory);
        }


        // get the average vector for all cells, take the inverse of the vector
        // and then use that to move a bit. add a little bit of aggressiveness
        // if there are several cells nearby
        int count = memory >> 4;

        // begin spreading out

        double vectx = 0.0;
        double vecty = 0.0;
        double vectfx = 0.0;
        double vectfy = 0.0;
        int fccnt = 0;
        for (Cell nc : nearby_cells) {

            // get their positions
            Point ncp = nc.getPosition();
            Point pcp = player_cell.getPosition();

            // add some weights, and get distances
            double weight;
            double distx = Math.abs(ncp.x - pcp.x);
            double disty = Math.abs(ncp.y - pcp.y);
            double distance = player_cell.distance(nc);

            if (distance >= AVOID_DIST) {
                continue;
            }

            // use distance and type or set weights
            if (nc.player == player_cell.player) {
                weight = 0.2;
                double fdistx = Math.abs(ncp.x - pcp.x);
                double fdisty = Math.abs(ncp.y - pcp.y);
                vectfx = vectfx + ((ncp.x - pcp.x) * Math.pow(0.5, 2*fdisty));
                vectfy = vectfy + ((ncp.y - pcp.y) * Math.pow(0.5, 2*fdistx));
                fccnt++;
            } else {
                if (distance > 1.1)
                    weight = -0.003;
                else
                    weight = 0.1;
            }
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

        fccnt = Math.max(fccnt, 1);
        if (vectfx == 0) vectfx = 1;
        if (vectfy == 0) vectfy = 1;
        if (count == 3) {
            avg_x = avg_x - 0.5*(vectfy / (double)fccnt);
            avg_y = avg_y + 0.5*(vectfx / (double)fccnt);
            count = 7;
        } else if (count == 7) {
            avg_x = avg_x + 0.5*(vectfy / (double)fccnt);
            avg_y = avg_y - 0.5*(vectfx / (double)fccnt);
            count = 3;
        } else {
            count++;
        }
        Point nextPoint = new Point(-avg_x, -avg_y);

        // if collides, try reducing the vector MAX_TRIES times
        int count2 = 0;
        while (collides(player_cell, nextPoint, nearby_cells, nearby_pheromes)) {
            if (count2 == MAX_TRIES) {
                nextPoint = new Point(0,0);
                break;
            }
            nextPoint = new Point(nextPoint.x * NEXT_REDUCE_MOVE, nextPoint.y * NEXT_REDUCE_MOVE);
            count2++;
        }

        return new Move(nextPoint, memory);
    }

    /* Synchronized circle strategy
       Memory byte:
        - 2 bits strategy
        - 6 bits direction counter
     */
    private Move sync(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        Point currPos = player_cell.getPosition();
        byte nextMemory = memory;
        Move nextMove = null;
        int currCount = memory & 0b00111111;

        // reproduce if possible
        if (player_cell.getDiameter() >= 2) {
            // increment direction count and reproduce
            currCount = (currCount + 1) % SYNC_MAX_DIR_COUNT;
            byte countBits = (byte) (currCount & 0b00111111);
            nextMemory = (byte) (nextMemory & 0b11000000);
            nextMemory = (byte) (nextMemory | countBits);
            return new Move(true, nextMemory, nextMemory);
        }

        // if sees an enemy cell, evolve to border behavior
        int enemyCount = 0;
        Iterator<Cell> cell_it = nearby_cells.iterator();
        while (cell_it.hasNext()) {
            Cell c = cell_it.next();
            Point cPos = c.getPosition();
            if (currPos.distance(cPos) >= SYNC_SIGHT_THRESHOLD) {
                continue;
            }
            if (c.player != player_cell.player) {
                enemyCount++;
            }
            if (enemyCount >= SYNC_ENEMY_COUNT_TRIGGER) {
                nextMemory = (byte) 0b10000000;
                return scout(player_cell, nextMemory, nearby_cells, nearby_pheromes);
            }
        }

        // get the next move direction (the "base" move)
        Point nextBaseVector = syncGetVectorFromCount(currCount);
        double nextBaseRadians = Math.atan2(nextBaseVector.y, nextBaseVector.x);
        double nextBaseDegrees = Math.toDegrees(nextBaseRadians);

        // if you're big and you're on the edge of a large group, evolve behavior
        if (player_cell.getDiameter() >= SYNC_DIAMETER_TRIGGER) {
            double outsideDegrees = (360 + (nextBaseDegrees + 90) % 360) % 360;
            double outDegMin = (360 + (outsideDegrees - 90) % 360) % 360;
            double outDegMax = (360 + (outsideDegrees - 90) % 360) % 360;
            double insideDegrees = (360 + (nextBaseDegrees - 90) % 360) % 360;
            double inDegMin = (360 + (outsideDegrees - 90) % 360) % 360;
            double inDegMax = (360 + (outsideDegrees - 90) % 360) % 360;
            boolean outsideClear = true;
            boolean insideCrowded = false;

            int outsideCount = 0;
            int insideCount = 0;
            cell_it = nearby_cells.iterator();
            while (cell_it.hasNext()) {
                Cell c = cell_it.next();
                Point neighborPos = c.getPosition();
                if (currPos.distance(neighborPos) >= SYNC_SIGHT_THRESHOLD) {
                    continue;
                }
                Point vectorToNeighbor = new Point(neighborPos.x - currPos.x, neighborPos.y - currPos.y);
                double neighborRadians = Math.atan2(vectorToNeighbor.y, vectorToNeighbor.x);
                double neighborDegrees = Math.toDegrees(neighborRadians);
                neighborDegrees = (360 + neighborDegrees % 360) % 360; // normalize

                // only consider friendly cells
                if (c.player == player_cell.player) {
                    if (syncAngleIsBetween(inDegMin, inDegMax, neighborDegrees)) {
                        insideCount++;
                    }
                    if (syncAngleIsBetween(outDegMin, outDegMax, neighborDegrees)) {
                        outsideClear = false;
                        break;
                    }
                }
            } // end iterate through all neighbor cells
            if (insideCount >= SYNC_INSIDE_CROWDED_THRESHOLD) {
                insideCrowded = true;
            }
            if (insideCrowded && outsideClear) {
                nextMemory = (byte) 0b01000000;
                return scout(player_cell, nextMemory, nearby_cells, nearby_pheromes);
            }
        } // end check if inside is crowded and outside clear to evolve
        
        // get normalized min angle and max angle for the arc to look in
        double degMin = (360 + (nextBaseDegrees - SYNC_LOOK_ARC/2) % 360) % 360;
        double degMax = (360 + (nextBaseDegrees + SYNC_LOOK_ARC/2) % 360) % 360;

        // get all neighbors that fall in the arc of movement (+/-60 degrees from base)
        Vector<Double> neighborAngles = new Vector<Double>(); // in degrees
        cell_it = nearby_cells.iterator();
        while (cell_it.hasNext()) {
            Cell c = cell_it.next();
            Point neighborPos = c.getPosition();
            if (currPos.distance(neighborPos) >= SYNC_SIGHT_THRESHOLD) {
                continue;
            }
            Point vectorToNeighbor = new Point(neighborPos.x - currPos.x, neighborPos.y - currPos.y);
            double neighborRadians = Math.atan2(vectorToNeighbor.y, vectorToNeighbor.x);
            double neighborDegrees = Math.toDegrees(neighborRadians);
            neighborDegrees = (360 + neighborDegrees % 360) % 360; // normalize
            if (syncAngleIsBetween(degMin, degMax, neighborDegrees)) {
                neighborAngles.add(neighborDegrees);
            }
        } // end while loop through nearby_cells

        // get widest angle within the look arc
        neighborAngles.add(degMax); // add the max angle into the vector
        neighborAngles.sort(new Comparator<Double>() {
                // sorts the angles in order of difference from min angle to max angle
                public int compare(Double d1, Double d2) {
                    double a = (360 + (d1-degMin) % 360) % 360;
                    double b = (360 + (d2-degMin) % 360) % 360;
                    if (a == b) return 0;
                    else if (a < b) return -1;
                    else return 1;
                }
            });
        double widestAngle = 0;
        double prevAngle = degMin;
        double startAngle = 0;
        double endAngle = 0;
        for (double d : neighborAngles) {
            double diff = (360 + (d - prevAngle) % 360) % 360;
            if (diff > widestAngle) {
                widestAngle = diff;
                startAngle = prevAngle;
                endAngle = d;
            }
            prevAngle = d;
        }

        // get center of the widest angle to move in that direction
        double nextMoveDegrees = (360 + (endAngle - widestAngle/2) % 360) % 360;
        double nextMoveRadians = Math.toRadians(nextMoveDegrees);
        double dx = Cell.move_dist * Math.cos(nextMoveRadians);
        double dy = Cell.move_dist * Math.sin(nextMoveRadians);
        Point nextVector = new Point(dx, dy);
        if (collides(player_cell, nextVector, nearby_cells, nearby_pheromes)) {
            // collision, move to widest arc general
            double[] arc = getWidestAngle(player_cell, nearby_cells, nearby_pheromes);
            double arcMin = arc[0];
            double arcMax = arc[1];
            widestAngle = (360 + (arcMax - arcMin) % 360) % 360;
            nextMoveDegrees = (360 + (arcMax - widestAngle/2) % 360) % 360;
            nextMoveRadians = Math.toRadians(nextMoveDegrees);
            dx = Cell.move_dist * Math.cos(nextMoveRadians);
            dy = Cell.move_dist * Math.sin(nextMoveRadians);
            nextVector = new Point(dx, dy);
            int tries = 0;
            while (collides(player_cell, nextVector, nearby_cells, nearby_pheromes)) {
                if (tries >= MAX_TRIES) {
                    nextVector = new Point(0, 0);
                    break;
                }
                nextVector = new Point(nextVector.x * NEXT_REDUCE_MOVE, nextVector.y * NEXT_REDUCE_MOVE);
                tries++;
            }
        }

        // increment count and write to new memory
        currCount = (currCount + 1) % SYNC_MAX_DIR_COUNT;
        byte countBits = (byte) (currCount & 0b00111111);
        nextMemory = (byte) (nextMemory & 0b11000000);
        nextMemory = (byte) (nextMemory | countBits);
        nextMove = new Move(nextVector, nextMemory);

        return nextMove;
    }

    // finds if an angle is between a min and a max angle
    private boolean syncAngleIsBetween(double degMin, double degMax, double angle) {
        if (degMin < degMax) {
            return (angle >= degMin && angle <= degMax);
        }
        else {
            return (angle >= degMin || angle <= degMax);
        }
    }

    private Point syncGetVectorFromCount(int count) {
        double theta = Math.toRadians((360 / (double)SYNC_MAX_DIR_COUNT) * count);
        double dx = Cell.move_dist * Math.cos(theta);
        double dy = Cell.move_dist * Math.sin(theta);
        return new Point(dx, dy);
    }
    
    private Move border(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        return new Move(new Point(0,0), memory);        
    }

    /*
     * T-cell strategy: move toward big enemies
     */
    private Move tcell(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        Move move = null;

        Cell enemy = findBiggestEnemy(player_cell, nearby_cells, nearby_pheromes);
        if (enemy == null) {
            return scout(player_cell, memory, nearby_cells, nearby_pheromes);
        }

        Point enemyPos = enemy.getPosition();
        Point playerPos = player_cell.getPosition();
        Point vector = new Point(enemyPos.x - playerPos.x, enemyPos.y - playerPos.y);
        double magnitude = getMagnitude(vector.x, vector.y);

        double moveDist = 0.5*player_cell.getDiameter()*1.01 + 0.5*enemy.getDiameter() + 0.00011;
        Point newDest = new Point(vector.x * moveDist / magnitude, vector.y * moveDist / magnitude);

        return new Move(newDest, memory);
    }

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
    	double theta = Math.toRadians(30 * (double)arg);
    	double dx = Cell.move_dist * Math.cos(theta);
    	double dy = Cell.move_dist * Math.sin(theta);
    	return new Point(dx, dy);
    }

    /*
     * Returns an angle in radians.
     */
    private double getAngleFromVector(Point vector) {
        return Math.atan2(vector.y, vector.x);
    }

    /*
     * Get magnitude of a vector given x,y.
     */
    private double getMagnitude(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    /*
     * Get the positive version of the angle.
     */
    private double getPositiveAngle(double angle, String unit) {
        double adj = 0;
        if (unit == "d")
            adj = 360;
        else
            adj = 2 * Math.PI;
        return (angle > 0 ? angle : angle + adj);
    }

    /*
     * Get the negative version of the angle.
     */
    private double getNegativeAngle(double angle, String unit) {
        double adj = 0;
        if (unit == "d")
            adj = 360;
        else
            adj = 2 * Math.PI;
        return (angle > 0 ? angle : angle - adj);
    }


    /*
     * Get the tangent vectors to the other cell.
     */
    private Point[] getTangentVectors(Point self, Point other, double radius) {
        Point hypot = new Point(other.x - self.x, other.y - self.y);
        // find the angle to center and add/subtract the angle to the tangent
        double mainAngle = getAngleFromVector(hypot);
        double deltAngle = Math.asin(radius / getMagnitude(hypot.x, hypot.y));
        // magnitude of tangent vectors
        double magnitude = getMagnitude(hypot.x, hypot.y) * Math.cos(deltAngle);

        Point upperVector = new Point(magnitude * Math.cos(mainAngle + deltAngle),
                                      magnitude * Math.sin(mainAngle + deltAngle));
        Point lowerVector = new Point(magnitude * Math.cos(mainAngle - deltAngle),
                                      magnitude * Math.sin(mainAngle - deltAngle));

        return new Point[] {lowerVector, upperVector};
    }



    private int getStrategy(byte memory) {
        int strategy = (memory >> 6) & 0b11;
        return strategy;
    }

    /*
     * Looks at nearby cells and finds the biggest enemy
     */
    private Cell findBiggestEnemy(Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        Cell biggestEnemy = null;
        double maxDiam = 0;
        boolean approachable = false;
        for (Cell c : nearby_cells) {
            if (c.player == player_cell.player || player_cell.distance(c) > player_cell.getDiameter() * 2)
                continue;
            if (c.getDiameter() > maxDiam) {
                biggestEnemy = c;
                maxDiam = c.getDiameter();
            }
        }

        return biggestEnemy;
    }

    /*
     * Same as method below but sets a default [0,360] range.
     */
    private double[] getWidestAngle(Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        return getWidestAngle(player_cell, nearby_cells, nearby_pheromes, new double[] {0.0,360.0});
    }

    /*
     * Args:
     *  bounds: [lower, upper] angle bounds that we're looking at
     * Normalizes angles to a [0,360] range.
     * Returns an array of doubles containing the lower and upper angles that border the widest angle
     * e.g. if the widest angle is 30 degrees between 10 and 40 degrees,
     *      we would return [10.0, 40.0]
     */
    private double[] getWidestAngle(Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes, double[] bounds) {
        boolean bounded = (bounds[1] - bounds[0] != 360);
        double adjustment = (bounds[0] < bounds[1] ? bounds[0] : getNegativeAngle(bounds[0], "d"));
        double lowerBound = 0;
        double upperBound = bounds[1] - adjustment;

        List<Point> neighbors = new ArrayList<Point>();
        Iterator<Cell> cell_it = nearby_cells.iterator();
        Point currentPosition = player_cell.getPosition();

        // get all nearby cell position vectors
        while (cell_it.hasNext()) {
            Cell curr = cell_it.next();
            if (curr.player == player_cell.player) {
                if (player_cell.distance(curr) > FRIENDLY_AVOID_DIST) // ignore far friendly cells
                    continue;
            }
            else if (player_cell.distance(curr) > AVOID_DIST) // don't worry about far away cells
                continue;

            Point currPos = curr.getPosition();
            double currRad = curr.getDiameter() / 2;
            Point[] vectors = getTangentVectors(currentPosition, currPos, currRad);

            for (Point v : vectors) {
                double adjustedAngle = (getPositiveAngle(Math.toDegrees(getAngleFromVector(v)), "d") - adjustment) % 360;
                if (adjustedAngle > lowerBound && adjustedAngle < upperBound) {
                    neighbors.add(v);
                }
            }
        }
        // do pheromes too why not
        Iterator<Pherome> ph_it = nearby_pheromes.iterator();
        while (ph_it.hasNext()) {
            Pherome curr = ph_it.next();
            if (curr.player == player_cell.player)
                continue;
            if (player_cell.distance(curr) > PH_AVOID_DIST)
                continue;
            Point currPos = curr.getPosition();
            Point v = new Point(currPos.x - currentPosition.x, currPos.y - currentPosition.y);
            double adjustedAngle = (getPositiveAngle(Math.toDegrees(getAngleFromVector(v)), "d") - adjustment) % 360;
            if (adjustedAngle > lowerBound && adjustedAngle < upperBound) {
                neighbors.add(v);
            }
        }

        if (neighbors.size() < 1) { // case: no cells
            return null;
        } else if (neighbors.size() == 1 && !bounded) { // case: if only one thing nearby
            // just move in the opposite direction
            Point other = neighbors.get(0);
            double otherVector = getPositiveAngle(Math.toDegrees(getAngleFromVector(other)), "d");
            return new double[] {otherVector, otherVector + 360};
        } else { // case: cells too close, get the widest angle
            neighbors.sort(new Comparator<Point>() {
                public int compare(Point v1, Point v2) {
                    double a1 = getPositiveAngle(Math.toDegrees(getAngleFromVector(v1)), "d");
                    double a2 = getPositiveAngle(Math.toDegrees(getAngleFromVector(v2)), "d");

                    if (a1 == a2) return 0;
                    else if (a1 < a2) return -1;
                    else return 1;
                }
            });

            // find widest angle
            double lowerAngle = 0; // smaller angle that borders the widest angle
            double widestAngle = 0;
            for (int i = 0; i <= neighbors.size(); i++) {
                double a1;
                double a2;
                if (i == 0) {
                    a1 = (bounded ? lowerBound : getAngleFromVector(neighbors.get(neighbors.size()-1)));
                } else {
                    a1 = getPositiveAngle(getAngleFromVector(neighbors.get(i-1)), "r");
                }
                if (i == neighbors.size()) {
                    if (bounded) {
                        a2 = upperBound;
                    } else {
                        continue;
                    }
                } else {
                    a2 = getPositiveAngle(getAngleFromVector(neighbors.get(i)), "r");
                }
                double deltAngle = Math.abs(a2 - a1);
                if (deltAngle > widestAngle) {
                    widestAngle = deltAngle;
                    lowerAngle = a1;
                }
            }

            return new double[] {lowerAngle, lowerAngle + widestAngle};
        }

    }
}
