package slather.h3;

import slather.sim.GridObject;
import slather.sim.Cell;
import slather.sim.Point;
import slather.sim.Move;
import slather.sim.Pherome;
import java.util.*;

public class Player implements slather.sim.Player {

	private Random gen;
    private double d;
    private int t;
    private int side_length;
    private final int CLOSE_RANGE = 2;
    private final int ANGLE_PRECISION = 2;

    public void init(double d, int t, int side_length) {
		gen = new Random();
		this.d = d;
		this.t = t;
		this.side_length = side_length;
    }

	public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {			
		
		if (player_cell.getDiameter() >= 2){ // reproduce whenever possible			
			byte memory1 = memory;
			int angle2 = memoryToAngleInt(memory);
			angle2 = (angle2 + 180)%360; //Angle should be opposite
			byte memory2 =  angleToByte(angle2);
			return new Move(true, memory1, memory2);
		}
		
		
		// Try safe moves nonstop
		for (int i = 0; i < 360/ANGLE_PRECISION; i+= ANGLE_PRECISION) {
			int angle = i;
			Point rand_vector = extractVectorFromAngle(angle);
			if (!collides(player_cell, rand_vector, nearby_cells, nearby_pheromes)){
				Cell newCell = new Cell(player_cell.getPosition(), player_cell.player, player_cell.getDiameter());
				if( MaxGrowableAmount(newCell, nearby_pheromes, nearby_cells) < 1.01 - .0001){
					memory = angleToByte(angle);
					return new Move(rand_vector, memory);
				}
			}
		}
		
		System.out.println("Couldn't find a safe move.");
	
		// If there was a collision, try a few random directions to go in until one doesn't collide
		for (int i = 0; i < 10; i++) {
			int rand_angle = gen.nextInt(360);
			Point rand_vector = extractVectorFromAngle(rand_angle);
			if (!collides(player_cell, rand_vector, nearby_cells, nearby_pheromes)){
				memory = angleToByte(rand_angle);
				return new Move(rand_vector, memory);
			}
		}

		// If no successful random direction, try reversing
		int reverseAngle = (memoryToAngleInt(memory) + 180)%360;
		memory = angleToByte(reverseAngle);
		Point rev_vector = extractVectorFromAngle(reverseAngle);
		if (!collides(player_cell, rev_vector, nearby_cells, nearby_pheromes)){
			return new Move(rev_vector, memory);
		}

		// if all tries fail, just chill in place
		return new Move(new Point(0, 0), (byte) 0);
	}
	

	
	// check if moving player_cell by vector collides with any nearby cell or
	// hostile pherome
	private boolean collides(Cell player_cell, Point vector, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		Iterator<Cell> cell_it = nearby_cells.iterator();
		Point destination = player_cell.getPosition().move(vector);
		while (cell_it.hasNext()) {
			Cell other = cell_it.next();
			if (destination.distance(other.getPosition()) < 0.5 * player_cell.getDiameter() + 0.5 * other.getDiameter()
					+ 0.00011)
				return true;
		}
		Iterator<Pherome> pherome_it = nearby_pheromes.iterator();
		while (pherome_it.hasNext()) {
			Pherome other = pherome_it.next();
			if (other.player != player_cell.player
					&& destination.distance(other.getPosition()) < 0.5 * player_cell.getDiameter() + 0.0001)
				return true;
		}
		return false;
	}

	// convert an angle (in 2-deg increments) to a vector with magnitude
	// Cell.move_dist (max allowed movement distance)
	private Point extractVectorFromAngle(int arg) {
		
		double theta = Math.toRadians(1 * (double) arg); //We need bigger circles!
		double dx = Cell.move_dist * Math.cos(theta);
		double dy = Cell.move_dist * Math.sin(theta);
		return new Point(dx, dy);
	}
	
    private Set<Cell> closeRangeCells(Cell source_cell, Set<Cell> all_cells) {
    	Set<Cell> closest_cells = new HashSet<Cell>();
		for (Cell other_cell : all_cells) {
			if (source_cell.distance(other_cell) < CLOSE_RANGE ) {
				closest_cells.add(other_cell);
			}
		}
		return closest_cells;
    }
    
	private int readf8Bits(byte memory){
		int f8bits = (memory & 0xff); 
		return f8bits;
	}
	
	private byte writeMemoryByte(int f8bits){
		if(f8bits < 0){
			throw new RuntimeException("f8bits is negative: [" + f8bits + "]");
		}
		if(f8bits > 255){
			throw new RuntimeException("f8bits is greater than 255: [" + f8bits + "]");
		}
		byte memory = (byte) ((f8bits & 0xff));
		return memory;
	}

	private int memoryToAngleInt(byte memory){
		return ANGLE_PRECISION * readf8Bits(memory);
	}
	
	private byte angleToByte(int angle){
		int intToWrite = angle/ANGLE_PRECISION;
		if(intToWrite < 0){
			throw new RuntimeException("angle/ANGLE_PRECISION is negative: [" + intToWrite + "]");
		}
		if(intToWrite > 255){
			throw new RuntimeException("angle/ANGLE_PRECISION is greater than 255: [" + intToWrite + "]");
		}
		return (byte) (intToWrite & 0xff);
	}
	

	private Point extractVectorFromAngleWithScalar(int arg, float f) {
		double theta = Math.toRadians(1 * (double) arg); //We need bigger circles!
		double dx = f*Cell.move_dist * Math.cos(theta);
		double dy = f*Cell.move_dist * Math.sin(theta);
		return new Point(dx, dy);
	}
	
	private boolean vectorSafeForReproduction(Point vector, Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes){
		//Test if the vector is even a valid move in the first place
		if(collides(player_cell, vector, nearby_cells, nearby_pheromes))
			return false;
		
		Point newLoc = player_cell.getPosition().move(vector);
		double new_diameter = player_cell.getDiameter() * 1.01;
		
		Iterator<Pherome> pherome_it = nearby_pheromes.iterator();
		while (pherome_it.hasNext()) {
		    Pherome next = pherome_it.next();
		    if (next.player != player_cell.player){
		    	if(newLoc.distance(next.getPosition()) < new_diameter*.5){
		    		return false;
		    	}
		    }
		}
		
		Iterator<Cell> cell_it = nearby_cells.iterator();
		while (cell_it.hasNext()) {
			Cell next = cell_it.next();
			if(newLoc.distance(next.getPosition()) + 1 < new_diameter*.5 + next.getDiameter()*5){
				return false;
			}
		}
				
		//If passed all those tests, return true;
		return true;
	}
	
	private int FindBestAngleMoveLazy(Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes, float scalar){
		boolean collides[] = new boolean[360/ANGLE_PRECISION];
		for(int i=0; i<360/ANGLE_PRECISION; i++){
			Point vector = extractVectorFromAngleWithScalar(i*ANGLE_PRECISION, scalar);
			if (collides(player_cell, vector, nearby_cells, nearby_pheromes)){
				collides[i] = true;
			}
			else{
				collides[i] = false;
			}
		}
		
		int falseSubStringLength[] = new int[360/ANGLE_PRECISION];
		for(int i=360/ANGLE_PRECISION - 1; i >=0; i--){
			if(i==360/ANGLE_PRECISION - 1)
				falseSubStringLength[i] = 0;
			else if(collides[i]==true)
				falseSubStringLength[i]=0;
			else{
				falseSubStringLength[i] = falseSubStringLength[i+1] + 1;
			}
		}
		
		//Check if all One
		boolean notAllZero = true;;
		for(int i=0; i<360/ANGLE_PRECISION - 1; i++){
			if(falseSubStringLength[i]==0)
				notAllZero = false;
		}
		if(notAllZero) //Return -1, tell cell to go by memory
			return -1;
		
		//Account for the wrap-around
		falseSubStringLength[360/ANGLE_PRECISION-1] = (collides[360/ANGLE_PRECISION-1]) ? 0 : 1;
		
		for(int i=360/ANGLE_PRECISION - 1; i >=0; i--){
			if(collides[i]==true)
				break;
			else{
				falseSubStringLength[i] += falseSubStringLength[0];
			}
		}
		
		int maxIndex=-1;
		for(int i=0; i<360/ANGLE_PRECISION; i++){
			if(maxIndex<0 && falseSubStringLength[i]!=0){
				maxIndex = i;
			}
			else if(maxIndex >=0 && falseSubStringLength[i] > falseSubStringLength[maxIndex]){
				maxIndex = i;
			}
		}
		
		ArrayList<Integer> maxScorers = new ArrayList<Integer>();
		for(int i=0; i<360/ANGLE_PRECISION; i++){
			if(falseSubStringLength[i] == falseSubStringLength[maxIndex]){
				maxScorers.add(i);
			}
		}
		maxIndex = maxScorers.get(gen.nextInt(maxScorers.size()));
		
		return ((maxIndex+falseSubStringLength[maxIndex]/2)*ANGLE_PRECISION)%360;
		
	}
	
	private double MaxGrowableAmount(Cell c, Set<Pherome> pheromes, Set<Cell> cells) {
		Iterator<Pherome> pherome_it = pheromes.iterator();
		double new_diameter = c.getDiameter() * 1.01;
		while (pherome_it.hasNext()) {
		    Pherome next = pherome_it.next();
		    if (next.player != c.player)
			new_diameter = Math.min(new_diameter, 2*c.distance(next));
		}
		Iterator<Cell> cell_it = cells.iterator();
		while (cell_it.hasNext()) {
		    Cell next = cell_it.next();
		    new_diameter = Math.min(new_diameter, 2*(c.distance(next) + 0.5*c.getDiameter()));
		}
		if (new_diameter > 2)
		    new_diameter = 2.0000001;
		double diameter = Math.max(new_diameter, c.getDiameter());
		return diameter/c.getDiameter();
    }
	
	private double distanceToClosestNeighbor(Cell player_cell, Set<Cell> neighbors){
		return player_cell.distance(getClosestCell(player_cell, neighbors));
	}
	
	private Cell getClosestCell(Cell player_cell, Set<Cell> neighbors){
		double maxDistance = Double.MAX_VALUE;
		Cell neighbor = null;
		for(Cell c : neighbors){
			double distance = player_cell.distance(c);
			if(distance < maxDistance){
				neighbor = c;
				maxDistance = distance;
			}
		}
		return neighbor;
	}
	
	//Threshold is negative if we want to ignore it
 	private TreeMap<Integer, Cell> generateMapOfNearbyCells(Cell player_cell, Set<Cell> nearby_cells, boolean ignoreOtherPlayer, boolean ignoreSamePlayer, float threshold){
		TreeMap<Integer, Cell> angleToCellMap = new TreeMap<Integer, Cell>();
		for(Cell c : nearby_cells){
			if(ignoreSamePlayer && (c.player == player_cell.player)){
				continue;
			}
			if(ignoreOtherPlayer && (c.player != player_cell.player)){
				continue;
			}
			if( (threshold > 0)  && (player_cell.distance(c) > threshold)	){
				continue;
			}
			
			double cX = c.getPosition().x;
			double cY = c.getPosition().y;
			double tX = player_cell.getPosition().x;
			double tY = player_cell.getPosition().y;
			double dX = cX-tX;
			double dY = cY-tY;
			int angle = (int)Math.toDegrees(Math.atan2(dY,dX));
			angle = (angle+360)%360;
			angleToCellMap.put((int)Math.toDegrees(angle), c);
		}
		return angleToCellMap;
	}
	//If threshold is negative, we ignore it
	private TreeMap<Integer, Pherome> generateMapOfNearbyPheromes(Cell player_cell, Set<Pherome> nearby_pheromes, boolean ignoreOtherPlayer, boolean ignoreSamePlayer, float threshold){
		TreeMap<Integer, Pherome> angleToPheromeMap = new TreeMap<Integer, Pherome>();
		for(Pherome p : nearby_pheromes){
			if(ignoreSamePlayer && (p.player == player_cell.player)){
				continue;
			}
			if(ignoreOtherPlayer && (p.player != player_cell.player)){
				continue;
			}
			if( (threshold > 0)  && (player_cell.distance(p) > threshold)	){
				continue;
			}
			
			double cX = p.getPosition().x;
			double cY = p.getPosition().y;
			double tX = player_cell.getPosition().x;
			double tY = player_cell.getPosition().y;
			double dX = cX-tX;
			double dY = cY-tY;
			int angle = (int)Math.toDegrees(Math.atan2(dY,dX));
			angle = (angle+360)%360;

			
			angleToPheromeMap.put((int)Math.toDegrees(angle), p);
			
			
		}
		return angleToPheromeMap;
	}
}