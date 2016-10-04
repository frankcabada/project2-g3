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
	
	public static int t=4;

	@Override
	public void init(double d, int t) {
		gen = new Random();
		strategy = new ClusterStrategy();
	}

	@Override
	public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		if(nearby_pheromes.size()>0){
			Iterator<Pherome> it=nearby_pheromes.iterator();
			t=it.next().max_duration;
		}

		// Convert the byte memory to binary string for easier usage
		ExplorerMemory m = ExplorerMemory.getNewObject();
		m.initialize(memory);

		Move toTake = strategy.generateMove(player_cell, m, nearby_cells, nearby_pheromes);

		return toTake;
	}
}