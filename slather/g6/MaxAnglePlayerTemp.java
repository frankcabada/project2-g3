package slather.g6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import slather.sim.Cell;
import slather.sim.Move;
import slather.sim.Pherome;
import slather.sim.Player;
import slather.sim.Point;

public class MaxAnglePlayerTemp implements Player {
	private static int cell_vision = 2;

	@Override
	public void init(double d, int t, int side) {
		// TODO Auto-generated method stub
	}

	@Override
	public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		// TODO Auto-generated method stub
		Point vector = new Point(0, 0);
		if (player_cell.getDiameter() >= 2) {
			return new Move(true, (byte) -1, (byte) -1);
		}
		if (!nearby_cells.isEmpty()) {
			Set<Cell> cells = findCellInRange(nearby_cells, player_cell);
			if (cells.size() == 1) {
				vector = avoidCell(player_cell, cells.iterator().next());
			} else if (cells.size() >= 2) {
				// find best angle
				vector = findBestDirection(cells, player_cell);
			} else {
				vector = new Point(0, 0);
			}

		} else {
			// if no cell around, stay put/follow previous direction/go circle
			vector = new Point(0, 0);
		}
		if (!collides(player_cell, vector, nearby_cells, nearby_pheromes))
			return new Move(vector, memory);
		
		// if no previous direction specified or if there was a collision, try
		// random directions to go in until one doesn't collide
		for (int i = 0; i < 4; i++) {
			Random gen = new Random();
			int arg = gen.nextInt(180) + 1;
			vector = extractVectorFromAngle(arg);
			if (!collides(player_cell, vector, nearby_cells, nearby_pheromes))
				return new Move(vector, (byte) arg);
		}

		// if all tries fail, just chill in place
		return new Move(new Point(0, 0), (byte) 0);
	}

	private Point findBestDirection(Set<Cell> cells, Cell player_cell) {
		// TODO Auto-generated method stub
		Cell[] sortedCells = sortCell(cells, player_cell);
		int largestAngle = Integer.MIN_VALUE;
		Cell current = sortedCells[0];
		int directionAngle = extractAngleFromVector(current.getPosition(), player_cell);
		/*if (cells.size() != sortedCells.length)
			System.out.println("Cells length" + cells.size() + ". Sorted cell length: " + sortedCells.length);
		System.out.println("\n" + sortedCells.length);*/
		/*for (Cell cell : sortedCells) {
			System.out.println(cell.getPosition().x + "," + cell.getPosition().y);
		}*/

		for (int i = 1; i < sortedCells.length && sortedCells[i] != null; i++) {
			int currentAngle = Math.abs(extractAngleFromVector(sortedCells[i].getPosition(), player_cell)
					- extractAngleFromVector(current.getPosition(), player_cell));
			if (currentAngle > largestAngle) {
				largestAngle = currentAngle;
				directionAngle = extractAngleFromVector(sortedCells[i].getPosition(), player_cell) - largestAngle / 2;
			}
			current = sortedCells[i];
		}
		if (sortedCells[sortedCells.length - 1] != null) {
			int currentAngle = Math
					.abs(extractAngleFromVector(sortedCells[sortedCells.length - 1].getPosition(), player_cell)
							- extractAngleFromVector(sortedCells[0].getPosition(), player_cell));
			if (currentAngle > largestAngle) {
				largestAngle = currentAngle;
				directionAngle = extractAngleFromVector(sortedCells[0].getPosition(), player_cell) - largestAngle / 2;
			}
		}
		// System.out.println("Largest angle:"+ directionAngle);

		return extractVectorFromAngle(directionAngle);
	}


	// sort cells by its angle with play cell
	private Cell[] sortCell(Set<Cell> cells, Cell player_cell) {
		Cell[] orderedCells = new Cell[cells.size()];
		Map<Integer, Cell> cellAngleMap = new HashMap<Integer, Cell>();
		List<Integer> angleSet = new ArrayList<Integer>();
		for (Cell cell : cells) {
			int angle = extractAngleFromVector(cell.getPosition(), player_cell);
			if (!cellAngleMap.containsKey(angle)) {
				cellAngleMap.put(angle, cell);
				angleSet.add(angle);
			}
		}
		Collections.sort(angleSet);
		int i = 0;
		for (int key : angleSet) {
			orderedCells[i++] = cellAngleMap.get(key);
		}

		return orderedCells;
	}

	private Set<Cell> findCellInRange(Set<Cell> cells, Cell player_cell) {
		Set<Cell> cell_List = new HashSet<>();
		for (Cell cell : cells) {
			if (player_cell.distance(cell) <= cell_vision) {
				cell_List.add(cell);
			}
		}
		return cell_List;
	}

	private Point avoidCell(Cell pl_cell, Cell one) {
		int enemy_dir = extractAngleFromVector(one.getPosition(), pl_cell);
		enemy_dir *= 2; // back to 360 degrees for easier mental arithmetic
		int my_cell_dir = (enemy_dir + 180) % 360; // opposite direction of
													// enemy
		my_cell_dir /= 2;
		return this.extractVectorFromAngle(my_cell_dir);
	}

	private boolean collides(Cell player_cell, Point vector, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		Iterator<Cell> cell_it = nearby_cells.iterator();
		Point destination = player_cell.getPosition().move(vector);
		while (cell_it.hasNext()) {
			Cell other = cell_it.next();
			if (destination.distance(other.getPosition()) < 0.5 * player_cell.getDiameter() + 0.5 * other.getDiameter()
					+ 0.00011)
				return true;
		}
		Iterator<Pherome> pherome_it = nearby_pheromes.iterator();
		while (pherome_it.hasNext()) {
			Pherome other = pherome_it.next();
			if (other.player != player_cell.player
					&& destination.distance(other.getPosition()) < 0.5 * player_cell.getDiameter() + 0.0001)
				return true;
		}
		return false;
	}

	public Point extractVectorFromAngle(int angel) {
		double theta = Math.toRadians(2 * angel);
		double dx = Cell.move_dist * Math.cos(theta);
		double dy = Cell.move_dist * Math.sin(theta);
		return new Point(dx, dy);
	}

	/*
	 * compute the angle from Point arg. should be out of 180 since it deals in
	 * angles in 2-deg increments. referenced
	 * http://www.davdata.nl/math/vectdirection.html for extrapolating angles
	 * from vectors.
	 */
	private int extractAngleFromVector(Point arg, Cell player_cell) {
		double x = player_cell.getPosition().x;
		double y = player_cell.getPosition().y;

		if (x == arg.x) { // cell is either directly above or below ours
			if (y > arg.y) { // go up
				return 45; // 90/2
			} else { // otherwise go down
				return 135; // 270/2
			}
		}

		double dx = arg.x - x;
		double dy = arg.y - y;
		double angle = Math.atan(dy / dx);
		if (arg.x < x)
			angle += Math.PI;
		if (angle < 0)
			angle += 2 * Math.PI;
		return (int) (Math.toDegrees(angle) / 2);
	}
}