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
	static double MAX_VISION_DISTANCE = 7.0;
	static double nearDelivery=Math.pow(1.01, 69);

	@Override 
	public Move generateMove(Cell player_cell, Memory memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		/*
		 * If cell can reproduce, do it. Otherwise, move based on clustering
		 * strategy
		 */
		
		if (player_cell.getDiameter() >= 2) {
			Memory childMemory = generateFirstChildMemory(memory);
			Memory secondChildMemory = generateSecondChildMemory(memory, childMemory);
			return new Move(true, childMemory.getByte(), secondChildMemory.getByte());
		}
		
		/*
		 * Restricting vision for cells to a maximum distance
		 */

//		Set<Cell> restrictedNearbyCells = new HashSet<>();
//		for (Cell cell : nearby_cells) {
//			if (player_cell.distance(cell) <= MAX_VISION_DISTANCE) {
//				restrictedNearbyCells.add(cell);
//			}
//		}
//		nearby_cells = restrictedNearbyCells;
		nearby_cells=limitVisionOnCells(player_cell,nearby_cells);
		
//		Set<Pherome> restrictedNearbyPhermoes = new HashSet<>();
//		for (Pherome pherome: nearby_pheromes) {
//			if (player_cell.distance(pherome) <= MAX_VISION_DISTANCE) {
//				restrictedNearbyPhermoes.add(pherome);
//			}
//		}
//		nearby_pheromes = restrictedNearbyPhermoes;
		nearby_pheromes=limitVisionOnPheromes(player_cell,nearby_pheromes);

		Point moveDirection;
		
		/* Check prioritized situations */
		Point presetRules=checkPresetRules(player_cell,nearby_cells,nearby_pheromes);
		if(presetRules!=null){
			moveDirection=presetRules;
		}else{
			System.out.println("The situation does not meet preset rules. Do your job.");
			if (memory instanceof ExplorerMemory) {
				System.out.println("Cell is explorer.");
				ExplorerMemory memObj = (ExplorerMemory) memory;
				moveDirection = getCumulativeDirection(player_cell, nearby_cells, nearby_pheromes);
				// Last bit denotes direction of movement
				// If it is 1, move towards our own cells
				if (memObj.getOpposite() == 1) {
					moveDirection = new Point(-moveDirection.x, -moveDirection.y);
				}
				/* Add randomness if it's explorer */
				moveDirection = addRandomnessAndGenerateFinalDirection(moveDirection);
			} else {
				System.out.println("Cell is defender.");
				int t = Player.T;
				System.out.println("Defender move");
				DefenderMemory memObj = (DefenderMemory) memory;
				moveDirection = drawCircle(player_cell, memObj, t);
			}
		}
		
		/* Check if it will collide */
		Point finalMoveDirection = moveDirection;
		int i = 0;
		int MAX_RANDOM_TRIES = 3;
		boolean willCollide = collides(player_cell, finalMoveDirection, nearby_cells, nearby_pheromes);
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
		Memory nextMem = generateNextMoveMemory(memory);

		return new Move(finalMoveDirection, nextMem.getByte());
	}

	public Point drawCircle(Cell myCell, DefenderMemory memory, int t) {
		int radian = memory.getCircleBits();
		int sides = Player.num_def_sides;
		sides = Math.min(sides, 10);

		double start = 2 * Math.PI * radian / sides;
		double step = 2 * Math.PI / sides;

		double theta = start + step;
		System.out.println("Current direction is " + start + ", going towards " + theta);
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
	 * If no cells around, your pherome will drive you away automatically.
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
		// Point fromCells = joinForcesFromCells(myCell, nearby_cells, 1.0);
		Point fromFriends = joinForcesFromCells(myCell, friends, 1.0, 1);
		Point fromEnemies = joinForcesFromCells(myCell, enemies, 1.0, 1);
		Point fromCells;

		if (friends.size() == 0 && enemies.size() == 0) {
			System.out.println("Nothing around.");
			fromCells = new Point(0, 0);
		} else if (friends.size() == 0) {
			/*
			 * Run away in this case, namely go opposite direction from the
			 * merged force
			 */
			// Point reverseForceFromCells = new Point(-1 * fromCells.x, -1 *
			// fromCells.y);
			// return reverseForceFromCells;
			System.out.println("No friends around. Go to free space. ");
			fromCells = headToFreeSpace(myCell, nearby_cells, new HashSet<Pherome>());

		} else if (enemies.size() == 0) {
			System.out.println("No enemies around. Merge forces. ");
			// Point toExpand = headToFreeSpace(myCell, nearby_cells,
			// nearby_pheromes);

			// return toExpand;
			fromCells = fromFriends;
		} else {
			fromCells = ToolBox.normalizeDistance(fromFriends, fromEnemies);
		}

		/* If no friend or enemy, your own pherome will drive you away */
		Point fromPheromes = joinForcesFromPheromes(myCell, nearby_pheromes, 0.5);
		
		Point normalized = ToolBox.normalizeDistance(fromCells, fromPheromes);

		if (normalized.x == 0.0 && normalized.y == 0.0) {
			System.out.println("Nothing around. Decide the direction arbitrarily");
			normalized = ToolBox.normalizeDistance(new Point(gen.nextDouble(), gen.nextDouble()));
		}
		System.out.println("The direction is x: " + normalized.x + " y: " + normalized.y);

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
		if (angleMap.size() == 0) {
			System.out.println("Nothing around. Just go somewhere. ");
			return new Point(gen.nextDouble(), gen.nextDouble());
		}
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
			double angleDiff = ToolBox.angleDiff(lastAngle, thisAngle);
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

		/* Generate the point to go */
		return ToolBox.newDirection(myPoint, toGo);
	}

	/*
	 * chaseIt>0: chase the cell chaseIt<0: run away from the cell
	 * 
	 */
	public static Point joinForcesFromCells(Cell myCell, Set<Cell> cells, double weight, int chaseIt) {
		Point myPos = myCell.getPosition();
		double offX = 0.0;
		double offY = 0.0;
		for (Cell c : cells) {
			Point cPos = c.getPosition();
			/* The smaller the distance, the larger the force */
			Point diff = ToolBox.pointDistance(cPos, myPos);
			// The force is proportional to the mass of the cell, which depends
			// on the square of diameter in a 2d environment
			offX += chaseIt * diff.x * weight * (c.getDiameter() * c.getDiameter());
			offY += chaseIt * diff.y * weight * (c.getDiameter() * c.getDiameter());
		}

		// System.out.println("The merged force from cells is X: " + offX + " Y:
		// " + offY);

		return new Point(offX, offY);
	}

	public static Point joinForcesFromPheromes(Cell myCell, Set<Pherome> pheromes, double weight) {
		Point myPos = myCell.getPosition();
		double offX = 0.0;
		double offY = 0.0;
		for (Pherome p : pheromes) {
			Point pPos = p.getPosition();
			/* The smaller the distance, the larger the force */
			int chaseIt = 1;
			// if (p.player == myCell.player)
			// chaseIt = -1;
			// else
			// chaseIt = 1;

			Point diff = ToolBox.pointDistance(pPos, myPos);
			// The force is proportional to the mass of the cell, which depends
			// on the square of diameter in a 2d environment
			offX += chaseIt * diff.x * weight;
			offY += chaseIt * diff.y * weight;
		}

		// System.out.println("The merged force from pheromes is X: " + offX + "
		// Y: " + offY);
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
		double desiredStandardDeviation = 0.0;// Set a very small deviation so
		// Set a very small deviation so
		// that the whole force thing
		// still makes sense
		double offX = direction.x + gen.nextGaussian() * desiredStandardDeviation + desiredMean;
		double offY = direction.y + gen.nextGaussian() * desiredStandardDeviation + desiredMean;

		Point newDir = ToolBox.normalizeDistance(new Point(offX, offY));
		// System.out.println("The randomized direction has X: " + newDir.x + "
		// Y: " + newDir.y);
		return newDir;
	}
	/*
	 * Newly added functions
	 * */
	public static Set<Cell> limitVisionOnCells(Cell player_cell,Set<Cell> nearby_cells){
		Set<Cell> restrictedNearbyCells = new HashSet<>();
		for (Cell cell : nearby_cells) {
			if (player_cell.distance(cell) <= MAX_VISION_DISTANCE) {
				restrictedNearbyCells.add(cell);
			}
		}
		return restrictedNearbyCells;
	}
	public static Set<Pherome> limitVisionOnPheromes(Cell player_cell,Set<Pherome> nearby_pheromes){
		Set<Pherome> restrictedNearbyPhermoes = new HashSet<>();
		for (Pherome pherome: nearby_pheromes) {
			if (player_cell.distance(pherome) <= MAX_VISION_DISTANCE) {
				restrictedNearbyPhermoes.add(pherome);
			}
		}
		return restrictedNearbyPhermoes;
	}
	/*
	 * Preset rules:
	 * 1. If about to reproduce: head to free space.
	 * 2. If no cells around, my own pherome should possibly drive me away.
	 * 3. If no enemies around, merge forces.
	 * 4. If no friends around, go to free space.
	 * */
	public static Point checkPresetRules(Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes){
		Set<Cell> friends = new HashSet<>();
		Set<Cell> enemies = new HashSet<>();
		for (Cell c : nearby_cells) {
			if (c.player == player_cell.player)
				friends.add(c);
			else
				enemies.add(c);
		}
		/* Calculate forces */
		Point fromFriends = joinForcesFromCells(player_cell, friends, 1.0, 1);
		Point fromEnemies = joinForcesFromCells(player_cell, enemies, 1.0, 1);
		Point fromPheromes = joinForcesFromPheromes(player_cell, nearby_pheromes, 0.5);
				
		Point moveDirection=null;
		if(player_cell.getDiameter()>=nearDelivery){
			System.out.println("Now the radius is " + player_cell.getDiameter() + ", go to free space for delivery.");
			moveDirection = headToFreeSpace(player_cell, nearby_cells, nearby_pheromes);
			moveDirection=ToolBox.normalizeDistance(moveDirection);
		}else if(friends.size()==0&&enemies.size()==0){
			System.out.println("Nothing around.");
			Point fromCells = new Point(0, 0);
			moveDirection = ToolBox.normalizeDistance(fromCells, fromPheromes);
			if (moveDirection.x == 0.0 && moveDirection.y == 0.0) {
				System.out.println("Nothing around. Decide the direction arbitrarily");
				moveDirection = ToolBox.normalizeDistance(new Point(gen.nextDouble()-0.5, gen.nextDouble()-0.5));
			}
		}else if(friends.size()==0){
			System.out.println("No friends around. Go to free space. ");
			moveDirection = headToFreeSpace(player_cell, nearby_cells, nearby_pheromes);
			moveDirection=ToolBox.normalizeDistance(moveDirection);
		}else if(enemies.size()==0){
			System.out.println("No enemies around. Merge forces. ");
			Point fromCells = fromFriends;
			moveDirection = ToolBox.normalizeDistance(fromCells, fromPheromes);
		}
		if(moveDirection!=null)
			System.out.println("According to the preset strategies, the direction is x: " + moveDirection.x + " y: " + moveDirection.y);
		return moveDirection;
	}
}
