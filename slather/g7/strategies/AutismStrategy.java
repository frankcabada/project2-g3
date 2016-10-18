package slather.g7.strategies;

import java.util.Set;

import slather.g7.DummyMemory;
import slather.g7.Grouping;
import slather.g7.Memory;
import slather.g7.Strategy;
import slather.g7.ToolBox;
import slather.sim.Cell;
import slather.sim.Move;
import slather.sim.Pherome;
import slather.sim.Point;

/*
 * This strategy will keep a distance to everyone around it.
 * Basically it combines vectors with regard to the square of distance
 * */
public class AutismStrategy implements Strategy{

	static double cellWeight=1.0;
	static double pheromeWeight=0.5;
	
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
		System.out.println("Autism Strategy. Keep a distance with everyone.");
		Point nextStep=generateNextDirection(player_cell,memory,nearby_cells,nearby_pheromes);
		Memory nextMem = generateNextMoveMemory(memory);

		return new Move(nextStep,nextMem.getByte());
	}

	@Override
	public Point generateNextDirection(Cell player_cell, Memory memory, Set<Cell> nearby_cells,
			Set<Pherome> nearby_pheromes) {
		Grouping group=new Grouping(player_cell,nearby_cells,nearby_pheromes);
		
		Point fromCells=ToolBox.joinGravityFromCells(player_cell, nearby_cells, cellWeight, 1);
		Point fromPheromes=ToolBox.joinGravityFromPheromes(player_cell, nearby_pheromes, pheromeWeight, 1);
		
		Point toMove=ToolBox.normalizeDistance(fromCells,fromPheromes);
		
		return toMove;
	}

}
