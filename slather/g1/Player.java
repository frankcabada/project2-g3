package slather.g1;

import slather.sim.Cell;
import slather.sim.Move;
import slather.sim.Pherome;
import slather.sim.Point;

import java.util.*;

public class Player implements slather.sim.Player {

    private int trailLength;
    private double distanceVisible;
    private static final double MAXIMUM_MOVE = 1.0;
    private static final double THRESHOLD_DISTANCE = 2.0;
    private int side_length;
    private Random gen;

    public void init(double d, int t, int side_length) {
        this.trailLength = t;
        this.distanceVisible = d;
        this.side_length = side_length;
        this.gen = new Random();
    }


    public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        if (player_cell.getDiameter() >= 2) // reproduce whenever possible
            return new Move(true, (byte)0, (byte)(trailLength/2));


        //Get number of enemies and friendlies and get closest enemy
        int numEnemies = 0;
        int numFriendlies = 0;
        Cell closestEnemy = null;
        for(Cell neighbor : nearby_cells) {
            if(neighbor.player != player_cell.player) {
                numEnemies++;
                double distance = neighbor.distance(player_cell);
                if(closestEnemy == null || (distance <= 1 && distance < closestEnemy.distance(player_cell)))
                    closestEnemy = neighbor;
            } else {
                numFriendlies++;
            }
        }

        if(numFriendlies > 3 && numEnemies > 0) {
            Point move = getNormalizedVector(player_cell.getPosition(), closestEnemy.getPosition(), player_cell.getDiameter()).invert().toPoint();
            return new Move(move, memory);
        }

        // Move hexMove = hexagonMethod(player_cell, memory);
        // boolean enemies = numEnemies > 0;
        // if(!enemies)
        // 	for (Pherome p: nearby_pheromes){
        // 		if (p.player != player_cell.player) {
        // 			enemies = true;
        // 			break;
        // 		}
        // 	}

        // for (Cell c: nearby_cells)
        // 	enemies = true;
        // if (enemies || collides(player_cell, hexMove.vector, nearby_cells, nearby_pheromes) || trailLength < 4)
        // 	return getVectorBasedMove(player_cell, memory, nearby_cells, nearby_pheromes);
        // return hexMove;

        return getVectorBasedMove(player_cell, memory, nearby_cells, nearby_pheromes);
    }

    public Move hexagonMethod(Cell player_cell, byte memory){
        Point myPosition = player_cell.getPosition();
        boolean reverse = false;
        int polySide = (int) memory;
        if (polySide < 0){
            reverse = true;
            polySide = (int)switchDirection(memory);
        }
        double angle = polySide*getAngle();
        Point newPlace = new Point(myPosition.x+Math.cos(angle), myPosition.y+Math.sin(angle));
        Vector v = getNormalizedVector(myPosition, newPlace, player_cell.getDiameter());
        if (reverse){
            v.invert();
            polySide = (int) switchDirection((byte)((polySide-1)%getTrailLength()));
        } else{
            polySide = (polySide+1)%getTrailLength();
        }
        //Get final destination point
        Point finalPoint = v.add(myPosition);

        //Make sure point falls within MAXIMUM_MOVE
        double distance = finalPoint.distance(myPosition);
        if(distance > MAXIMUM_MOVE) {
            v = v.multiply(MAXIMUM_MOVE/distance);
        }
        return new Move(v.toPoint(), (byte)polySide);
    }

    private Vector getNormalizedVector(Point myPosition, Point otherPosition, double diameter) {
        //Check distance between cell and neighboring cell. If greater than theshold, add vector
        Vector vector = new Vector(myPosition, otherPosition);
        if(Math.abs(myPosition.x - otherPosition.x) > diameter + distanceVisible)
            vector = new Vector(vector.getX() - side_length, vector.getY());
        if(Math.abs(myPosition.y - otherPosition.y) > diameter + distanceVisible)
            vector = new Vector(vector.getX(), vector.getY() - side_length);
        return vector;
    }

    private List<Vector> getAllNeighborVectors(Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        List<Vector> vectors = getNeighborsWithinDistance(player_cell, nearby_cells, nearby_pheromes, 0.5);
        if (vectors.isEmpty())
            vectors = getNeighborsWithinDistance(player_cell, nearby_cells, nearby_pheromes, 1.0);
        if (vectors.isEmpty())
            vectors = getNeighborsWithinDistance(player_cell, nearby_cells, nearby_pheromes, 1.5);
        if (vectors.isEmpty())
            vectors = getNeighborsWithinDistance(player_cell, nearby_cells, nearby_pheromes, 2.0);

        return vectors;
    }

    private List<Vector> getNeighborsWithinDistance(Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes, double dist){
        List<Vector> vectors = new ArrayList<>();
        //Get list of vectors of nearby neighbors
        Point myPosition = player_cell.getPosition();
        for(Cell c : nearby_cells) {
            Point otherPosition = c.getPosition();
            if (player_cell.distance(c) < dist)
                vectors.add(getNormalizedVector(myPosition, otherPosition, player_cell.getDiameter()));

        }
        // System.out.println("nearby: "+nearby_cells.size()+"vector length: "+vectors.size());
        //Also get list of nearby pheremones
        for(Pherome p : nearby_pheromes) {
            if(player_cell.player != p.player) {
                Point otherPosition = p.getPosition();
                if (player_cell.distance(p) < dist)
                    vectors.add(getNormalizedVector(myPosition, otherPosition, player_cell.getDiameter()));
            }
        }

        return vectors;
    }

    private double getAngle(){
        int val = getTrailLength();
        if (val == 0)
            val = 1;
        return 2.0*Math.PI/getTrailLength();
    }
    private int getTrailLength(){
        if (trailLength > 15)
            return 15;
        return trailLength;
    }

    private List<Vector> validHoneycombPositions() {
        return null;
    }

    private Move getVectorBasedMove(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        Point myPosition = player_cell.getPosition();
        List<Vector> vectors = getAllNeighborVectors(player_cell, nearby_cells, nearby_pheromes);

        //Add all vectors together
        Vector finalVector = new Vector(0, 0);
        for(Vector v : vectors) {
            finalVector = finalVector.add(v);
        }

        //Get inverse of direction
        finalVector = finalVector.invert();

        //Get final destination point
        Point finalPoint = finalVector.add(myPosition);

        //Make sure point falls within MAXIMUM_MOVE
        double distance = finalPoint.distance(myPosition);
        if(distance > MAXIMUM_MOVE) {
            finalPoint = finalVector.multiply(MAXIMUM_MOVE/distance).add(myPosition);
        }

        // if all tries fail, just chill in place
        return new Move((new Vector(myPosition, finalPoint)).toPoint(), switchDirection(memory));
    }

    private byte switchDirection(byte memory){
        int side = ((int)memory)^(-128);
        return (byte)(side);
    }

    //CODE FROM G0 COLLIDES METHOD
    // check if moving player_cell by vector collides with any nearby cell or hostile pherome
    private boolean collides(Cell player_cell, Point vector, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        Point destination = player_cell.getPosition().move(vector);
        for(Cell other : nearby_cells) {
            if ( destination.distance(other.getPosition()) < 0.5*player_cell.getDiameter() + 0.5*other.getDiameter() + 0.00011)
                return true;
        }
        for(Pherome other : nearby_pheromes)
            if (other.player != player_cell.player && destination.distance(other.getPosition()) < 0.5*player_cell.getDiameter() + 0.0001)
                return true;
		return false;
    }

}
