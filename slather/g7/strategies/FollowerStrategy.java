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
 * This strategy follows the closest friend in vision.
 * */

public class FollowerStrategy implements Strategy{
	static double vision=5.0;

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
		System.out.println("Follower strategy. Head to the closest friend I see.");
		Point nextStep=generateNextDirection(player_cell,memory,nearby_cells,nearby_pheromes);
		Memory nextMem = generateNextMoveMemory(memory);

		return new Move(nextStep,nextMem.getByte());
	}

	@Override
	public Point generateNextDirection(Cell player_cell, Memory memory, Set<Cell> nearby_cells,
			Set<Pherome> nearby_pheromes) {
		//Only chase the friends that can possibly be reached?
		Set<Cell> near=ToolBox.limitVisionOnCells(player_cell, nearby_cells, vision);
		
		double shortestDist=100.0;
		Cell toFollow=null;
		for(Cell c:near){
			if(c.player==player_cell.player){
				double distBetween=c.getPosition().distance(player_cell.getPosition());
				if(distBetween<shortestDist){
					toFollow=c;
					shortestDist=distBetween;
				}
			}
		}
		Point toMove;
		if(toFollow==null){
			System.out.println("Closest friend not found. Decide direction randomly.");
			toMove=ToolBox.generateRandomDirection();
		}else{
			System.out.println("Going to closest friend "+toFollow.getPosition());
			Set<Cell> togo=new HashSet<>();
			togo.add(toFollow);
			toMove=ToolBox.joinForcesFromCells(player_cell, togo, 1.0, -1, false);
		}
		
		return ToolBox.normalizeDistance(toMove);
	}

}
