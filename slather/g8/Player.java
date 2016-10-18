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

    private int centerX;
    private int centerY;

    private static final int NUMDIRECTIONS = 4;   // CONSTANTS - number of directions
    private static final int MAX_SHAPE_MEM = 4;   // CONSTANTS - maximum shape size (in bits)
    private static final int MIN_SHAPE_MEM = 2;   // CONSTANTS - minimum shape size (in bits)
    private static final int DURATION_CAP = 4;   // CONSTANTS - cap on traveling
    private static final int AVOID_DIST = 2;     // CONSTANTS - for random walker: avoid cells within this radius
    private static final int PH_AVOID_DIST = 2;  // CONSTANTS - avoid pheromes within this radius
    private static final int FRIENDLY_AVOID_DIST = 5; // CONSTANTS - for random walker: turn away from friendlies that are in this radius
    private static final double MAX_MOVEMENT = 1; // CONSTANTS - maximum movement rate (assumed to be 1mm?)
    private static final int MAX_SIGHT_TRIGGER = 3; // CONSTANTS - radius beyond which we dont care about cells to look for widest angle
    private static final int MAX_TRIES = 30; // CONSTANT - max tries to reduce vector
    private static final double INWARD_MOVE_DIST = 0.95; // CONSTANT - amount to move inwards to the center
    private static final int EXPLODE_MAX_DIST = 16; // CONSTANT - max counter for explode strategy
    private static final int COUNTDOWN_TO_EXPLODE = 3; // CONSTANT - num generations of clustering before they all explode
    private static final int EXPLODE_AVOID = 2; // CONSTANT - explode only looks in this radius to avoid cells
    private static final double NEXT_REDUCE_MOVE = 0.7; // CONSTANT - factor to reduce movement if collision happens
    private int SHAPE_MEM_USAGE; // CONSTANTS - calculate this based on t
    private int EFFECTIVE_SHAPE_SIZE; // CONSTANTS - actual number of sides to our shape

    public void init(double d, int t, int sideLength) {
        gen = new Random();
	    this.d = d;
	    this.t = t;
        this.sideLength = sideLength;

        centerX = (int)(sideLength / 2);
        centerY = (int)(sideLength / 2);

        SHAPE_MEM_USAGE = (int) Math.ceil( Math.log(t)/Math.log(2) );
        SHAPE_MEM_USAGE = Math.max(MIN_SHAPE_MEM, SHAPE_MEM_USAGE);
        SHAPE_MEM_USAGE = Math.min(MAX_SHAPE_MEM, SHAPE_MEM_USAGE);
        EFFECTIVE_SHAPE_SIZE = (int) Math.min( Math.pow(2, SHAPE_MEM_USAGE), t );
        EFFECTIVE_SHAPE_SIZE = (int) Math.max( 4, EFFECTIVE_SHAPE_SIZE);
        // TODO: put minimum for small t?
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

        /*
            Set multiple strategies
            1) first is to move to the center and create a cluster bomb there
            2) the second strategy people would start moving around becoming explorers
            3) the third strategy people would create cluster
            4) 4th would be end game strategy
        */

        if (strategy == 0) {
            //System.out.println("MOVE TO CENTER");
            nextMove = moveToCenter(player_cell, memory, nearby_cells, nearby_pheromes);
        } else if (strategy == 1) {
            //System.out.println("SCOUT");
            nextMove = scout(player_cell, memory, nearby_cells, nearby_pheromes);
        } else if (strategy == 2) {
            //System.out.println("CLUSTER");
            nextMove = cluster(player_cell, memory, nearby_cells, nearby_pheromes);
        } else if (strategy == 3) {
            nextMove = explode(player_cell, memory, nearby_cells, nearby_pheromes);
            Point nextVector = nextMove.vector;
            boolean nextReproduce = nextMove.reproduce;
            byte nextMemory = nextMove.memory;
            byte inOrOut = (byte) (memory & 0b00000001);
            // if on OUTWARD movement, if cant move, revert to scout
            if (!nextReproduce && nextVector.x == 0 && nextVector.y == 0 && inOrOut == 0) {
                System.out.println("REVERT TO SCOUT");
                memory = (byte) 0b01000001;
                nextMove = scout(player_cell, memory, nearby_cells, nearby_pheromes);
                byte newMemory = nextMove.memory;
            }
        } else {
            nextMove = new Move(new Point(0,0), memory);
        }
        return nextMove;
    }


    private Move moveToCenter(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {

        if (player_cell.getDiameter() >= 2) {
           memory = (byte)(memory & 0b11001111);
           return new Move(true, (byte) (memory | 0b10000000), (byte) (memory | 0b10000000));
        }

        // Get the player's position
        double pcx = player_cell.getPosition().x;
        double pcy = player_cell.getPosition().y;

        // get the difference vector from the center, and the distance
        double diffx = (double)(centerX - pcx);
        double diffy = (double)(centerY - pcy);
        double dist = player_cell.getPosition().distance(new Point(centerX, centerY));

        // normalize the vector to 1 mm
        diffx = diffx / dist;
        diffy = diffy / dist;

        Point nextPoint = new Point(diffx, diffy);
        Move nextMove = new Move(nextPoint, memory);

        double vectx = 0.0;
        double vecty = 0.0;
        int fcc = 0;
        for (Cell c : nearby_cells) {
            if (c.player != player_cell.player) continue;
            if (player_cell.distance(c) > ((player_cell.getDiameter() * 1.01))) continue;
            vectx = c.getPosition().x - player_cell.getPosition().x;
            vecty = c.getPosition().y - player_cell.getPosition().x;
            fcc++;
        }
        if (fcc > 0) {
            double avg_x = vectx / (double)fcc;
            double avg_y = vecty / (double)fcc;
            diffx = -avg_y;
            diffy = -avg_x;
        }


        int counter = 0;
        while (collides(player_cell, nextMove.vector, nearby_cells, nearby_pheromes)) {
            diffx = diffx * 0.5;
            diffy = diffy * 0.5;
            nextPoint = new Point(diffx, diffy);
            nextMove = new Move(nextPoint, memory);
            counter++;
            if (counter >= 10) break;
        }
        if (counter >= 10) {
            nextMove = new Move(new Point(0,0), memory);
        }
        return nextMove;
    }

    /* Random walker strategy:
       Uses "widest angle" strategy inspired by g5 (& others?)
       Move away from cells that are too close (specified by AVOID_DIST)
       If no closeby friendly cells to avoid, act like default player (move in straight lines)
       Memory byte:
        - 2 bits strategy
        - 2 bits unused
        - 4 bits previous angle (must be > 0)
    */
    private Move scout(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        if (player_cell.getDiameter() >= 2) {
           return new Move(true, memory, memory);
        }

        int prevAngle = memory & 0b00001111;
        Point currentPosition = player_cell.getPosition();
        Move nextMove = null;

        List<Point> neighbors = new ArrayList<Point>();
        Iterator<Cell> cell_it = nearby_cells.iterator();

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
            neighbors.addAll(Arrays.asList(vectors));
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
            neighbors.add(new Point(currPos.x - currentPosition.x, currPos.y - currentPosition.y));
        }

        if (neighbors.size() < 1) { // case: no cells
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
        } else if (neighbors.size() == 1) { // case: if only one thing nearby
            // just move in the opposite direction
            Point other = neighbors.get(0);
            double magnitude = getMagnitude(currentPosition.x - other.x, currentPosition.y - other.y);
            double newX = Cell.move_dist * (currentPosition.x - other.x) / magnitude;
            double newY = Cell.move_dist * (currentPosition.y - other.y) / magnitude;

            Point newVector = new Point(newX, newY);
            int newAngle = (int) (getPositiveAngle(Math.toDegrees(getAngleFromVector(newVector)), "d") / 30);

            byte angleBits = (byte) (newAngle & 0b00001111);
            byte newMemory = (byte) (memory & 0b11110000);
            newMemory = (byte) (newMemory | angleBits);
            nextMove = new Move(newVector, newMemory);

        } else { // case: cells too close, move in direction of the widest angle
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
            double lowerAngle = prevAngle; // smaller angle that borders the widest angle
            double widestAngle = 0;
            for (int i = 0; i < neighbors.size(); i++) {
                double a1;
                if (i == 0) {
                    a1 = getAngleFromVector(neighbors.get(neighbors.size()-1));
                } else {
                    a1 = getPositiveAngle(getAngleFromVector(neighbors.get(i-1)), "r");
                }
                double a2 = getPositiveAngle(getAngleFromVector(neighbors.get(i)), "r");
                double deltAngle = Math.abs(a2 - a1);
                if (deltAngle > widestAngle) {
                    widestAngle = deltAngle;
                    lowerAngle = a1;
                }
            }

            // calculate direction to go (aim for the "middle" of the widest angle)
            double destAngle = (lowerAngle + widestAngle) / 2;

            double destX = Cell.move_dist * Math.cos(destAngle);
            double destY = Cell.move_dist * Math.sin(destAngle);

            /* this is probably not necessary anymore?
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
            */

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
      4 bits generation count
     */
    private Move cluster(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        // if can reproduce, do so and increment the generation
        if (player_cell.getDiameter() >= 2) {
            int generation = memory & 0b00001111;
            generation++;

            if (generation == COUNTDOWN_TO_EXPLODE) {
                memory = (byte) 0b11000000;
                return new Move(true, memory, memory);
            }
            else {
                memory = (byte) (memory & 0b11110000); // clear old generation bits
                memory = (byte) (memory | generation); // set current generation bits
                return new Move(true, memory, memory);
            }
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

    private Move explode(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        Point currPos = player_cell.getPosition();
        Point nextVector = null;
        byte nextMemory = memory;
        Move nextMove = null;

        // get the difference vector from the center, and the distance
        double diffx = (double)(centerX - currPos.x);
        double diffy = (double)(centerY - currPos.y);
        double dist = player_cell.getPosition().distance(new Point(centerX, centerY));

        // normalize the vector to 1 mm
        diffx = diffx / dist;
        diffy = diffy / dist;

        Point toCenter = new Point(diffx, diffy);
        Point awayCenter = new Point(-toCenter.x, -toCenter.y);
        byte inOrOut = (byte) (memory & 0b00000001);
        int currCount = (memory >> 1) & 0b00001111;
        int effT = Math.min(t/2 + 1, EXPLODE_MAX_DIST);
        if (currCount == 0) {
            nextMemory = (byte) (memory ^ 0b00000001);
            inOrOut = (byte) (nextMemory & 0b00000001);
        }

        // reproduce if possible, increment the count
        if (player_cell.getDiameter() >= 2) {
            currCount = (currCount + 1) % effT;
            byte countBits = (byte) ((currCount << 1) & 0b00011110);
            nextMemory = (byte) (nextMemory & 0b11100001);
            nextMemory = (byte) (nextMemory | countBits);
            return new Move(true, memory, memory);
        }

        if (inOrOut == 0) {
            // move OUTWARDS from the center
            double angleAwayCenter = Math.atan2(awayCenter.y, awayCenter.x);
            double degAwayCenter = Math.toDegrees(angleAwayCenter);
            degAwayCenter = (360 + degAwayCenter % 360) % 360;
            double degMin = (360 + (degAwayCenter - 120) % 360) % 360; // normalize
            double degMax = (360 + (degAwayCenter + 120) % 360) % 360; // normalize

            Vector<Double> neighborAngles = new Vector<Double>();
            Iterator<Cell> cell_it = nearby_cells.iterator();
            while (cell_it.hasNext()) {
                Cell c = cell_it.next();
                Point neighborPos = c.getPosition();
                if (currPos.distance(neighborPos) >= EXPLODE_AVOID) {
                    continue;
                }
                Point vectorToNeighbor = new Point(neighborPos.x - currPos.x, neighborPos.y - currPos.y);
                double neighborAngle = Math.atan2(vectorToNeighbor.y, vectorToNeighbor.x);
                double neighborDeg = Math.toDegrees(neighborAngle);
                neighborDeg = (360 + neighborDeg % 360) % 360;
                if (degMin < degMax) {
                    if (neighborDeg >= degMin && neighborDeg <= degMax) {
                        neighborAngles.add(neighborDeg);
                    }
                }
                else {
                    if (neighborDeg >= degMin || neighborDeg <= degMax) {
                        neighborAngles.add(neighborDeg);
                    }
                }
            }

            neighborAngles.add(degMax);
            neighborAngles.sort(new Comparator<Double>() {
                    // sorts the angles in order of closest to min angle to max angle
                    public int compare(Double d1, Double d2) {
                        double a = (360 + (d1-degMin) % 360) % 360;
                        double b = (360 + (d2-degMin) % 360) % 360;
                        if (a == b) return 0;
                        else if (a < b) return -1;
                        else return 1;
                    }
                });
            double maxArc = 0;
            double prevAngle = degMin;
            double arcStart = degMin;
            double arcEnd = 0;
            for (double d : neighborAngles) {
                double arc = Math.abs(d - prevAngle) % 360;
                if (arc > maxArc) {
                    maxArc = arc;
                    arcStart = prevAngle;
                    arcEnd = d;
                }
                prevAngle = d;
            }
            double angleMove = ((arcStart + arcEnd) / 2) + 180;
            if (arcStart <= arcEnd) {
                angleMove = (arcStart + arcEnd) / 2;
            }
            else {
                angleMove = ((arcStart + arcEnd) / 2) + 180;
            }
            angleMove++;
            double theta = Math.toRadians(angleMove);
            double dx = Cell.move_dist * Math.cos(theta);
            double dy = Cell.move_dist * Math.sin(theta);
            System.out.println("Deg away from center: " + degAwayCenter);
            System.out.println("Neighbor angles: " + neighborAngles.toString());
            System.out.println("largest arc: " + maxArc + ", (" + arcStart + ", " + arcEnd);
            System.out.println("Move to: " + angleMove%360);

            nextVector = new Point(dx, dy);
            int tries = 0;
            while (collides(player_cell, nextVector, nearby_cells, nearby_pheromes)) {
                if (tries == MAX_TRIES) {
                    nextVector = new Point(0,0);
                    break;
                }
                nextVector = new Point(nextVector.x * NEXT_REDUCE_MOVE, nextVector.y * NEXT_REDUCE_MOVE);
                tries++;
            }

        } // end inOrOut == 0
        else {
            // move INWARDS toward center, less than 1
            nextVector = new Point(toCenter.x * INWARD_MOVE_DIST, toCenter.y * INWARD_MOVE_DIST);
            int tries = 0;
            while(collides(player_cell, nextVector, nearby_cells, nearby_pheromes)) {
                if (tries == MAX_TRIES) {
                    nextVector = new Point(0,0);
                    break;
                }
                nextVector = new Point(nextVector.x * NEXT_REDUCE_MOVE, nextVector.y * NEXT_REDUCE_MOVE);
                tries++;
            }
        }
        currCount = (currCount + 1) % effT;
        byte countBits = (byte) ((currCount << 1) & 0b00011110);
        nextMemory = (byte) (nextMemory & 0b11100001);
        nextMemory = (byte) (nextMemory | countBits);
        nextMove = new Move(nextVector, nextMemory);
        int strat = getStrategy(nextMemory);
        String s = String.format("%8s", Integer.toBinaryString(memory & 0xFF)).replace(' ','0');
        if (strat != 3) {
            System.out.println("New memory set to: " + s);
        }
        return nextMove;
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
        int strategy = (memory >> 6) & 0b11;
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
