package slather.g3;

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
		
//		BitSet bitset = BitSet.valueOf(new byte[]{memory});
//		System.out.println(bitset.toString());
		
		//Get the first 6 bits out of memory
		int f6bits = ((memory >> 2) & 0x3f); 
		//Get the last 2 bits out of memory
		int l2bits = memory & 0x03;

		
//		System.out.println("Memory is: " + memory);
//		System.out.println("First 6 bits: " + f6bits + "\t Last 2 bits: " + l2bits);		
		
		
		if (player_cell.getDiameter() >= 2){ // reproduce whenever possible

			Random rand = new Random();
			
			byte memory1 = (byte) ((f6bits << 2) | (0x03 & l2bits)); //First daughter keeps same strategy
			byte memory2 = (byte) ((f6bits << 2) | (0x03 & (l2bits+rand.nextInt(3))%4)); //Second tries random strategy different from original
			
			return new Move(true, memory1, memory2);
		}
		
		
		
		for(int i=0; i<4; i++){
			//This is the counterclockwise circle strategy
			f6bits = (f6bits+1)%60; //Keep degree precision at 6 (6*60 = 360 degrees)

			//				Point vector = extractVectorFromAngle(f6bits * 6);
			
			Point vector;
			if(f6bits <30)
				vector = extractVectorFromAngle(f6bits * 6 +l2bits*90);
			else
				vector = extractVectorFromAngle((60-f6bits) * 6 +l2bits*90);
			
			//Store bits back into memory
			memory = (byte) ((f6bits << 2) | (0x03 & l2bits));
			if (!collides(player_cell, vector, nearby_cells, nearby_pheromes))
				return new Move(vector, memory);
			else{
				l2bits = (l2bits+1)%4; //switch to cw circles
//					f6bits = (f6bits + 30)%60;
			}
		}

		// If there was a collision, try
		// random directions to go in until one doesn't collide
		for (int i = 0; i < 4; i++) {
//			int arg = gen.nextInt(180) + 1;
			f6bits = gen.nextInt(60);
			Point vector = extractVectorFromAngle(f6bits);
			if (!collides(player_cell, vector, nearby_cells, nearby_pheromes)){
				memory = (byte) ((f6bits << 2) | (0x03 & l2bits));
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
	private Point extractVectorFromAngle(int arg) {
		
		double theta = Math.toRadians(1 * (double) arg); //We need bigger circles!
		double dx = Cell.move_dist * Math.cos(theta);
		double dy = Cell.move_dist * Math.sin(theta);
		return new Point(dx, dy);
	}
	


}
