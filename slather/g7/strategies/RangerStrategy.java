package slather.g7.strategies;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import slather.g7.DefenderMemory;
import slather.g7.DummyMemory;
import slather.g7.ExplorerMemory;
import slather.g7.Memory;
import slather.g7.Player;
import slather.g7.Strategy;
import slather.g7.ToolBox;
import slather.sim.Cell;
import slather.sim.GridObject;
import slather.sim.Move;
import slather.sim.Pherome;
import slather.sim.Point;

public class RangerStrategy implements Strategy{

	static Random gen = new Random();
	static double vision = 2.0;
	static int visionBufferSize = 10;
	
	@Override
	public Memory generateNextMoveMemory(Memory currentMemory) {
		return currentMemory.generateNextMoveMemory();
	}

	@Override
	public Memory generateFirstChildMemory(Memory currentMemory) {
		return currentMemory.generateFirstChildMemory();

	}

	@Override
	public Memory generateSecondChildMemory(Memory currentMemory, Memory firstChildMemory) {
		return currentMemory.generateSecondChildMemory(firstChildMemory);
	}

	@Override
	public Move generateMove(Cell player_cell, Memory memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		System.out.println("Ranger strategy. Go in circles.");
		//If we were not circling last round, choose a wise direction to start
		if(memory instanceof DummyMemory||memory instanceof ExplorerMemory){
			System.out.println("The cell was not defender last round. Decide the direction.");
			
			//Choose a good move
			Point dir=decideNextDirection(player_cell,nearby_cells,nearby_pheromes);
			int circleMem=translateDirToMem(dir);
			
			//Initialize the memory again with this direction
			DefenderMemory dm=DefenderMemory.getNewObject();
			dm.initialize(circleMem);
			Memory nextMem = generateNextMoveMemory(dm);
			
			//Go to the direction
			Point nextStep=generateNextDirection(player_cell,memory,nearby_cells,nearby_pheromes);
			
			return new Move(dir,nextMem.getByte());
		}
		//If we were circling last round, keep circling this round
		else if(memory instanceof DefenderMemory){
			Point nextStep=generateNextDirection(player_cell,memory,nearby_cells,nearby_pheromes);
			Memory nextMem = generateNextMoveMemory(memory);
			
			return new Move(nextStep,nextMem.getByte());
		}else{
			System.out.println("Error: Unmanipulable memory in RangerMemory "+memory.getByte());
			return new Move(new Point(0,0),new DummyMemory().getByte());
		}
	}

	@Override
	public Point generateNextDirection(Cell player_cell, Memory memory, Set<Cell> nearby_cells,
			Set<Pherome> nearby_pheromes) {
		DefenderMemory mem=(DefenderMemory) memory;
		return drawCircle(player_cell,mem,Player.num_def_sides);
	}

	public Point drawCircle(Cell myCell, DefenderMemory memory, int t) {
		int radian = memory.getCircleBits();
		int sides = Player.num_def_sides;

		double start = 2 * Math.PI * radian / sides;
		double step = 2 * Math.PI / sides;

		double theta = start + step;
		System.out.println("Current direction is " + start + ", going towards " + theta);
		return ToolBox.newDirection(theta);
	}
	
	public Point decideNextDirection(Cell myCell,Set<Cell> cells,Set<Pherome> pheromes){
		Point target=headToFreeSpaceStartFromRight(myCell,cells,pheromes);
		return target;
	}
	
	public int translateDirToMem(Point dir){
		double angle = ToolBox.getCosine(new Point(0.0,0.0), dir);
		
		int sides = Player.num_def_sides;
		double step = 2 * Math.PI / sides;
		
		//check how many steps this angle contains
		int stepCount=(int)Math.ceil(angle/step);
		System.out.println("Angle "+angle+" corresponds to memory "+stepCount);
		return stepCount;
	}
	
	/*
	 * Head to free space Look around to find the largest angle between things
	 * and go there
	 */
	public Point headToFreeSpaceStartFromRight(Cell myCell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		// Point runFromCells = runFromAll(myCell, nearby_cells, 1.0);
		// Point runFromPheromes = runFromAll(myCell, nearby_pheromes, 0.5);

		Point myPoint = myCell.getPosition();
		nearby_cells = ToolBox.limitVisionOnCells(myCell, nearby_cells, vision);
		nearby_pheromes = ToolBox.limitVisionOnPheromes(myCell, nearby_pheromes, vision);

		/* Store a sorted angle from every grid object around my cell */
		TreeMap<Double, GridObject> angleMap = new TreeMap<>();

		//Limit vision to the closest several things
		Set<GridObject> thingsISee = ToolBox.limitVisionToSize(myCell, nearby_cells, nearby_pheromes, true, visionBufferSize);
		
		for (GridObject g : thingsISee) {
			double angle = ToolBox.getCosine(myCell.getPosition(), g.getPosition());
			angleMap.put(angle, g);
		}

		/* Find the largest gap between things */
		if (angleMap.size() == 0) {
			System.out.println("Nothing around. Just go somewhere. ");
			Point dir = new Point(gen.nextDouble() - 0.5, gen.nextDouble() - 0.5);
			return ToolBox.normalizeDistance(dir);
		} else if (angleMap.size() == 1) {
			System.out.println("Only one cell around.");
			double toGo = angleMap.firstKey() + Math.PI;
			return ToolBox.newDirection(toGo);
		}

		Iterator<Entry<Double, GridObject>> it = angleMap.entrySet().iterator();
		GridObject lastPoint = angleMap.lastEntry().getValue();
		double lastAngle = angleMap.lastKey();
		double largestGap = 0.0;
		double largestGapStart = 0.0;
		double largestGapEnd = 0.0;
		
		GridObject target = null;
		while (it.hasNext()) {
			Entry<Double, GridObject> e = it.next();
			/* thisPoint is always on the right of lastPoint */
			GridObject thisPoint = e.getValue();

			double thisAngle = e.getKey();// this is the angle of the center
			double angleDiff = ToolBox.angleDiff(lastAngle, thisAngle);
			System.out.println("Angle difference with no regard to radius is:" + angleDiff);
			// If the object is cell, need to consider the radius
			Point mp = myCell.getPosition();
			
			double startAngleTan=0.0;
			double endAngleTan=0.0;
			if (lastPoint instanceof Cell) {
				Cell c = (Cell) lastPoint;
				Point cp = c.getPosition();
				// calculate the distance between two points
				Point diffPoint = ToolBox.pointDistance(cp, mp);
				double mc = diffPoint.x * diffPoint.x + diffPoint.y * diffPoint.y;
				System.out.println("Distance between points is:" + mc);
				if (mc <= 0.00001) {// might overflow
					System.out.println("Error: Distance between cells is smaller than radius!");
				} else {
					double cr = c.getDiameter();
					// If the object is cell, the distance cannot be 0
					double diffTheta = Math.asin(cr / mc);
					System.out.println("Subtracting theta covered by cell radius: " + diffTheta);
					angleDiff -= diffTheta;
					endAngleTan=diffTheta;
				}
			}
			if (thisPoint instanceof Cell) {
				Cell c = (Cell) thisPoint;
				Point cp = c.getPosition();
				// calculate the distance between two points
				Point diffPoint = ToolBox.pointDistance(cp, mp);
				double mc = diffPoint.x * diffPoint.x + diffPoint.y * diffPoint.y;
				System.out.println("Distance between points is:" + mc);
				if (mc <= 0.00001) {// might overflow
					System.out.println("Error: Distance between cells is smaller than radius!");
				} else {
					double cr = c.getDiameter();
					// If the object is cell, the distance cannot be 0
					double diffTheta = Math.asin(cr / mc);
					System.out.println("Subtracting theta covered by cell radius: " + diffTheta);
					angleDiff -= diffTheta;
					startAngleTan=diffTheta;
				}
			}

			if (angleDiff > largestGap) {
				largestGap = angleDiff;
				target=lastPoint;
				largestGapStart = thisAngle-startAngleTan;
				largestGapEnd = lastAngle+endAngleTan;
			}

			/* Slide the window */
			lastAngle = thisAngle;
			lastPoint = thisPoint;
		}
		System.out.println("The largest gap found so far is: " + largestGap);

		/* Decide the angle to go */
//		double toGo = largestGapStart - 0.5 * largestGap;
		double toGo=largestGapEnd;
		System.out.println("The angle we go to is "+toGo);

		/* Generate the point to go */
		return ToolBox.newDirection(toGo);
	}
	
}
