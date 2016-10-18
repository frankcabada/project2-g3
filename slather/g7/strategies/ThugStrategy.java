package slather.g7.strategies;

import java.util.HashSet;
import java.util.Set;

import slather.g7.DummyMemory;
import slather.g7.Memory;
import slather.g7.Strategy;
import slather.g7.ToolBox;
import slather.sim.Cell;
import slather.sim.Move;
import slather.sim.Pherome;
import slather.sim.Point;

/*
 * This strategy chooses the biggest enemy cell in vision and goes towards it.
 * */
public class ThugStrategy implements Strategy{

	static double vision=2.0;
	
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
		System.out.println("Thug strategy. Attack the biggest enemy cell around.");
		Point nextStep=generateNextDirection(player_cell,memory,nearby_cells,nearby_pheromes);
		Memory nextMem = generateNextMoveMemory(memory);

		return new Move(nextStep,nextMem.getByte());
	}

	@Override
	public Point generateNextDirection(Cell player_cell, Memory memory, Set<Cell> nearby_cells,
			Set<Pherome> nearby_pheromes) {
		Set<Cell> toConsider=ToolBox.limitVisionOnCells(player_cell, nearby_cells, vision);
		
		//Find the target
		Cell toAttack=null;
		for(Cell c:toConsider){
			if(c.player!=player_cell.player){
				if(toAttack==null||toAttack.getDiameter()<c.getDiameter())
					toAttack=c;
			}
		}
		//Go to that direction
		Point toMove;
		if(toAttack==null){
			System.out.println("Nothing to attack. Go randomly.");
			toMove=ToolBox.generateRandomDirection();
		}else{
			Set<Cell> target=new HashSet<>();
			target.add(toAttack);
			toMove=ToolBox.joinForcesFromCells(player_cell, target, 1.0, -1, false);
			
		}
		return ToolBox.normalizeDistance(toMove);
	}

}
