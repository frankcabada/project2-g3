package slather.g2;

import slather.sim.Cell;


///////////////// DO NOT import tjis file please.
///////////////// DO NOT import tjis file please.
///////////////// DO NOT import tjis file please.
///////////////// DO NOT import tjis file please.
///////////////// DO NOT import tjis file please.
import slather.sim.Point;
///////////////// DO NOT import tjis file please.
///////////////// DO NOT import tjis file please.
///////////////// DO NOT import tjis file please.
///////////////// DO NOT import tjis file please.
///////////////// DO NOT import tjis file please.


import slather.sim.Move;
import slather.sim.Pherome;
import slather.g2.util.Vector;
import java.util.*;
import java.lang.*;

// Either there's a giant bug.
// Or I'm complete rekt by the random bot


public class Scout extends Player {

    @Override
    public void init(double d, int t, int size) {
        this.RANDOM_GENERATOR = new Random();
        this.BOARD_SIZE = size;
        this.VISION = d;
    }

    private static final double TWOPI = 2*Math.PI;

    private double normalizeAngle(double a, double start) {
        if( a < start ) {
            return normalizeAngle( a+TWOPI, start);
        } else if (a >= (start+TWOPI)) {
            return normalizeAngle( a-TWOPI, start);
        } else {
            return a;
        }
    }

    private double byte2angle(byte b) {
        // -128 <= b < 128
        // -1 <= b/128 < 1
        // -pi <= a < pi
        return normalizeAngle(TWOPI * (((double) ((b) & this.ANGLE_MASK) ) / this.ANGLE_MAX), 0);
    }

    private byte angle2byte(double a, byte b) {
        final double actualAngle = ((normalizeAngle(a,0) / TWOPI)*this.ANGLE_MAX);
        final int anglePart = (int) (((int)actualAngle) & this.ANGLE_MASK);
        final byte memoryPart = (byte) ( b & ~this.ANGLE_MASK);
        // System.out.println("angle2byte "+ memoryPart +","+ anglePart +","+ (normalizeAngle(a,0)/TWOPI) +","+ this.ANGLE_MAX +","+ a);
        return (byte) ((anglePart | memoryPart));
    }

	private int densityDetection(int player, Set <Cell> nearby_cells, Set <Pherome> nearby_pheromes) {
		int own = 0, enemy = 0;
		for (Cell cell : nearby_cells) {
			if (player == cell.player) ++ own;
			else ++ enemy;
		}
		for (Pherome pherome : nearby_pheromes) {
			if (player == pherome.player) ++ own;
			else ++ enemy;
		}

		return own - enemy;
	}


    @Override
    public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        if (player_cell.getDiameter() >= 2) // reproduce whenever possible
            return new Move(true, (byte)-1, (byte)-1);

        double threshold = player_cell.getDiameter() * 0.5 + 4;
		double pthreshold = threshold;
		double TRANSITION = Math.PI * 0.6;
		//double pthreshold = player_cell.getDiameter() * 0.5 + 2;

		Set<Pherome> pheromes = new HashSet<Pherome>();
		Set<Cell> cells = new HashSet<Cell>();

		for (Cell cell : nearby_cells)
			if (cell.player == player_cell.player)
				cells.add(cell);
		for (Pherome pherome : nearby_pheromes)
			if (pherome.player == player_cell.player)
				pheromes.add(pherome);
		double surrounded = angleCovered(player_cell, cells, pheromes);

		double vision = threshold;
		double pvision = pthreshold;

		if (surrounded > TRANSITION) {
			vision = threshold;
			pvision = pthreshold;
			while (vision - player_cell.getDiameter() * 0.5 >= 1) {
				// consider only enemy pheromes and own cells
				cells.clear();
				for (Cell cell : nearby_cells)
					if (cell.player == player_cell.player || cell.getPosition().distance(player_cell.getPosition()) <= vision)
						cells.add(cell);

				pheromes.clear();
				/*for (Pherome pherome : nearby_pheromes)
					if (pherome.player != player_cell.player && pherome.getPosition().distance(player_cell.getPosition()) <= pvision)
						pheromes.add(pherome);*/

				//angle = findTheLargestAngle(player_cell, cells, pheromes);
				if (cells.size() == 0 && pheromes.size() == 0) break;

				List<Option> options = findFreeAngles(player_cell, cells, pheromes);
				if (options.size() > 0) {
					for (Option opt : options) {
						for (double len = 1; len > 0.2; len -= 0.1) {
							Point next = new Point(Math.cos(opt.angle) * len, Math.sin(opt.angle) * len);
							if (!collides(player_cell, new Vector(next), nearby_cells, nearby_pheromes)) {
								memory = angle2byte(opt.angle, memory);
								return new Move(next, memory);
							}
						}
					}
				}
				//next = new Point(Math.cos(angle), Math.sin(angle));
				vision *= 0.9;
				pvision *= 0.9;
			}
		} else

		if (true || surrounded <= TRANSITION) {
			vision = threshold;
			pvision = pthreshold;
			//while (angle > Math.PI * 2 - 1 || collides(player_cell, new Vector(next), nearby_cells, nearby_pheromes)) {
			while (vision - player_cell.getDiameter() * 0.5 >= 1) {
				cells.clear();
				for (Cell cell : nearby_cells)
					if (cell.getPosition().distance(player_cell.getPosition()) <= vision)
						cells.add(cell);
				pheromes.clear();
				for (Pherome pherome : nearby_pheromes)
					if (pherome.player != player_cell.player && pherome.getPosition().distance(player_cell.getPosition()) <= pvision)
						pheromes.add(pherome);

				//angle = findTheLargestAngle(player_cell, cells, pheromes);
				//next = new Point(Math.cos(angle), Math.sin(angle));
				if (cells.size() == 0 && pheromes.size() == 0) break;

				List<Option> options = findFreeAngles(player_cell, cells, pheromes);
				if (options.size() > 0) {
					for (Option opt : options) {
						for (double len = 1; len > 0.1; len -= 0.1) {
							Point next = new Point(Math.cos(opt.angle) * len, Math.sin(opt.angle) * len);
							if (!collides(player_cell, new Vector(next), nearby_cells, nearby_pheromes)) {
								memory = angle2byte(opt.angle, memory);
								return new Move(next, memory);
							}
						}
					}
				}

				vision *= 0.9;
				pvision *= 0.9;
			}
		}

        double angle = byte2angle(memory);
		for (double len = 1; len > 0.1; len -= 0.1) {
			Point next = new Point(Math.cos(angle) * len, Math.sin(angle) * len);
			if (!collides(player_cell, new Vector(next), nearby_cells, nearby_pheromes))
				return new Move(next, memory);
		}

		for (int i = 0; i < 100; ++ i) {
			angle = this.RANDOM_GENERATOR.nextDouble() * Math.PI * 2 - Math.PI;
			for (double len = 1; len > 0.1; len -= 0.1) {
				Point next = new Point(Math.cos(angle) * len, Math.sin(angle) * len);
				if (!collides(player_cell, new Vector(next), nearby_cells, nearby_pheromes)) {
					memory = angle2byte(angle, memory);
					return new Move(next, memory);
				}
			}
		}
		return new Move(new Point(0, 1), (byte) memory);
    }

		private class Option implements Comparable<Option> {
			public double angle;
			public double width;

			public Option(double angle, double width) {
				this.angle = angle;
				this.width = width;
			}

			public int compareTo(Option a) {
				return -Double.compare(width, a.width);
			}
		};
        private class Event implements Comparable<Event> {
            public double value;
            public int index;

            public Event(double value, int index) {
                this.value = value;
                this.index = index;
            }

            public int compareTo(Event a) {
                return Double.compare(value, a.value);
            }
        };

	private double angleCovered(Cell cell, Set <Cell> cells, Set <Pherome> pheromes) {
		double radius = cell.getDiameter() * 0.5;

		List <Event> events = new ArrayList<Event>();
		for (Cell p : cells) {
            Point dir = correctedSubtract(p.getPosition(), cell.getPosition());
            if (dir.norm() < 1e-8) continue;
            double r = radius + p.getDiameter() * 0.5;
            double angle = Math.atan2(dir.y, dir.x);
            double delta = Math.asin(Math.min(1.0, r / dir.norm()));
            //System.out.println("(" + dir.x + "," + dir.y + ")" + delta);
            if (angle - delta < -Math.PI) {
                events.add(new Event(-Math.PI, 1));
                events.add(new Event(angle + delta, -1));
                events.add(new Event(angle - delta + 2.0 * Math.PI, 1));
                events.add(new Event(Math.PI, -1));
            } else if (angle + delta > Math.PI) {
                events.add(new Event(-Math.PI, 1));
                events.add(new Event(angle + delta - 2.0 * Math.PI, -1));
                events.add(new Event(angle - delta, 1));
                events.add(new Event(Math.PI, -1));
            } else {
                events.add(new Event(angle - delta, 1));
                events.add(new Event(angle + delta, -1));
            }
        }
        for (Pherome p : pheromes) {
            Point dir = correctedSubtract(p.getPosition(), cell.getPosition());
            if (dir.norm() < 1e-8) continue;
            double r = radius;
            double angle = Math.atan2(dir.y, dir.x);
            double delta = Math.asin(Math.min(1.0, r / dir.norm()));
            //System.out.println("(" + dir.x + "," + dir.y + ")" + r + "," + dir.norm() + "," + delta);
            if (angle - delta < -Math.PI) {
                events.add(new Event(-Math.PI, 1));
                events.add(new Event(angle + delta, -1));
                events.add(new Event(angle - delta + 2.0 * Math.PI, 1));
                events.add(new Event(Math.PI, -1));
            } else if (angle + delta > Math.PI) {
                events.add(new Event(-Math.PI, 1));
                events.add(new Event(angle + delta - 2.0 * Math.PI, -1));
                events.add(new Event(angle - delta, 1));
                events.add(new Event(Math.PI, -1));
            } else {
                events.add(new Event(angle - delta, 1));
                events.add(new Event(angle + delta, -1));
            }
        }


        Collections.sort(events);

		if (events.size() == 0) return 0;

		double result = 0;
		int stack = events.get(0).index;
		double last = events.get(0).value;
        for (int i = 1; i < events.size(); ++ i) {
			if (stack == 0) last = events.get(i).value;
            stack += events.get(i).index;
            if (stack == 0) result += events.get(i).index - last;
        }

		return result;
	}

    private List<Option> findFreeAngles(Cell cell, Set<Cell> cells, Set<Pherome> pheromes) {
        double radius = cell.getDiameter() * 0.5;

        List<Event> events = new ArrayList<Event>();

        for (Cell p : cells) {
            Point dir = correctedSubtract(p.getPosition(), cell.getPosition());
            if (dir.norm() < 1e-8) continue;
            double r = radius + p.getDiameter() * 0.5;
            double angle = Math.atan2(dir.y, dir.x);
            double delta = Math.asin(Math.min(1.0, r / dir.norm()));
            //System.out.println("(" + dir.x + "," + dir.y + ")" + delta);
            if (angle - delta < -Math.PI) {
                events.add(new Event(-Math.PI, 1));
                events.add(new Event(angle + delta, -1));
                events.add(new Event(angle - delta + 2.0 * Math.PI, 1));
                events.add(new Event(Math.PI, -1));
            } else if (angle + delta > Math.PI) {
                events.add(new Event(-Math.PI, 1));
                events.add(new Event(angle + delta - 2.0 * Math.PI, -1));
                events.add(new Event(angle - delta, 1));
                events.add(new Event(Math.PI, -1));
            } else {
                events.add(new Event(angle - delta, 1));
                events.add(new Event(angle + delta, -1));
            }
        }
        for (Pherome p : pheromes) {
            Point dir = correctedSubtract(p.getPosition(), cell.getPosition());
            if (dir.norm() < 1e-8) continue;
            double r = radius;
            double angle = Math.atan2(dir.y, dir.x);
            double delta = Math.asin(Math.min(1.0, r / dir.norm()));
            //System.out.println("(" + dir.x + "," + dir.y + ")" + r + "," + dir.norm() + "," + delta);
            if (angle - delta < -Math.PI) {
                events.add(new Event(-Math.PI, 1));
                events.add(new Event(angle + delta, -1));
                events.add(new Event(angle - delta + 2.0 * Math.PI, 1));
                events.add(new Event(Math.PI, -1));
            } else if (angle + delta > Math.PI) {
                events.add(new Event(-Math.PI, 1));
                events.add(new Event(angle + delta - 2.0 * Math.PI, -1));
                events.add(new Event(angle - delta, 1));
                events.add(new Event(Math.PI, -1));
            } else {
                events.add(new Event(angle - delta, 1));
                events.add(new Event(angle + delta, -1));
            }
        }

        Collections.sort(events);

		List<Option> result = new ArrayList<Option>();

        if (events.size() == 0) {
            //System.err.println("Dead game");
            return result;
        }

        int stack = events.get(0).index;
        double width = -1;
        for (int i = 1; i < events.size(); ++ i) {
            if (stack == 0) {
                double w = events.get(i).value - events.get(i - 1).value;
                //if (w > width) {
                //    width = w;
                double angle = events.get(i).value - w * 0.5;
				if (w > 1e-8)
					result.add(new Option(angle, w));
                //}
            }
            stack += events.get(i).index;
        }
        double w = events.get(0).value + 2 * Math.PI - events.get(events.size() - 1).value;
        if (w > 1e-8) {
            double angle = events.get(0).value - w * 0.5;
            if (angle < -Math.PI) angle += 2 * Math.PI;
			result.add(new Option(angle, w));
        }
		Collections.sort(result);

		//for (Option opt : result)
	//		System.out.print("(" + opt.angle + "," + opt.width + ")");
	//	System.out.println();
        // System.out.print(events.size());
        // for (Event event: events)
        //     System.out.print(" (" + event.value + "," + event.index + ")");
        // System.out.println();
        // System.out.println("width: " + width + ", angle: " + result);
        return result;
    }

    private Point add(Point a, Point b) {
        return new Point(a.x + b.x, a.y + b.y);
    }

    private Point subtract(Point a, Point b) {
        return new Point(a.x - b.x, a.y - b.y);
    }

    private Point correctedSubtract(Point a, Point b) {
        double x = a.x - b.x, y = a.y - b.y;
        if (Math.abs(x) > Math.abs(a.x + BOARD_SIZE - b.x)) x = a.x + BOARD_SIZE - b.x;
        if (Math.abs(x) > Math.abs(a.x - BOARD_SIZE - b.x)) x = a.x - BOARD_SIZE - b.x;
        if (Math.abs(y) > Math.abs(a.y + BOARD_SIZE - b.x)) y = a.y + BOARD_SIZE - b.y;
        if (Math.abs(y) > Math.abs(a.y - BOARD_SIZE - b.x)) y = a.y - BOARD_SIZE - b.y;

        return new Point(x, y);
    }

    private Point multiply(Point a, double d) {
        return new Point(a.x * d, a.y * d);
    }

    private Point normalize(Point a) {
        if (a.norm() < 1e-7) return a;
        else return multiply(a, 1.0/a.norm());
    }

    // convert an angle (in 2-deg increments) to a vector with magnitude Cell.move_dist (max allowed movement distance)
    private Point extractVectorFromAngle(int arg) {
        double theta = Math.toRadians( 2* (double)arg );
        double dx = Cell.move_dist * Math.cos(theta);
        double dy = Cell.move_dist * Math.sin(theta);
        return new Point(dx, dy);
    }

    public Move reproduce(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        // System.out.println("Scout reproduce");
        return new Move(true, memory, memory);
    }

}
