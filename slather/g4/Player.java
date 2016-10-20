package slather.g4;

import slather.sim.Cell;
import slather.sim.Point;
import slather.sim.GridObject;
import slather.sim.Move;
import slather.sim.Pherome;
import java.util.*;

public class Player implements slather.sim.Player {

	private Random gen;
	// weight parameters
	private final static int NUMBER_OF_RANDOM_TRY = 10;
	private final static double PHEROME_IMPORTANCE = 0.2;

	// range for tail length
	private final static int LOWEST_LEN_OF_TAIL = 10;

	// angle
	private final static int ANGLE_RANGE = 360;
	private final static int SCALE = 3; // establish mapping from ANGLE_RANGE to byte so that every arg is in range [0, 120)
	private int our_angle_range;
	
	// input variable
	private int tail;
	private double visible_distance;
	private double side;

	public void init(double d, int t, int side_length) {
		this.gen = new Random();
		this.visible_distance = d;
		this.tail = t;
		this.side = side_length;
		this.our_angle_range = Player.ANGLE_RANGE / Player.SCALE;
	}

	public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		if (player_cell.getDiameter() >= 2) // reproduce whenever possible
			return new Move(true, (byte) -128, (byte) -128);
			//return new Move(true, (byte) -128, (byte) -128);

		// care bout your children, dude!

		// strategies choosen branch
		int arg = 0;
		int spin_sep = get_spin_sep(this.tail, this.visible_distance); 
		int detector_sep = get_detector_sep(this.tail, this.visible_distance);
		double speed = getSpeed(player_cell, nearby_cells, nearby_pheromes, 2);

		if (memory == -128 && nearby_cells.size() > 0 && this.visible_distance >= 10) {
			arg = getOppositeDirection(player_cell, nearby_cells);
		} else if (nearby_cells.size() == 0) {
			arg = memory;
		} else if (isCrowded(player_cell, nearby_cells, nearby_pheromes, 2) == true) {

			int tmp_arg = 0;
			if (memory < 0 && memory >= -120) {// spin in opposite direction
				tmp_arg = spin(player_cell, (byte)(-memory - 1) , nearby_cells, nearby_pheromes, spin_sep, false);
				//arg = spin(player_cell, -memory+1, nearby_cells, nearby_pheromes, spin_sep, false);
			} else {
				tmp_arg = spin(player_cell, memory, nearby_cells, nearby_pheromes, spin_sep, true);
				//arg = spin(player_cell, memory, nearby_cells, nearby_pheromes, spin_sep, true);
			}
			
			Point tmp_vector = extractVectorFromAngle(tmp_arg, speed);

			if (!collides(player_cell, tmp_vector, nearby_cells, nearby_pheromes)) {
				if (memory < 0 && memory >= -120) {
					return new Move(tmp_vector, (byte) (-tmp_arg - 1));
				} else {
					return new Move(tmp_vector, (byte) tmp_arg);	
				}
				
			} else {
				arg = (memory + 60) % 120;

				//for (double s = speed ; s > 0.1 ; s -= 0.1) {
					tmp_vector = extractVectorFromAngle(arg, speed);
					if (!collides(player_cell, tmp_vector, nearby_cells, nearby_pheromes)) {
						return new Move(tmp_vector, (byte) (-arg-1));
					}
				//}
			}	

		} else {
			arg = detector(player_cell, memory, nearby_cells, nearby_pheromes, detector_sep);
		}

		Point vector = null;
		for (double s = speed; s > 0.1 ; s -= 0.1) {
			 vector = extractVectorFromAngle(arg, s);
			if (!collides(player_cell, vector, nearby_cells, nearby_pheromes)) {
				//System.out.println(" " + vector.x + ":" + vector.y + " ");
				return new Move(vector, (byte) arg);
			}
		}

		// backup strategy: random escape
		// TODO: add scale
		for (double s = speed ; s > 0.1 ; s -= 0.1) {
			for (int i = 0; i < Player.NUMBER_OF_RANDOM_TRY; i++) {
				arg = gen.nextInt(this.our_angle_range) + 1;
				vector = extractVectorFromAngle(arg, s);
				if (!collides(player_cell, vector, nearby_cells, nearby_pheromes))
					//System.out.println(" " + vector.x + ":" + vector.y + " ");
					return new Move(vector, (byte) arg);
				
			}
		}

		return new Move(new Point(0, 0), (byte) 0);	
	}

	/* get input variable determined parameters */
	private int get_spin_sep(int tail, double visible_distance) {
		return Math.max(tail, Player.LOWEST_LEN_OF_TAIL);
	}
	private int get_detector_sep(int tail, double visible_distance) {
		return 4;
	}
	private double isCrowdedThreshold(int tail, double visible_distance) {
		return 100;
	}
	private double getSpeed(Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes, double d_filter) {
		double dens = density(player_cell, nearby_cells, nearby_pheromes, d_filter);
		double thre = isCrowdedThreshold(tail, visible_distance);
		return Math.min(1, thre/dens);
	}

	private int getOppositeDirection(Cell player_cell, Set<Cell> nearby_cells) {
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
		int arg = (int) extractAngleFromVector(vx, vy);
		return arg; //gen.nextInt(10) - 5;
	}

	private int getOppositeDirectionP(Cell player_cell, Set<Pherome> nearby_pheromes) {
		Pherome nearest = null;
		double minDis = Double.MAX_VALUE;
		for (Pherome c : nearby_pheromes) {
			if (c.distance(player_cell) < minDis && c.player != player_cell.player) {
				minDis = c.distance(player_cell);
				nearest = c;
			}
		}

		Point pos = nearest.getPosition();
		double vx = player_cell.getPosition().x - pos.x;
		double vy = player_cell.getPosition().y - pos.y;
		int arg = (int) extractAngleFromVector(vx, vy);
		return arg; //+ gen.nextInt(30) - 15;
	}

	private int spin(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes, int seperation) {
		return ((this.our_angle_range) / seperation + memory) % (this.our_angle_range);
	}


	private int spin(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes, int seperation, boolean flag) {
		if (flag == true)
			return	((Player.ANGLE_RANGE / seperation) / Player.SCALE + memory) % 120 ;
		else 
			return ( (memory - (Player.ANGLE_RANGE / seperation) / Player.SCALE) % 120 + 120) %120 ;
	}



	private int findLargestGap(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {

		int count_pherome = nearby_pheromes.size();
		for (Pherome nearby_pherome : nearby_pheromes) {
			if (nearby_pherome.player == player_cell.player) count_pherome --;
		}
		if (count_pherome == 1 && nearby_cells.size() == 0) {
			return getOppositeDirectionP(player_cell, nearby_pheromes);
		} else if (count_pherome == 0 && nearby_cells.size() == 1) {
			return getOppositeDirection(player_cell, nearby_cells);
		}

		PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> b[0] - a[0]);
		List<Integer> l = new ArrayList<>();

		for (Cell nearby_cell : nearby_cells) {
			double dy = nearby_cell.getPosition().y - player_cell.getPosition().y;
			double dx = nearby_cell.getPosition().x - player_cell.getPosition().x;
			l.add((int) extractAngleFromVector(dx, dy));
		}
		for (Pherome nearby_pherome : nearby_pheromes) {
			if (nearby_pherome.player == player_cell.player) continue;
			double dy = nearby_pherome.getPosition().y - player_cell.getPosition().y;
			double dx = nearby_pherome.getPosition().x - player_cell.getPosition().x;
			l.add((int) extractAngleFromVector(dx, dy));
		}

		Collections.sort(l);
		for (int i=0 ; i<l.size() ; ++i) {
			int gap = 0;
			int direction = 0;

			if (i == l.size()-1) {
				gap = l.get(0) - l.get(i) + 120;
				direction = (l.get(0) - gap/2 + 120)%120;
			} else {
				gap = l.get(i+1) - l.get(i);
				direction = (l.get(i+1) + l.get(i))/2;
			}
			
			
			int[] pair = {gap, direction};
			pq.offer(pair);
		}

		int size = pq.size();
		int[] top = pq.poll();
		return top[1];
	}

	private int densityRadar(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes, int seperation) {
		double[] direction = new double[seperation];

		for (Cell nearby_cell : nearby_cells) {
			double m_x = nearby_cell.getPosition().x - player_cell.getPosition().x;
			double m_y = nearby_cell.getPosition().y - player_cell.getPosition().y;
			int index = (int) (extractRatioFromVector(m_x, m_y) * seperation);
			direction[index] += trans1(nearby_cell.distance(player_cell));
		}

		for (Pherome nearby_pherome : nearby_pheromes) {
			if (nearby_pherome.player == player_cell.player) continue;
			double m_x = nearby_pherome.getPosition().x - player_cell.getPosition().x;
			double m_y = nearby_pherome.getPosition().y - player_cell.getPosition().y;
			int index = (int) (extractRatioFromVector(m_x, m_y) * seperation);
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

		return (index * this.our_angle_range / seperation + gen.nextInt(this.our_angle_range / seperation)) % this.our_angle_range;
	}

	private int detector(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes, int seperation) {
		if (this.visible_distance < 10) {
			// TODO: remove hardcode
			//ArrayList<GridObject> nearby_obstacles = getNearbyObstaclesEscapeSelf(player_cell, nearby_cells, nearby_pheromes);
			Set<Cell> nearby_same = new HashSet<>();
			Set<Cell> nearby_different = new HashSet<>();
			Set<Pherome> nearby_pheromes_empty = new HashSet<>(); 

			for (Cell cell : nearby_cells) {
				//if (player_cell.getPosition().distance(cell.getPosition()) < 5 * player_cell.getDiameter()){
                if (cell.player == player_cell.player){
                    nearby_same.add(cell);
                } else {
                    nearby_different.add(cell);
                }
                //nearby_obstacles.add(cell);
            //} 
        	}
        	if(nearby_same.size() * 1.5 > nearby_different.size()){
            	return findLargestGap(player_cell, memory, nearby_same, nearby_pheromes_empty);
        	}
			return findLargestGap(player_cell, memory, nearby_cells, nearby_pheromes);
			 //Point p = findLargestGap(player_cell, nearby_cells, nearby_pheromes);
			 //return (int) extractAngleFromVector(p.x, p.y);
		} else {
			return densityRadar(player_cell, memory, nearby_cells, nearby_pheromes, seperation);
		}
	}


	/* use a number to reflect how dense a cell's neighborhood is */
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
	private boolean isCrowded(Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes, double d_filter) {
		return density(player_cell, nearby_cells, nearby_pheromes, d_filter) >= isCrowdedThreshold(this.tail, this.visible_distance);
	}

	
	/* check if a position is prohibited */
	private boolean collidesOriginal(Cell player_cell, Point vector, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
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
					&& destination.distance(other.getPosition()) < 0.5 * player_cell.getDiameter() * 1.01 + 0.0001)
				return true;
		}
		return false;
	}

	/* Transform function to transform a number to different scale */
	public double trans1(double a) {
		return 1.0/(a+0.5);
	}
	public double trans2(double a) {
		return 1/Math.pow((0.01+a),2);
	}

	/* transform between angle and unit vector */
	// speed is from 0 to 1
	private Point extractVectorFromAngle(int arg, double speed) {
		double theta = Math.toRadians((double) arg * Player.SCALE);
		double dx = Cell.move_dist * Math.cos(theta) * speed;
		double dy = Cell.move_dist * Math.sin(theta) * speed;
		return new Point(dx, dy);
	}
	private double findLeastAbs(double x1, double x2, double x3) {
		if (Math.abs(x1) < Math.abs(x2)) return Math.abs(x1) < Math.abs(x3) ? x1 : x3;
		else return Math.abs(x2) < Math.abs(x3) ? x2 : x3;
	}
	private double extractRatioFromVector(double dx, double dy) {
		// dx_ and dy_ are used to find "wraps around" distance 
		double dx_ = findLeastAbs(dx, dx+this.side, dx-this.side);
		double dy_ = findLeastAbs(dy, dy+this.side, dy-this.side);
		double theta = Math.atan(dy_/dx_);
		double ratio = (theta / (2 * Math.PI) + (dx_ > 0 ? (dy_ < 0 ? 1 : 0) : 0.5));
		return ratio;
	}
	private double extractAngleFromVector(double dx, double dy) {
		return extractRatioFromVector(dx, dy) * this.our_angle_range;
	}

}
