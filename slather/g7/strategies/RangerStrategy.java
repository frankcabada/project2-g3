package slather.g7.strategies;

import java.util.Set;

import slather.g7.DefenderMemory;
import slather.g7.Memory;
import slather.g7.Player;
import slather.g7.Strategy;
import slather.g7.ToolBox;
import slather.sim.Cell;
import slather.sim.Move;
import slather.sim.Pherome;
import slather.sim.Point;

public class RangerStrategy implements Strategy{

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
		System.out.println("Ranger strategy. Go in circles.");
		Point nextStep=generateNextDirection(player_cell,memory,nearby_cells,nearby_pheromes);
		Memory nextMem = generateNextMoveMemory(memory);
		
		return new Move(nextStep,nextMem.getByte());
	}

	@Override
	public Point generateNextDirection(Cell player_cell, Memory memory, Set<Cell> nearby_cells,
			Set<Pherome> nearby_pheromes) {
		DefenderMemory mem=(DefenderMemory) memory;
		return drawCircle(player_cell,mem,Player.num_def_sides);
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
}
