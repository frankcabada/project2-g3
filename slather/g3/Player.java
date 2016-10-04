package slather.g3;

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
    private final int CLOSE_RANGE_VISION = 5;

    public void init(double d, int t, int side_length) {
		gen = new Random();
		this.d = d;
		this.t = t;
		this.side_length = side_length;
    }

	public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		
//		BitSet bitset = BitSet.valueOf(new byte[]{memory});
//		System.out.println(bitset.toString());
		
		//Get the first 6 bits out of memory
		int f6bits = readF6Bits(memory);
		//Get the last 2 bits out of memory
		int l2bits = readL2Bits(memory);

//		System.out.println("Memory is: " + memory);
//		System.out.println("First 6 bits: " + f6bits + "\t Last 2 bits: " + l2bits);				
		
		if (player_cell.getDiameter() >= 2){ // reproduce whenever possible

			Random rand = new Random();
			
			byte memory1 = writeMemoryByte(f6bits, 0);
			byte memory2 = writeMemoryByte(f6bits, 0);
			
			return new Move(true, memory1, memory2);
		}

		// This is our first strategy. No second one yet.
		if(l2bits==0) {
			ArrayList<Integer> cellAngleList = generateAngleListOfNearbyCells(player_cell,nearby_cells, false);
			ArrayList<Integer> pheromeAngleList = generateAngleListOfNearbyPheromes(player_cell,nearby_pheromes, true);
			ArrayList<Integer> angleList = new ArrayList<Integer>();
			angleList.addAll(cellAngleList);
			angleList.addAll(pheromeAngleList);
			Collections.sort(angleList);
//			System.out.println(angleList);
			
			//Keep trying for max cell distance moves, otherwise move 10% less and keep trying
			for(int f=10; f>0; f--){
				if(angleList.isEmpty()){
					int finalAngle = f6bits*6;
					Point vector = extractVectorFromAngleAndDistance(finalAngle,0.1f*f);
					if (!collides(player_cell, vector, nearby_cells, nearby_pheromes)){
						memory = writeMemoryByte(f6bits,l2bits);
						return new Move(vector, memory);
					}
				}
				else if(angleList.size()==1){
					int finalAngle = (angleList.get(0)+180)%360;
					f6bits = finalAngle/6;
					Point vector = extractVectorFromAngleAndDistance(finalAngle,0.1f*f);
					if (!collides(player_cell, vector, nearby_cells, nearby_pheromes)){
						memory = writeMemoryByte(f6bits,l2bits);
						return new Move(vector, memory);
					}
				}
				else if(angleList.size()>=2){
					HashMap<Integer, Integer> angDiffToAngle = new HashMap<Integer,Integer>();
					
//					int maxDiff = angleList.get(0)-angleList.get(angleList.size()-1)+360;
//					int index = angleList.size()-1;
					for(int i=0; i<angleList.size()-1; i++){
						//Don't consider if causes collision
						if (collides(player_cell, extractVectorFromAngle((angleList.get(i+1)+angleList.get(i))/2), nearby_cells, nearby_pheromes)){
							memory = writeMemoryByte(f6bits,l2bits);
							continue;
						}
//						if(	(angleList.get(i+1)-angleList.get(i)) > maxDiff	){
//							maxDiff = (angleList.get(i+1)-angleList.get(i));
//							index = i;
//						}
						angDiffToAngle.put((angleList.get(i+1)-angleList.get(i))/2, (angleList.get(i)+angleList.get(i+1))/2);
					}
					int tempDiff = angleList.get(0)-angleList.get(angleList.size()-1)+360;
					
					angDiffToAngle.put(tempDiff, angleList.get(angleList.size()-1) + tempDiff/2);
					
					ArrayList<Integer> angleDifferenceList = new ArrayList<Integer>(angDiffToAngle.keySet());
					Collections.sort(angleDifferenceList);
					Collections.reverse(angleDifferenceList);
					for(int i=0; i<angleDifferenceList.size(); i++){
						int finalAngle = angDiffToAngle.get(angleDifferenceList.get(i));
						finalAngle %= 360;
						f6bits = finalAngle/6;
						Point vector = extractVectorFromAngleAndDistance(finalAngle,0.1f*f);
						if (!collides(player_cell, vector, nearby_cells, nearby_pheromes)){
							memory = writeMemoryByte(f6bits,l2bits);
							return new Move(vector, memory);
						}
					}
				}
				
			}
			
		}
		
		// If there was a collision, try
		// random directions to go in until one doesn't collide
		for (int i = 0; i < 4; i++) {
//			int arg = gen.nextInt(180) + 1;
			f6bits = gen.nextInt(60);
			Point vector = extractVectorFromAngle(f6bits*6);
			if (!collides(player_cell, vector, nearby_cells, nearby_pheromes)){
				memory = writeMemoryByte(f6bits,l2bits);
				return new Move(vector, memory);
			}
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
	
	private Point extractVectorFromAngleAndDistance(int arg, float mult) {
		if(mult <= 0) throw new RuntimeException("mult is nonpositive; mult[" + mult + "]");
		if(mult > 1) throw new RuntimeException("mult is greater than 1; mult[" + mult + "]");
		
		double theta = Math.toRadians(1 * (double) arg); //We need bigger circles!
		double dx = mult*Cell.move_dist * Math.cos(theta);
		double dy = mult*Cell.move_dist * Math.sin(theta);
		return new Point(dx, dy);
	}
	
	private Point extractVectorFromAngle(int arg) {
		
		double theta = Math.toRadians(1 * (double) arg); //We need bigger circles!
		double dx = Cell.move_dist * Math.cos(theta);
		double dy = Cell.move_dist * Math.sin(theta);
		return new Point(dx, dy);
	}
	
    private Set<Cell> closeRangeCells(Cell source_cell, Set<Cell> all_cells) {
    	Set<Cell> closest_cells = new HashSet<Cell>();
		for (Cell other_cell : all_cells) {
			if (source_cell.distance(other_cell) < CLOSE_RANGE_VISION) {
				closest_cells.add(other_cell);
			}
		}
		return closest_cells;
    }
    
	private int readF6Bits(byte memory){
		int f6bits = ((memory >> 2) & 0x3f); 
		return f6bits;
	}

	private int readL2Bits(byte memory){
		int l2bits = memory & 0x03;
		return l2bits;
	}
	
	private byte writeMemoryByte(int f6bits, int l2bits){
		if(f6bits < 0){
			throw new RuntimeException("f6bits is negative: [" + f6bits + "]");
		}
		if(f6bits >= 63){
			throw new RuntimeException("f6bits is greater than 63: [" + f6bits + "]");
		}
		if(l2bits < 0){
			throw new RuntimeException("l2bits is negative: [" + l2bits + "]");
		}
		if(l2bits > 3){
			throw new RuntimeException("l2bits is greater than 3: [" + f6bits + "]");
		}
		byte memory = (byte) ((f6bits << 2) | (0x03 & l2bits));
		return memory;
	}

	private ArrayList<Integer> generateAngleListOfNearbyCells(Cell player_cell, Set<Cell> nearby_cells, boolean ignoreSamePlayer){
		ArrayList<Integer> angleList = new ArrayList<Integer>();
		for(Cell c : nearby_cells){
			if(ignoreSamePlayer && (c.player == player_cell.player)) //Ignore your own pheromes
				continue;
			
			if(player_cell.distance(c) > CLOSE_RANGE_VISION)
				continue;
			
			double cX = c.getPosition().x;
			double cY = c.getPosition().y;
			double tX = player_cell.getPosition().x;
			double tY = player_cell.getPosition().y;
			double dX = cX-tX;
			double dY = cY-tY;
			double angle = Math.atan(dY/dX);
			
			if(dX>=0 && dY>=0); //Do nothing
			if(dX>=0 && dY<0) angle += 2*Math.PI;
			if(dX<0 && dY>=0) angle = Math.PI - angle;
			if(dX<0 && dY<0) angle = Math.PI - angle;
//			System.out.println(player_cell.hashCode() + "\t" + dY/dX);
//			System.out.println(Math.toDegrees(angle));
			angleList.add((int)Math.toDegrees(angle));
		}
		Collections.sort(angleList);
		return angleList;
	}
	
	private ArrayList<Integer> generateAngleListOfNearbyPheromes(Cell player_cell, Set<Pherome> nearby_pheromes, boolean ignoreSamePlayer){
		ArrayList<Integer> angleList = new ArrayList<Integer>();
		for(Pherome p : nearby_pheromes){
			if(ignoreSamePlayer && (p.player == player_cell.player)) //Ignore your own pheromes
				continue;
			
			if(player_cell.distance(p) > CLOSE_RANGE_VISION)
				continue;
			
			double pX = p.getPosition().x;
			double pY = p.getPosition().y;
			double tX = player_cell.getPosition().x;
			double tY = player_cell.getPosition().y;
			double dX = pX-tX;
			double dY = pY-tY;
			double angle = Math.atan(dY/dX);
			
			if(dX>=0 && dY>=0); //Do nothing
			if(dX>=0 && dY<0) angle += 2*Math.PI;
			if(dX<0 && dY>=0) angle = Math.PI - angle;
			if(dX<0 && dY<0) angle = Math.PI - angle;
			
//			System.out.println(player_cell.hashCode() + "\t" + dY/dX);
//			System.out.println(Math.toDegrees(angle));
			angleList.add((int)Math.toDegrees(angle));
		}
		Collections.sort(angleList);
		return angleList;
	}
}
