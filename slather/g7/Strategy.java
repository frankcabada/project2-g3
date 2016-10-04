package slather.g7;

import java.util.Set;

import slather.sim.Cell;
import slather.sim.Move;
import slather.sim.Pherome;

public interface Strategy {
	
	public Memory getNewMemoryObject();
	
	public Memory generateNextMoveMemory(Memory currentMemory);
	public Memory generateFirstChildMemory(Memory currentMemory);
	public Memory generateSecondChildMemory(Memory currentMemory, Memory firstChildMemory);
	
	public Move generateMove(Cell player_cell, Memory memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes);
}
