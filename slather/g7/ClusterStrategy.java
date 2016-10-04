package slather.g7;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.Random;

import slather.sim.Cell;
import slather.sim.GridObject;
import slather.sim.Move;
import slather.sim.Pherome;
import slather.sim.Point;

public class ClusterStrategy implements Strategy {

	static Random gen = new Random();

	@Override
	public Memory getNewMemoryObject() {
		return ExplorerMemory.getNewObject();
	}

	@Override
	public Move generateMove(Cell player_cell, Memory memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		
		/*
		 * If cell can reproduce, do it. Otherwise, move based on clustering
		 * strategy
		 */

		// Reproduction
		if (player_cell.getDiameter() >= 2) {
			Memory childMemory = generateFirstChildMemory(memory);
			return new Move(true, childMemory.getByte(), generateSecondChildMemory(memory, childMemory).getByte());
		}

		Point moveDirection;
		char defOrExp = memory.getMemoryAt(0);
		if (defOrExp == '0') {
			ExplorerMemory memObj = (ExplorerMemory) memory;
			moveDirection = getCumulativeDirection(player_cell, nearby_cells, nearby_pheromes);
			// Last bit denotes direction of movement
			// If it is 1, move towards our own cells
			if (memObj.opposite == 1) {
				moveDirection = new Point(-moveDirection.x, -moveDirection.y);
			}
		} else {
			int t=Player.t;
			DefenderMemory memObj = (DefenderMemory) memory;
			moveDirection = drawCircle(player_cell, memObj, t);
		}

		/*
		 * 
		 * If no movement at all, the normalization will make the random
		 * movement 1mm in length.
		 */
		Point finalMoveDirection = addRandomnessAndGenerateFinalDirection(moveDirection);
		int i = 0;
		int MAX_RANDOM_TRIES = 3;
		boolean willCollide = collides(player_cell, finalMoveDirection, nearby_cells, nearby_pheromes);

		/* Check if it will collide */
		while (willCollide && i < MAX_RANDOM_TRIES) {
			finalMoveDirection = addRandomnessAndGenerateFinalDirection(moveDirection);
			willCollide = collides(player_cell, finalMoveDirection, nearby_cells, nearby_pheromes);
			i++;
		}
		/* If still colliding anyway, go to free space */
		if (willCollide) {
			System.out.println("Still colliding after 3 tries. Go to free space.");
			finalMoveDirection = headToFreeSpace(player_cell, nearby_cells, nearby_pheromes);
		}
		
		/* Next step memory */
		Memory nextMem=generateNextMoveMemory(memory);
		
		return new Move(finalMoveDirection,nextMem.getByte());
	}

	public Point drawCircle(Cell myCell, DefenderMemory memory, int t) {
		int radian=memory.circleBits;
		int sides=Math.max(4, 2*t);
		
		double start=2*Math.PI*radian/128;
		double step=2*Math.PI/sides;
		
		double theta=start+step;
		return ToolBox.newDirection(myCell.getPosition(), theta);
	}

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

	/*
	 * The current strategy is: No enemy cells -> expand. No friend cells -> run
	 * away. No cells at all -> go in a straight line.
	 * 
	 * If no enemies around, the merged force will automatically push you away.
	 * If no friends around, we must reverse the movement to make it not go to
	 * the enemies. If no cells around, your pherome will drive you away
	 * automatically.
	 */
	public static Point getCumulativeDirection(Cell myCell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		Set<Cell> friends = new HashSet<>();
		Set<Cell> enemies = new HashSet<>();
		for (Cell c : nearby_cells) {
			if (c.player == myCell.player)
				friends.add(c);
			else
				enemies.add(c);
		}

		System.out.println("Join forces from cells:");
		Point fromCells = joinForcesFromCells(myCell, nearby_cells, 1.0);

		if (friends.size() == 0) {
			/*
			 * Run away in this case, namely go opposite direction from the
			 * merged force
			 */
			System.out.println("No friend cells around. Need to go away from enemy cells as well.");
			Point reverseForceFromCells = new Point(-1 * fromCells.x, -1 * fromCells.y);
			return reverseForceFromCells;
		} else if (enemies.size() == 0) {
			Point toExpand = headToFreeSpace(myCell, nearby_cells, nearby_pheromes);
			return toExpand;
		}

		/* If no friend or enemy, your own pherome will drive you away */
		System.out.println("Join forces from pheromes:");
		Point fromPheromes = joinForcesFromPheromes(myCell, nearby_pheromes, 0.5);
		Point normalized = ToolBox.normalizeDistance(fromCells, fromPheromes);

		return normalized;
	}

	/*
	 * Head to free space Look around to find the largest angle between things
	 * and go there
	 */
	public static Point headToFreeSpace(Cell myCell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		// Point runFromCells = runFromAll(myCell, nearby_cells, 1.0);
		// Point runFromPheromes = runFromAll(myCell, nearby_pheromes, 0.5);

		Point myPoint = myCell.getPosition();

		/* Store a sorted angle from every grid object around my cell */
		TreeMap<Double, Point> angleMap = new TreeMap<>();

		for (Cell c : nearby_cells) {
			double angle = ToolBox.getCosine(myCell.getPosition(), c.getPosition());
			angleMap.put(angle, c.getPosition());
		}
		for (Pherome p : nearby_pheromes) {
			if (p.player == myCell.player)
				continue;
			double angle = ToolBox.getCosine(myCell.getPosition(), p.getPosition());
			angleMap.put(angle, p.getPosition());
		}

		/* Find the largest gap between things */
		Iterator<Entry<Double, Point>> it = angleMap.entrySet().iterator();
		Point lastPoint = angleMap.lastEntry().getValue();
		double lastAngle = angleMap.lastKey();
		double largestGap = 0.0;
		double largestGapStart = 0.0;
		double largestGapEnd = 0.0;
		while (it.hasNext()) {
			Entry<Double, Point> e = it.next();
			/* thisPoint is always on the right of lastPoint */
			Point thisPoint = e.getValue();
			double thisAngle = e.getKey();
			double angleDiff = ToolBox.angleDiff(thisAngle, lastAngle);
			if (angleDiff > largestGap) {
				largestGap = angleDiff;
				largestGapStart = lastAngle;
				largestGapEnd = thisAngle;
			}

			/* Slide the window */
			lastAngle = thisAngle;
			lastPoint = thisPoint;
		}
		System.out.println("The largest gap found so far is: " + largestGap);

		/* Decide the angle to go */
		double toGo = largestGapStart + 0.5 * largestGap;
		/* Todo: What if the largest gap is 0? Very unlikely */

		/* Generate the point to go */
		return ToolBox.newDirection(myPoint, toGo);
	}

	/*
	 * Based on the assumption that we chase opponent cells and get away from
	 * friendly ones.
	 */
	public static Point joinForcesFromCells(Cell myCell, Set<Cell> cells, double weight) {
		Point myPos = myCell.getPosition();

		double offX = 0.0;
		double offY = 0.0;
		for (Cell c : cells) {
			Point cPos = c.getPosition();
			/* The smaller the distance, the larger the force */
			int chaseIt;
			if (c.player == myCell.player)
				chaseIt = -1;
			else
				chaseIt = 1;

			double diffX = cPos.x - myPos.x;
			double diffY = cPos.y - myPos.y;
			// The force is proportional to the mass of the cell, which depends
			// on the square of diameter in a 2d environment
			offX += chaseIt * diffX * weight * (c.getDiameter() * c.getDiameter());
			offY += chaseIt * diffY * weight * (c.getDiameter() * c.getDiameter());
		}

		System.out.println("The merged force from cells is X: " + offX + " Y: " + offY);

		return new Point(offX, offY);
	}

	/* Be driven away from all cells */
	public static Point runFromAll(Cell myCell, Set<? extends GridObject> neighbors, double weight) {
		Point myPos = myCell.getPosition();

		double offX = 0.0;
		double offY = 0.0;
		for (GridObject g : neighbors) {
			Point gPos = g.getPosition();
			/* The smaller the distance, the larger the force */
			int chaseIt = -1;
			double diffX = gPos.x - myPos.x;
			double diffY = gPos.y - myPos.y;
			// The force is proportional to the mass of the cell, which depends
			// on the square of diameter in a 2d environment
			double addX = chaseIt * diffX * weight;
			double addY = chaseIt * diffY * weight;
			if (g instanceof Cell) {
				Cell c = (Cell) g;
				addX *= (c.getDiameter() * c.getDiameter());
				addY *= (c.getDiameter() * c.getDiameter());
			}
			offX += addX;
			offY += addY;
		}

		System.out.println("The merged force from cells is X: " + offX + " Y: " + offY);

		return new Point(offX, offY);
	}

	public static Point joinForcesFromPheromes(Cell myCell, Set<Pherome> pheromes, double weight) {
		Point myPos = myCell.getPosition();
		double offX = 0.0;
		double offY = 0.0;
		for (Pherome p : pheromes) {
			Point pPos = p.getPosition();
			/* The smaller the distance, the larger the force */
			int chaseIt;
			if (p.player == myCell.player)
				chaseIt = -1;
			else
				chaseIt = 1;

			double diffX = pPos.x - myPos.x;
			double diffY = pPos.y - myPos.y;
			// The force is proportional to the mass of the cell, which depends
			// on the square of diameter in a 2d environment
			offX += chaseIt * diffX * weight;
			offY += chaseIt * diffY * weight;
		}

		System.out.println("The merged force from pheromes is X: " + offX + " Y: " + offY);
		return new Point(offX, offY);
	}

	public static boolean collides(Cell player_cell, Point vector, Set<Cell> nearby_cells,
			Set<Pherome> nearby_pheromes) {
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

	public static Point addRandomnessAndGenerateFinalDirection(Point direction) {
		double desiredMean = 0.0;
		double desiredStandardDeviation = 0.1;// Set a very small deviation so
		// that the whole force thing
		// still makes sense
		double offX = direction.x + gen.nextGaussian() * desiredStandardDeviation + desiredMean;
		double offY = direction.y + gen.nextGaussian() * desiredStandardDeviation + desiredMean;

		Point newDir = ToolBox.normalizeDistance(new Point(offX, offY));
		// System.out.println("The randomized direction has X: " + newDir.x + "
		// Y: " + newDir.y);
		return newDir;
	}
}
