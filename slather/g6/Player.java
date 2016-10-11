package slather.g6;

import slather.sim.Cell;
import slather.sim.Point;
import slather.sim.Move;
import slather.sim.Pherome;
import java.util.*;

public class Player implements slather.sim.Player {

	private Random gen;
	private double d;
	private int t;
	
	private int turn;
	private double diameter;	/* used in determining the initial # of cells */
	private int initialCells;
	private int totalCells;
	private boolean determine;
	
	private boolean squaring;
	private int movesPerSide;
	private int totalOfSides;
	private MaxAnglePlayer maxAnglePlayer = new MaxAnglePlayer();

	public void init(double d, int t, int sideLength) {
		gen = new Random();
		this.d = d;
		this.t = t;
		this.turn = 0;
		this.diameter = 0;
		this.initialCells = 0;
		this.determine = true;
		
		this.movesPerSide = t / 4;	// 4 for the # of sides in a square
		this.totalOfSides = movesPerSide * 4;
		this.squaring = false;
	}

	public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		/* successfully records starting cell count into this.initialCells */
		/* records heirarchical reproduction count in memory */
		
		//uncomment to use max angle player 
		return maxAnglePlayer.play(player_cell, memory, nearby_cells, nearby_pheromes);
		/*if (turn == 0) {
			this.diameter = player_cell.getDiameter();
			this.initialCells++;
		}
		
		if (determine == true && turn != 0) {
			if (player_cell.getDiameter() == this.diameter) {
				this.initialCells++;
			} else {
				determine = false;
				this.totalCells = this.initialCells;
				 debug 
				System.out.println("Number of starting cells = " + this.initialCells);
			}
		}
		turn++;
		
		if (player_cell.getDiameter() >= 2) { // reproduce whenever possible
			
			int daughter_mem = Math.abs(memory - 90) % 180;
			
			 debug 
			System.out.printf("memory = %d\n", memory);
			System.out.printf("daughter mem = %d\n", daughter_mem);
			
			this.totalCells++;
			
			return new Move(true, memory, (byte) daughter_mem);
		}
		
		 squaring strategy 
		if (squaring) {
			int tryNum = 0;
			Point vector = null;

			while (tryNum < 4) {
				vector = this.squaringStrat(player_cell, memory);
				if (this.collides(player_cell, vector, nearby_cells, nearby_pheromes)) {
					memory += this.movesPerSide;
					tryNum++;
				} else {
					memory++;
					break;
				}
			}
			// double curX = player_cell.getPosition().x;
			// double curY = player_cell.getPosition().y;
			int angle = this.extractAngleFromVector(vector, player_cell);
			Point newVector = this.extractVectorFromAngle(angle);
			// System.out.printf("move from (%f, %f) ", curX, curY);
			// System.out.printf("to (%f, %f)\n", newVector.x, newVector.y);
			return new Move(newVector, memory);
		}
		

		
		 * go in opposite direction of opposing cells, doesn't currently use the
		 * memory
		 
		Set<Cell> friendly_cells = new HashSet<Cell>();
		Set<Cell> enemy_cells = new HashSet<Cell>();
		Set<Cell> two_closest_enemies = new HashSet<Cell>();
		if (!nearby_cells.isEmpty()) {

			// sort points into friendly and enemy cell positions
			for (Cell currCell : nearby_cells) {
				if (currCell.player == 6) {
					friendly_cells.add(currCell);
				} else {
					enemy_cells.add(currCell);
				}
			}

			
			 * use the closest 2 enemy cells to determine your direction of
			 * movement. use 1 enemy cell if there is only one enemy in your
			 * vicinity
			 
			Point vector;
			ArrayList<Cell> sortedCells = this.sort(enemy_cells, player_cell);
			if(!sortedCells.isEmpty()) {
				if (sortedCells.size() >= 2) {
					Cell cellOne = sortedCells.get(0);
					Cell cellTwo = sortedCells.get(1);
					vector = avoidEnemies(player_cell, cellOne, cellTwo);
				} else { // only 1 enemy cell in vicinity of player_cell
					Cell cellOne = sortedCells.get(0);
					vector = avoidEnemies(player_cell, cellOne);
				}
				if (!collides(player_cell, vector, nearby_cells, nearby_pheromes))
					return new Move(vector, memory);
			}
		}
		
		 follow previous direction unless it would cause a collision 
		if (memory > 0) { 
			Point vector = extractVectorFromAngle((int) memory);
			// check for collisions
			if (!collides(player_cell, vector, nearby_cells, nearby_pheromes))
				return new Move(vector, memory);
		}
		
		 
		 * if no previous direction specified or if there was a collision, try
		 * random directions to go in until one doesn't collide 
		 
		for (int i = 0; i < 4; i++) {
			int arg = gen.nextInt(180) + 1;
			Point vector = extractVectorFromAngle(arg);
			if (!collides(player_cell, vector, nearby_cells, nearby_pheromes))
				return new Move(vector, (byte) arg);
		}

		// if all tries fail, just chill in place
		return new Move(new Point(0, 0), memory);*/
	}

	/* sort cells from smallest distance from player_cell to greatest */
	private ArrayList<Cell> sort(Set<Cell> cells, Cell player_cell) {
		ArrayList<Cell> orderedCells = new ArrayList<Cell>();
		Map<Double, Cell> dict = new HashMap<Double, Cell>();
		List<Double> dist_list = new ArrayList<Double>();

		for (Cell cell : cells) {
			double dist = player_cell.distance(cell);
			dict.put(dist, cell);
		}

		Set<Double> keys = dict.keySet();
		for(double key : keys) {
			dist_list.add(key);
		}
		
		Collections.sort(dist_list);

		// for debugging
		// System.out.println(keys.toString());

		for (double key : dist_list) {
			orderedCells.add(dict.get(key));
		}

		return orderedCells;
	}

	private Point avoidEnemies(Cell pl_cell, Cell one) {
		int enemy_dir = extractAngleFromVector(one.getPosition(), pl_cell);
		enemy_dir *= 2; // back to 360 degrees for easier mental arithmetic
		int my_cell_dir = (enemy_dir + 180) % 360; // opposite direction of
													// enemy
		my_cell_dir /= 2;
		return this.extractVectorFromAngle(my_cell_dir);
	}

	/*
	 * invert (x, y) coordinate values for one and two. ex: (-1, 5) -> (1, -5)
	 * then add the angles of cells one and two to determine desired direction
	 */
	private Point avoidEnemies(Cell pl_cell, Cell one, Cell two) {
		double x_one = one.getPosition().x;
		double y_one = one.getPosition().y;
		double x_two = two.getPosition().x;
		double y_two = two.getPosition().y;
		Point invertedOne = new Point(-x_one, -y_one);
		Point invertedTwo = new Point(-x_two, -y_two);
		int angle_one = this.extractAngleFromVector(invertedOne, pl_cell);
		int angle_two = this.extractAngleFromVector(invertedTwo, pl_cell);
		int desired_angle = (angle_one + angle_two) / 2;
		return this.extractVectorFromAngle(desired_angle);
	}

	// check if moving player_cell by vector collides with any nearby cell or
	// hostile pherome
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

	// convert an angle (in 2-deg increments) to a vector with magnitude
	// Cell.move_dist (max allowed movement distance)
	private Point extractVectorFromAngle(int arg) {
		double theta = Math.toRadians(2 * (double) arg);
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
	
	/*
	 * squaring strategy only worthwhile if t >= 4.
	 * uses memory to determine up, down, left or right.
	 */
	private Point squaringStrat(Cell current, Byte memory) {

		int dir = memory % this.totalOfSides;
		int right = this.movesPerSide;
		int down = right * 2;
		int left = right * 3;
		int up = right * 4;

		double x = current.getPosition().x;
		double y = current.getPosition().y;

		if (dir < right) {
			x += Cell.move_dist;
		} else if (dir < down) {
			y -= Cell.move_dist;
		} else if (dir < left) {
			x -= Cell.move_dist;
		} else if (dir < up) {
			y += Cell.move_dist;
		}
		return new Point(x, y);
	}

}
