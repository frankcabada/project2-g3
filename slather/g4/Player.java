package slather.g4;

import slather.sim.Cell;
import slather.sim.Point;
import slather.sim.Move;
import slather.sim.Pherome;
import java.util.*;

public class Player implements slather.sim.Player {

	private Random gen;
	private final static int ANGEL_RANGE = 360;
	private final static int NUMBER_OF_RANDOM_TRY = 4;
	private final static double PHEROME_IMPORTANCE = 0.2;
	private final static int SCALE = 3; // establish mapping from ANGEL_RANGE to byte so that every arg is in range [0, 120)
	private int tail;
	private double visible_distance;

	public void init(double d, int t, int side_length) {
		this.gen = new Random();
		this.visible_distance = d;
		this.tail = t;
	}

	private boolean f(Cell player_cell, Set<Cell> nearby_cells) {
		if (nearby_cells.size() != 1) return false;
		for (Cell nearby_cell : nearby_cells) {
			return nearby_cell.player == player_cell.player;
		}
		return true;
	}

	public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		if (player_cell.getDiameter() >= 2) // reproduce whenever possible
			return new Move(true, (byte) -1, (byte) -1);

		// care bout your children, dude!

		// strategies choosen branch
		int arg = 0;
		int spin_sep = get_spin_sep(this.tail, this.visible_distance); 
		int detector_sep = get_detector_sep(this.tail, this.visible_distance);
		if (memory < 0 && nearby_cells.size() > 0) {
			//System.out.println("here");
			arg = getOppositeDirection(player_cell, nearby_cells, memory);
			// System.out.println(arg);
		} else if (nearby_cells.size() == 0 ) {// f(player_cell, nearby_cells) ) {
			arg = memory;
		} else if (isCrowded(player_cell, nearby_cells, nearby_pheromes, 2) == true) {
			arg = spin(player_cell, memory, nearby_cells, nearby_pheromes, spin_sep);
		} else {
			arg = detector(player_cell, memory, nearby_cells, nearby_pheromes, detector_sep);
		}

		Point vector = extractVectorFromAngle(arg);
		if (!collides(player_cell, vector, nearby_cells, nearby_pheromes)) {
			return new Move(vector, (byte) arg);
		}

		// backup strategy: random escape
		for (int i = 0; i < Player.NUMBER_OF_RANDOM_TRY; i++) {
			arg = gen.nextInt(Player.ANGEL_RANGE / Player.SCALE) + 1;
			vector = extractVectorFromAngle(arg);
			if (!collides(player_cell, vector, nearby_cells, nearby_pheromes))
				return new Move(vector, (byte) arg);
		}
		return new Move(new Point(0, 0), (byte) 0);	
	}

	private int get_spin_sep(int tail, double visible_distance) {
		return 10;
	}

	private int get_detector_sep(int tail, double visible_distance) {
		return 4;
	}

	private double threshold(int tail, double visible_distance) {
		return 3;
	}

	private int getOppositeDirection(Cell player_cell, Set<Cell> nearby_cells, byte memory) {
		Cell nearest = null;
		double minDis = Double.MAX_VALUE;
		for (Cell c : nearby_cells) {
			if (c.distance(player_cell) < minDis) {
				minDis = c.distance(player_cell);
				nearest = c;
			}
		}

		Point pos = nearest.getPosition();
		double vx = player_cell.getPosition().x - pos.x;
		double vy = player_cell.getPosition().y - pos.y;

		double theta = Math.atan(vy/vx);
		int arg = (int)((theta / (2 * Math.PI) + (vx > 0 ? (vy < 0 ? 1 : 0) : 0.5)) * 120);
		return arg + gen.nextInt(30) - 15;
	}

	private int spin(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes, int seperation) {
		return (Player.ANGEL_RANGE / seperation) / Player.SCALE + memory;
	}

	private int detector(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes, int seperation) {
		double[] direction = new double[seperation];

		for (Cell nearby_cell : nearby_cells) {
			double m_x = nearby_cell.getPosition().x - player_cell.getPosition().x;
			double m_y = nearby_cell.getPosition().y - player_cell.getPosition().y;
			double theta = Math.atan(m_y/m_x);
			int index = (int) ((theta / (2 * Math.PI) + (m_x > 0 ? (m_y < 0 ? 1 : 0) : 0.5)) * seperation);
			direction[index] += trans1(nearby_cell.distance(player_cell));
		}

		for (Pherome nearby_pherome : nearby_pheromes) {
			if (nearby_pherome.player == player_cell.player) continue;
			double m_x = nearby_pherome.getPosition().x - player_cell.getPosition().x;
			double m_y = nearby_pherome.getPosition().y - player_cell.getPosition().y;
			double theta = Math.atan(m_y/m_x);
			int index = (int) ((theta / (2 * Math.PI) + (m_x > 0 ? (m_y < 0 ? 1 : 0) : 0.5)) * seperation);
			direction[index] += Player.PHEROME_IMPORTANCE * trans1(nearby_pherome.distance(player_cell));
		}

		double sum = 0;

		for (int index = 0; index < direction.length; ++index) {
			sum += trans2(direction[index]);
		}

		// We give each direction a probability direction[i]/sum
		sum *= Math.random();
		int index = 0;
		for (; index < direction.length; ++index) {
			sum -= trans2(direction[index]);
			if (sum <= 0) break;
		}

		return (index * Player.ANGEL_RANGE / seperation + gen.nextInt(Player.ANGEL_RANGE / seperation)) / Player.SCALE;
	}

	private boolean isCrowded(Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes, double d_filter) {
		return density(player_cell, nearby_cells, nearby_pheromes, d_filter) >= threshold(this.tail, this.visible_distance);
	}

	private double density(Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes, double d_filter) {
		double weightSum = 0;
		for (Cell nearby_cell : nearby_cells) {
			if (nearby_cell.distance(player_cell) < d_filter)
				weightSum += trans1(nearby_cell.distance(player_cell));
		}

		for (Pherome nearby_pherome : nearby_pheromes) {
			if (nearby_pherome.player != player_cell.player && nearby_pherome.distance(player_cell) < d_filter) {
				weightSum += Player.PHEROME_IMPORTANCE * trans1(nearby_pherome.distance(player_cell));
			}
		}
		return weightSum;
	}

	private double trans1(double a) {
		return 1.0/(a+0.5);
	}

	private double trans2(double a) {
		return 1/Math.pow((0.01+a),2);
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

	private Point extractVectorFromAngle(int arg) {
		double theta = Math.toRadians((double) arg * Player.SCALE);
		double dx = Cell.move_dist * Math.cos(theta);
		double dy = Cell.move_dist * Math.sin(theta);
		return new Point(dx, dy);
	}

	private double extractRatioFromVector(double dx, double dy) {
		return 0;
	}

}
