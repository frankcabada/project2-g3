package slather.g7;

import java.util.*;
import java.util.Map.Entry;

import slather.g7.strategies.StrategyFactory;
import slather.sim.Cell;
import slather.sim.GridObject;
import slather.sim.Move;
import slather.sim.Pherome;
import slather.sim.Point;

public class Player implements slather.sim.Player {

	private Random gen;
	private Strategy strategy;

	public static int T;
	public static double D;
	public static int num_def_sides;
	public static double vision = 7.0;

	// This map will contain all the scenario -> strategy info
	public static HashMap<Scenario, StrategyType> strategyPerScenario;

	static {
		initScenarioStrategyMapping();
	}

	public static void initScenarioStrategyMapping() {
		strategyPerScenario = new HashMap<>();
		strategyPerScenario.put(Scenario.CLUSTER_BORDER, StrategyType.EXPLORER);
		strategyPerScenario.put(Scenario.DISPERSED, StrategyType.EXPLORER);
		strategyPerScenario.put(Scenario.EMPTYNESS, StrategyType.FREE_SPACE);
		strategyPerScenario.put(Scenario.ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.FRIENDS, StrategyType.TO_ENEMIES);
		strategyPerScenario.put(Scenario.NEARBY_ENEMIES, StrategyType.MOVE_TO_TARGET);
		strategyPerScenario.put(Scenario.NEARBY_FRIENDS, StrategyType.TO_ENEMIES);
		strategyPerScenario.put(Scenario.ONE_PHEROMONE, StrategyType.TO_ENEMIES);

		strategyPerScenario.put(Scenario.ALMOST_REPRODUCTION, StrategyType.KEEP_DISTANCE);

		strategyPerScenario.put(Scenario.MAX_SIZE, StrategyType.REPRODUCE);
		//System.out.println("Do nothing in this static init function.");
	}

	@Override
	public void init(double d, int t, int side_length) {
		T = t;
		D = d;
		gen = new Random();
		strategy = new ClusterStrategy();
		num_def_sides = Integer.min(16, Integer.max(4, T));
	}

	@Override
	public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		System.out.println("Memory is " + memory);

		/*
		 * Restricting vision for cells to a maximum distance
		 */
		nearby_cells = ToolBox.limitVisionOnCells(player_cell, nearby_cells, vision);
		nearby_pheromes = ToolBox.limitVisionOnPheromes(player_cell, nearby_pheromes, vision);

		try {
			// Check the type of cell
			String memStr = DefenderMemory.byteToString(memory);
			char defOrExp = memStr.charAt(0);
			// System.out.println("Cell type flag: "+defOrExp);
			/*
			 * 0: Explorer 1: Defender
			 */
			Memory m;
			if (defOrExp == '0') {
				ExplorerMemory thisMem = ExplorerMemory.getNewObject();
				thisMem.initialize(memory);
				m = thisMem;
			} else if (defOrExp == '1') {
				DefenderMemory thisMem = DefenderMemory.getNewObject();
				thisMem.initialize(memory);
				m = thisMem;
			} else {
				System.out.println("The cell is not recognized with bit " + defOrExp);
				m = DefenderMemory.getNewObject();
			}

			// Convert the byte memory to binary string for easier usage
			Scenario currentScenario = getScenario(player_cell, nearby_cells, nearby_pheromes);
			StrategyType st = strategyPerScenario.get(currentScenario);
			Move toTake;
			if (st == null) {
				System.out.println(
						"Error: Corresponding strategy for scenario " + currentScenario.name() + " is not found!");
				System.out.println("Fall back to ClusterStrategy");
				toTake = strategy.generateMove(player_cell, m, nearby_cells, nearby_pheromes);
				System.out.println("New memory for ClusterStrategy " + toTake.memory);
			} else if (st == StrategyType.REPRODUCE) {
				System.out.println("Retrieved strategy " + st.name() + " for scenario " + currentScenario.name());
				Strategy mindSet = StrategyFactory.getStrategyByType(st);
				toTake = mindSet.generateMove(player_cell, m, nearby_cells, nearby_pheromes);
				System.out.println("Reproducing.");
				return toTake;
			} else {
				System.out.println("Retrieved strategy " + st.name() + " for scenario " + currentScenario.name());
				Strategy mindSet = StrategyFactory.getStrategyByType(st);
				toTake = mindSet.generateMove(player_cell, m, nearby_cells, nearby_pheromes);
				System.out.println("New memory for flexible Strategy is " + toTake.memory);
			}

			/*
			 * Todo: Add distance check to ensure that we can grow after move.
			 */
			Point dir = toTake.vector;
			System.out.println("Checking the distance for growth.");
			dir = ToolBox.checkSpaceForGrowth(player_cell, dir, nearby_cells, nearby_pheromes);
			toTake = new Move(dir, toTake.memory);
			System.out.println("Checked direction with regard to growth is x:" + dir.x + " y:" + dir.y);

			return toTake;
		} catch (Exception e) {
			System.out.println("========= Exception in player =========");
			e.printStackTrace();
			return null;
		}
	}

	private Scenario getScenario(Cell myCell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		if (myCell.getDiameter() >= 2.0) {
			return Scenario.MAX_SIZE;
		}
		if (ScenarioClass.isAboutToReproduce(myCell, nearby_cells, nearby_pheromes)) {
			return Scenario.ALMOST_REPRODUCTION;
		}
		if (ScenarioClass.isEmpty(myCell, nearby_cells, nearby_pheromes)) {
			return Scenario.EMPTYNESS;
		}
		if (ScenarioClass.isOnlyMyPherome(myCell, nearby_cells, nearby_pheromes)) {
			return Scenario.ONE_PHEROMONE;
		}
		if (ScenarioClass.isClusterBorder(myCell, nearby_cells, nearby_pheromes)) {
			return Scenario.CLUSTER_BORDER;
		}
		if (ScenarioClass.isOnlyFriends(myCell, nearby_cells, nearby_pheromes)) {
			return Scenario.FRIENDS;
		}
		if (ScenarioClass.isOnlyEnemies(myCell, nearby_cells, nearby_pheromes)) {
			return Scenario.ENEMIES;
		}
		if (ScenarioClass.isNearbyFriends(myCell, nearby_cells, nearby_pheromes)) {
			return Scenario.NEARBY_FRIENDS;
		}
		if (ScenarioClass.isNearbyEnemies(myCell, nearby_cells, nearby_pheromes)) {
			return Scenario.NEARBY_ENEMIES;
		}

		return Scenario.DISPERSED;
	}

	public Strategy getStrategy() {
		return strategy;
	}
}