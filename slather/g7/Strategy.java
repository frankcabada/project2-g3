package slather.g7;

import java.util.Set;

import slather.sim.Cell;
import slather.sim.Move;
import slather.sim.Pherome;
import slather.sim.Point;

public interface Strategy {
	
	public Memory generateNextMoveMemory(Memory currentMemory);
	public Memory generateFirstChildMemory(Memory currentMemory);
	public Memory generateSecondChildMemory(Memory currentMemory, Memory firstChildMemory);
	
	public Move generateMove(Cell player_cell, Memory memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes);
	
	public Point generateNextDirection(Cell player_cell, Memory memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes);
}
