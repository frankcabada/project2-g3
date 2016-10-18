package slather.g7.strategies;

import java.util.HashSet;
import java.util.Set;

import slather.g7.ExplorerMemory;
import slather.g7.Memory;
import slather.g7.Strategy;
import slather.g7.ToolBox;
import slather.sim.Cell;
import slather.sim.Move;
import slather.sim.Pherome;
import slather.sim.Point;

public class ExplorerStrategy implements Strategy {
	static boolean considerRadius = true;// This is true because it's always
											// been true for Explorers
	static double cellWeight=1.0;
	static double pheromeWeight=0.5;
	
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
		System.out.println("Explorer strategy. Go out and back in iteratively.");
		Point nextStep=generateNextDirection(player_cell,memory,nearby_cells,nearby_pheromes);
		Memory nextMem = generateNextMoveMemory(memory);

		return new Move(nextStep,nextMem.getByte());
	}

	@Override
	public Point generateNextDirection(Cell player_cell, Memory memory, Set<Cell> nearby_cells,
			Set<Pherome> nearby_pheromes) {
		Point moveDirection;
		System.out.println("Cell is explorer.");
		ExplorerMemory memObj = (ExplorerMemory) memory;
		moveDirection = getCumulativeDirection(player_cell, nearby_cells, nearby_pheromes);
		// Last bit denotes direction of movement
		// If it is 1, move towards our own cells
		if (memObj.getOpposite() == 1) {
			moveDirection = new Point(-moveDirection.x, -moveDirection.y);
		}
		/* Add randomness if it's explorer */
		moveDirection = ToolBox.addRandomnessAndGenerateFinalDirection(moveDirection);
		return moveDirection;
	}

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
		/* Migrated from merging vectors to merging gravity force w.r.t. distance */
//		Point fromFriends = ToolBox.joinForcesFromCells(myCell, friends, 1.0, 1, considerRadius);
//		Point fromEnemies = ToolBox.joinForcesFromCells(myCell, enemies, 1.0, 1, considerRadius);
		Point fromFriends = ToolBox.joinGravityFromCells(myCell, friends, cellWeight, 1);
		Point fromEnemies = ToolBox.joinGravityFromCells(myCell, enemies, cellWeight, 1);
		Point fromCells = ToolBox.normalizeDistance(fromFriends, fromEnemies);

		/* If no friend or enemy, your own pherome will drive you away */
//		Point fromPheromes = ToolBox.joinForcesFromPheromes(myCell, nearby_pheromes, 0.5, 1);
		Point fromPheromes = ToolBox.joinGravityFromPheromes(myCell, nearby_pheromes, pheromeWeight, 1);

		Point normalized = ToolBox.normalizeDistance(fromCells, fromPheromes);

		if (normalized.x == 0.0 && normalized.y == 0.0) {
			System.out.println("Nothing around. Decide the direction arbitrarily");
			normalized = ToolBox.normalizeDistance(ToolBox.generateRandomDirection());
		}
		System.out.println("The direction is x: " + normalized.x + " y: " + normalized.y);

		return normalized;
	}

}
