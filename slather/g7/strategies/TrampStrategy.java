package slather.g7.strategies;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.Random;

import slather.g7.DummyMemory;
import slather.g7.Memory;
import slather.g7.Strategy;
import slather.g7.ToolBox;
import slather.sim.Cell;
import slather.sim.GridObject;
import slather.sim.Move;
import slather.sim.Pherome;
import slather.sim.Point;

public class TrampStrategy implements Strategy{
	static boolean considerRadiusInFreeSpaceFinding=false;
	static Random gen=new Random();
	static double vision=2.0;
	
	@Override
	public Memory generateNextMoveMemory(Memory currentMemory) {
		return new DummyMemory();
	}

	@Override
	public Memory generateFirstChildMemory(Memory currentMemory) {
		return new DummyMemory();
	}

	@Override
	public Memory generateSecondChildMemory(Memory currentMemory, Memory firstChildMemory) {
		return new DummyMemory();
	}

	@Override
	public Move generateMove(Cell player_cell, Memory memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		System.out.println("Tramp strategy. Go to free space.");
		Point nextStep=generateNextDirection(player_cell,memory,nearby_cells,nearby_pheromes);
		Memory nextMem = generateNextMoveMemory(memory);

		return new Move(nextStep,nextMem.getByte());
	}

	@Override
	public Point generateNextDirection(Cell player_cell, Memory memory, Set<Cell> nearby_cells,
			Set<Pherome> nearby_pheromes) {
		System.out.println("Tramp strategy. Head to free space.");
		Point nextStep=headToFreeSpace(player_cell,nearby_cells,nearby_pheromes);
		
		
		return nextStep;
	}
	
	/*
	 * Head to free space Look around to find the largest angle between things
	 * and go there
	 */
	public static Point headToFreeSpace(Cell myCell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		// Point runFromCells = runFromAll(myCell, nearby_cells, 1.0);
		// Point runFromPheromes = runFromAll(myCell, nearby_pheromes, 0.5);

		Point myPoint = myCell.getPosition();
		Set<Cell> cellsISee=ToolBox.limitVisionOnCells(myCell, nearby_cells, vision);
		Set<Pherome> pheromesISee=ToolBox.limitVisionOnPheromes(myCell, nearby_pheromes, vision);

		/* Store a sorted angle from every grid object around my cell */
		TreeMap<Double, GridObject> angleMap = new TreeMap<>();

		for (Cell c : cellsISee) {
			//If too far away, ignore 
			double dist=ToolBox.calcRawDist(myCell, c);
			/* Deduct the radius of the other cell.
			 * Effect: If the other cell is more than 2mm away but its body will come in our way, still consider it.
			 * */
			if(considerRadiusInFreeSpaceFinding)
				dist-=c.getDiameter();
			if(dist>2)
				continue;
			double angle = ToolBox.getCosine(myCell.getPosition(), c.getPosition());
			angleMap.put(angle, c);
		}
		for (Pherome p : pheromesISee) {
			if (p.player == myCell.player)
				continue;
			//If too far away, ignore 
			double dist=ToolBox.calcRawDist(myCell, p);
			if(dist>2)
				continue;
			double angle = ToolBox.getCosine(myCell.getPosition(), p.getPosition());
			angleMap.put(angle, p);
		}

		/* Find the largest gap between things */
		if (angleMap.size() == 0) {
			System.out.println("Nothing around. Just go somewhere. ");
			Point dir=new Point(gen.nextDouble()-0.5, gen.nextDouble()-0.5);
			return ToolBox.normalizeDistance(dir);
		}
		
		Iterator<Entry<Double, GridObject>> it = angleMap.entrySet().iterator();
		GridObject lastPoint = angleMap.lastEntry().getValue();
		double lastAngle = angleMap.lastKey();
		double largestGap = 0.0;
		double largestGapStart = 0.0;
		double largestGapEnd = 0.0;
		while (it.hasNext()) {
			Entry<Double, GridObject> e = it.next();
			/* thisPoint is always on the right of lastPoint */
			GridObject thisPoint = e.getValue();
			double thisAngle = e.getKey();//this is the angle of the center
			double angleDiff = ToolBox.angleDiff(lastAngle, thisAngle);
			System.out.println("Angle difference with no regard to radius is:" +angleDiff);
			//If the object is cell, need to consider the radius
			Point mp=myCell.getPosition();
			if(lastPoint instanceof Cell){
				Cell c=(Cell)lastPoint;
				Point cp=c.getPosition();
				//calculate the distance between two points
				Point diffPoint=ToolBox.pointDistance(cp, mp);
				double mc=diffPoint.x*diffPoint.x+diffPoint.y*diffPoint.y;
				System.out.println("Distance between points is:"+mc);
				if(mc<=0.00001){//might overflow
					System.out.println("Error: Distance between cells is smaller than radius!");
				}else{
					double cr=c.getDiameter();
					//If the object is cell, the distance cannot be 0
					double diffTheta=Math.asin(cr/mc);
					System.out.println("Subtracting theta covered by cell radius: "+diffTheta);
					angleDiff-=diffTheta;
				}
			}
			if(thisPoint instanceof Cell){
				Cell c=(Cell)thisPoint;
				Point cp=c.getPosition();
				//calculate the distance between two points
				Point diffPoint=ToolBox.pointDistance(cp, mp);
				double mc=diffPoint.x*diffPoint.x+diffPoint.y*diffPoint.y;
				System.out.println("Distance between points is:"+mc);
				if(mc<=0.00001){//might overflow
					System.out.println("Error: Distance between cells is smaller than radius!");
				}else{
					double cr=c.getDiameter();
					//If the object is cell, the distance cannot be 0
					double diffTheta=Math.asin(cr/mc);
					System.out.println("Subtracting theta covered by cell radius: "+diffTheta);
					angleDiff-=diffTheta;
				}
			}
			
			if (angleDiff > largestGap) {
				largestGap = angleDiff;
				largestGapStart = thisAngle;
				largestGapEnd = lastAngle;
			}

			/* Slide the window */
			lastAngle = thisAngle;
			lastPoint = thisPoint;
		}
		System.out.println("The largest gap found so far is: " + largestGap);

		/* Decide the angle to go */
		double toGo = largestGapStart - 0.5 * largestGap;
		/* Todo: What if the largest gap is 0? Very unlikely */
		if(toGo==0.0){
			System.out.println("The largest gap is 0?!");
		}

		/* Generate the point to go */
		return ToolBox.newDirection(myPoint, toGo);
	}

}
