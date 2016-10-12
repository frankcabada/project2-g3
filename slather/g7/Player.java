package slather.g7;

import java.util.*;
import java.util.Map.Entry;

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
		try {
			// Check the type of cell
			String memStr = DefenderMemory.byteToString(memory);
			char defOrExp = memStr.charAt(0);
			// System.out.println("Cell type flag: "+defOrExp);
			/*
			 * 0: Explorer 1: Defender
			 */

			// Convert the byte memory to binary string for easier usage

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
				System.out.println("The cell is not recognized!");
				m = DefenderMemory.getNewObject();
			}

			Move toTake = strategy.generateMove(player_cell, m, nearby_cells, nearby_pheromes);
			System.out.println("New memory is " + toTake.memory);
			return toTake;
		} catch (Exception e) {
			System.out.println("========= Exception in player =========");
			e.printStackTrace();
			return null;
		}
	}

	public Strategy getStrategy() {
		return strategy;
	}
}