package slather.g5;

import slather.sim.Cell;
import slather.sim.Point;
import slather.sim.Move;
import slather.sim.Pherome;
import slather.sim.GridObject;
import java.util.*;

//Overall strategy: Start at state 1, which is finding the largest angle
// If there are 3 or more friendly cells nearby, go to state 2
//at state 2, move away from friendly cells until there are expansion_ratio more enemy cells than friendly
//switch to late game at 8 cells near you. At this point the mid game function is called. If it fails, (which it 
//probably will) just move away from the nearest cell.
public class Player implements slather.sim.Player {
    
    private static final double THRESHOLD_DISTANCE = 2;
    
    private static final int MIDGAME_CELL_THRESHOLD = 3;//friendly nearby cells to go from early to mid game
    
    private static final double EXPANSION_RATIO = 2;//How many more enemy cells than friend cells there should be
    												//before you stop pushing against the boundary
	private static final int LATEGAME_CELL_THRESHOLD = 8;//isnt really used, since mid game and late game have
														//the same strategy
	private static final double ANGLE_PRECISION = 32;//2^bits used
	private Random gen;
    public AggresivePlayer aggresivePlayer;
    int t_;
    double d_;
    /*
     * Memory has the following values:
     * angle: 5 bits, role 3 bits
     * role:
     * 000 early game (explore with angle)
     * 001 mid game (angle for border, move away from own cells for inner)
     * 010 late game
     * 		 late game inner (small cells move close together, give large cells space)
     * 		 late game border (move away from our own cells?)
     * 011 attacker (move toward enemies)
     */
    
    
    public void init(double d, int t, int side_length) {
        aggresivePlayer = new AggresivePlayer();
        gen = new Random();
        t_ = t;
        d_ = d;
    }

    private Point pathOfLeastResistance(Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
    	
    	class GridObjectAnglePair <GridObjectAnglePair>{
            GridObject gridObject;
            Point angle;
            public GridObjectAnglePair(GridObject obj, Point ang) {
                angle = ang;
                gridObject = obj;
            }
        }
        
        class GridObjectAnglePairComparator implements Comparator<GridObjectAnglePair> {
            @Override
            public int compare(GridObjectAnglePair a, GridObjectAnglePair b) {
                double angle1 = Math.atan2(a.angle.y, a.angle.x);
                double angle2 = Math.atan2(b.angle.y, b.angle.x);
                if(angle1==angle2) return 0;
                return angle1<angle2?-1:1;
            }   
        }
        
        List<GridObjectAnglePair> nearby_list = new ArrayList<GridObjectAnglePair>(); 
        for(GridObject cell : nearby_cells) {
            nearby_list.add
            	(new GridObjectAnglePair
            			(cell,
            			getClosestDirection(player_cell.getPosition(), cell.getPosition())));
        }
        for(GridObject pherome : nearby_pheromes) {
            if(pherome.player != player_cell.player) {
                nearby_list.add
                (new GridObjectAnglePair
                		(pherome,
                		getClosestDirection(player_cell.getPosition(), pherome.getPosition())));
            }
        }
        nearby_list.sort(new GridObjectAnglePairComparator());
        if(nearby_list.size()>1) {
            double widest = -1;
            int widest_index = -1;
            for(int i = 0; i < nearby_list.size(); ++i) {
            	Point p0 = nearby_list.get(i-1<0?nearby_list.size()-1:i-1).gridObject.getPosition();
            	Point p1 = nearby_list.get(i).gridObject.getPosition();
                double angle = 
                		angleBetweenVectors(getClosestDirection(player_cell.getPosition(), p0),
                				getClosestDirection(player_cell.getPosition(), p1));
                //System.out.println("angle: " + angle);
                if( widest < angle ) {
                    widest = angle;
                    widest_index = i;
                }
            }
            //System.out.println();
            Point p1 = nearby_list.get(widest_index).angle;
            Point p2 = nearby_list.get(widest_index-1<0?nearby_list.size()-1:widest_index-1).angle;
            
            p2 = rotate_counter_clockwise(p2, widest/2);
            
            
            //return new Move(p3, memory);
            return p2;
        } else if(nearby_list.size() == 1) {
            return new Point(-nearby_list.get(0).angle.x,-nearby_list.get(0).angle.y);
        }
        return new Point(0,0);
    }
    
    /*
     * from p1 to p2 counter clockwise, 0 to 2PI
     */
    private double angleBetweenVectors(Point p1, Point p2) {
    	double x1 = p1.x, x2 = p2.x, y1 = p1.y, y2 = p2.y;
    	double dot = x1*x2 + y1*y2;    
    	double det = x1*y2 - y1*x2;  
    	double angle = Math.atan2(det, dot);
    	if(angle < 0) angle += 2*Math.PI;
    	return angle;
    }
    private Point getLargestTraversableDistance(
    		Point direction,
    		Cell player_cell,
    		Set<Cell> nearby_cells,
    		Set<Pherome> nearby_pheromes) {
    	
    	direction = getUnitVector(direction);
    	
    	double small = 0;
    	double large = 1;
    	while(large - small > 0.001) {
    		double mid = (small + large) / 2;
    		Point vector = new Point(direction.x * mid, direction.y * mid);
    		boolean can_move = !collides(player_cell, vector, nearby_cells, nearby_pheromes);
    		if(can_move) small = mid;
    		else large = mid;
    	}
    	Point nextPoint = new Point(direction.x * small, direction.y * small);
    	
    	return new Point(direction.x * small, direction.y * small);
    }
    private Point pathBetweenTangents(
    		Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
    	class GridObjectAnglePair{
            GridObject gridObject;
            Point angle;
            public GridObjectAnglePair(GridObject obj, Point ang) {
                angle = ang;
                gridObject = obj;
            }
        }
        
    	
        class GridObjectAnglePairComparator implements Comparator<GridObjectAnglePair> {
            @Override
            public int compare(GridObjectAnglePair a, GridObjectAnglePair b) {
                double angle1 = Math.atan2(a.angle.y, a.angle.x);
                double angle2 = Math.atan2(b.angle.y, b.angle.x);
                if(angle1==angle2) return 0;
                return angle1<angle2?-1:1;
            }   
        }
        
        List<GridObjectAnglePair> nearby_list = new ArrayList<GridObjectAnglePair>(); 
        for(GridObject cell : nearby_cells) {
            nearby_list.add
            	(new GridObjectAnglePair
            			(cell,
            			getClosestDirection(player_cell.getPosition(), cell.getPosition())));
        }
        for(GridObject pherome : nearby_pheromes) {
            if(pherome.player != player_cell.player) {
                nearby_list.add
                (new GridObjectAnglePair
                		(pherome, 
                		getClosestDirection(player_cell.getPosition(), pherome.getPosition())));
            }
        }
        nearby_list.sort(new GridObjectAnglePairComparator());
        if(nearby_list.size()>1) {
        	
        	//start with biggest cell, or 0 if no cells
        	int biggest_i = 0;
        	double big_diam = 0;
        	int ind = 0;
        	for(GridObjectAnglePair g : nearby_list) {
        		if(g.gridObject instanceof Cell) {
        			if(((Cell)g.gridObject).getDiameter() > big_diam) {
        				big_diam = ((Cell)g.gridObject).getDiameter();
        				biggest_i = ind;
        			}
        		}
        		++ind;
        	}
        	
        	
            double widest = -1;
            Point widest_vector = null;
            Point prev_tangent = 
            		(Point) getTangentDirections(player_cell, nearby_list.get(biggest_i).gridObject)
            		.get(1);
            int prev_i = biggest_i;
            int sz = nearby_list.size();
            for(int i = biggest_i + 1; i < nearby_list.size() + biggest_i + 1; ++i) {
            	int k = i%sz;
            	Point p0 = nearby_list.get(prev_i).gridObject.getPosition();
            	Point p1 = nearby_list.get(k).gridObject.getPosition();
            	
            	Point current_tangent1 = 
            			(Point) getTangentDirections(player_cell, nearby_list.get(k).gridObject)
            			.get(0);
            	Point current_tangent2 = 
            			(Point) getTangentDirections(player_cell, nearby_list.get(k).gridObject)
            			.get(1);
            	double angle = angleBetweenVectors(prev_tangent, current_tangent1);
                
                double center_angle = 
                		angleBetweenVectors(getClosestDirection(player_cell.getPosition(), p0),
                				getClosestDirection(player_cell.getPosition(), p1));
                //if overlap occurs
                if(center_angle < Math.PI && angle > Math.PI) {
                	double angle_temp = angleBetweenVectors(current_tangent2, prev_tangent);
                	if(angle_temp < Math.PI) {
                		prev_i = k;
                		prev_tangent = current_tangent2;
                	}
                //no overlap
                } else {
                	if(widest < angle) {
                		widest = angle;
                		widest_vector = prev_tangent;
                	}
                	prev_tangent = current_tangent2;
                	prev_i = k;
                }
            }
            if(widest_vector==null) {
            	//This will also likely return empty...
            	return pathOfLeastResistance(player_cell, nearby_cells, nearby_pheromes);
            }
            Point p2 = rotate_counter_clockwise(widest_vector, widest/2);
            
            
            //return new Move(p3, memory);
            return p2;
        } else if(nearby_list.size() == 1) {
            return new Point(-nearby_list.get(0).angle.x,-nearby_list.get(0).angle.y);
        }
        return new Point(0,0);
    }
    
    
    /*
     * Angle in Radian
     */
    Point rotate_counter_clockwise(Point vector, double angle) {
		double newx, newy,x,y;
		x = vector.x;
		y = vector.y;
		newx = x*Math.cos(angle) - y*Math.sin(angle);
		newy = y*Math.cos(angle) + x*Math.sin(angle);
		return new Point(newx, newy);
	}
    
    private Set<GridObject> getRestrictedGridObjects(Cell player_cell, 
    						Set<GridObject> nearby_objects, double d_restrict) {
    	Set<GridObject> nearby_objects_restricted = new HashSet<GridObject>();
    	for (GridObject near_cell :nearby_objects ) {
            if (player_cell.distance(near_cell) <= d_restrict ) {
            	nearby_objects_restricted.add(near_cell);
                
            }
        }
    	return nearby_objects_restricted;
    }
    
    /*
     * Decides roles.
     */
    byte updateMemory(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Cell> nearby_cells_restricted,
			Set<Pherome> nearby_pheromes, Set<Pherome> nearby_pheromes_restricted) {
    	int friends = 0;
    	int enemies = 0;
    	for(Cell cell : nearby_cells_restricted) if(cell.player==player_cell.player) {
    		friends ++;
    	} else enemies++;
    	if(isEarlyGame(memory) && friends >= MIDGAME_CELL_THRESHOLD) {
    		
    		memory = setMidGame(memory);
    	} else if(isMidGame(memory) && nearby_cells_restricted.size() >= LATEGAME_CELL_THRESHOLD ) {
    		memory = setLateGame(memory);
    	}
    	
    	return memory;
    }
    
    byte setEarlyGame(byte memory) {
    	byte new_memory = memory;
    	new_memory >>= 3;
    	new_memory <<= 3;
    	return new_memory;
    }
    boolean isEarlyGame(byte memory) {
    	return (memory&(7))==0;
    }
    byte setMidGame(byte memory) {
    	byte new_memory = memory;
    	new_memory >>= 3;
    	new_memory <<= 3;
    	new_memory |= 1;
    	return new_memory;
    }
    boolean isMidGame(byte memory) {
    	return (memory&(7))==1;
    }
    byte setLateGame(byte memory) {
    	byte new_memory = memory;
    	new_memory >>= 3;
    	new_memory <<= 3;
    	new_memory |= 2;
    	return new_memory;
    }
    boolean isLateGame(byte memory) {
    	return (memory&(7))==2;
    }
    byte setAttacker(byte memory) {
    	byte new_memory = memory;
    	new_memory >>= 3;
    	new_memory <<= 3;
    	new_memory |= 3;
    	return new_memory;
    }
    boolean isAttacker(byte memory) {
    	return (memory&(7))==3;
    }
    
    
    public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        //restrict to d_restrict mm sight
    	//System.out.println(nearby_cells.size());
    	
        double d_restrict = THRESHOLD_DISTANCE;
        d_restrict = Math.min(d_restrict,d_);
        @SuppressWarnings("unchecked")
		Set<Cell> nearby_cells_restricted = 
        		(Set<Cell>)(Set<?>)
        		getRestrictedGridObjects(player_cell, new HashSet<GridObject>(nearby_cells), d_restrict);
        @SuppressWarnings("unchecked")
		Set<Pherome> nearby_pheromes_restricted = 
        		(Set<Pherome>)(Set<?>)
        		getRestrictedGridObjects(player_cell, new HashSet<GridObject>(nearby_pheromes), d_restrict);
        
        
        

        //System.out.println(nearby_cells_restricted.size());
        memory = 
        		updateMemory(player_cell,
        				memory,
        				nearby_cells,
        				nearby_cells_restricted,
        				nearby_pheromes,
        				nearby_pheromes_restricted);
        // reproduce whenever possible
        if (player_cell.getDiameter() >= 2) {
            return new Move(true, (byte)memory, (byte)memory);
        }

        if(isEarlyGame(memory)) {
        //	System.out.println("early move");
        	return moveEarlyGame(player_cell, memory, nearby_cells,
        			nearby_cells_restricted, nearby_pheromes, nearby_pheromes_restricted);
        	
        } else if(isMidGame(memory)) {
        //	System.out.println("mid move");
        	return moveMidGame(player_cell, memory, nearby_cells,
        			nearby_cells_restricted, nearby_pheromes, nearby_pheromes_restricted);
        } else if(isLateGame(memory)){
        	return moveLateGame(player_cell, memory, nearby_cells,
        			nearby_cells_restricted, nearby_pheromes, nearby_pheromes_restricted);
        } else if(isAttacker(memory)) {
        	return moveAttacker(player_cell, memory, nearby_cells,
        			nearby_cells_restricted, nearby_pheromes, nearby_pheromes_restricted	);
        }
        
	        
        System.out.println("faiiil");
        // if all tries fail, just chill in place
        return new Move(new Point(0,0), (byte)0);
    }
    
    
    private Move moveAttacker(Cell player_cell,
    		byte memory,
    		Set<Cell> nearby_cells,
    		Set<Cell> nearby_cells_restricted,
			Set<Pherome> nearby_pheromes,
			Set<Pherome> nearby_pheromes_restricted) {
		// TODO Auto-generated method stub
		return moveMidGame(player_cell,
				memory,
				nearby_cells,
				nearby_cells_restricted,
				nearby_pheromes,
				nearby_pheromes_restricted);
	}

	private Move moveLateGame(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Cell> nearby_cells_restricted,
			Set<Pherome> nearby_pheromes, Set<Pherome> nearby_pheromes_restricted) {
		// TODO Auto-generated method stub
		return moveMidGame(player_cell,
				memory,
				nearby_cells,
				nearby_cells_restricted,
				nearby_pheromes,
				nearby_pheromes_restricted);
	}

	private Move moveMidGame(Cell player_cell,
    		byte memory,
    		Set<Cell> nearby_cells, Set<Cell> nearby_cells_restricted,
			Set<Pherome> nearby_pheromes,
			Set<Pherome> nearby_pheromes_restricted) {
		Set<Cell> friendlies = new HashSet<Cell>();
		Set<Cell> enemies = new HashSet<Cell>();
		
		for(Cell cell : nearby_cells_restricted) {
			if(cell.player == player_cell.player) {
				friendlies.add(cell);
			}
			if(cell.player != player_cell.player) {
				enemies.add(cell);
			}
		}
		int num_friendlies = friendlies.size();
		int num_enemies = enemies.size();
		Point direction;
		if(num_enemies < EXPANSION_RATIO * num_friendlies) {
			direction = pathBetweenTangents(player_cell, friendlies, new HashSet<Pherome>());
		} else {
			direction = pathBetweenTangents(player_cell, nearby_cells_restricted, nearby_pheromes_restricted);
		}
		if(collides(player_cell, direction, nearby_cells_restricted, nearby_pheromes_restricted)) {
			direction = getLargestTraversableDistance(
					direction, player_cell, nearby_cells_restricted, nearby_pheromes_restricted);
		}
		if (!collides(player_cell, direction, nearby_cells_restricted, nearby_pheromes_restricted)) {
			return new Move(direction, memory);
		} else {
			Cell closest = getClosest(player_cell, nearby_cells_restricted);
	    	direction = getClosestDirection(closest.getPosition(), player_cell.getPosition());
	    	getLargestTraversableDistance(direction,
	    			player_cell,
	    			nearby_cells_restricted,
	    			nearby_pheromes_restricted);
	    	return new Move(direction, memory);
		}
		
	}

	private Cell getClosest(Cell player_cell, Set<Cell> nearby_cells) {
		double distance = 100;
		Cell special = null;
		for(Cell cell : nearby_cells) {
			if(player_cell.distance(cell) < distance) {
				distance = player_cell.distance(cell);
				special = cell;
			}
		}
		return special;
	}
	
	private Point vectorEnemyAddition(Cell player_cell,
			Set<Cell> nearby_cells_restricted, 
			Set<Pherome> nearby_pheromes_restricted) {
		double xsum=0, ysum=0;
		for(Cell cell : nearby_cells_restricted) {
			if(cell.player != player_cell.player) {
				Point dir = getClosestDirection(player_cell.getPosition(), cell.getPosition());
				xsum += dir.x;
				ysum += dir.y;
			}
		}
		return getUnitVector(new Point(xsum,ysum));
	}
    /*
     * angle in degrees
     */
    private byte loadAngleToMemoryDegrees(byte memory, double angle) {
    	memory = (byte) (memory&7);
    	byte new_memory = (byte)((int)(angle * ANGLE_PRECISION/360.0));
    	new_memory <<= 3;
    	new_memory |= (memory&7);
    	return new_memory;
    }
    private byte loadAngleToMemory(byte memory, Point direction) {
    	double angle = Math.toDegrees(Math.atan2(direction.y, direction.x));
    	return loadAngleToMemoryDegrees(memory, angle);
    }
	private Move moveEarlyGame(Cell player_cell,
			byte memory, 
			Set<Cell> nearby_cells, 
			Set<Cell> nearby_cells_restricted,
			Set<Pherome> nearby_pheromes, 
			Set<Pherome> nearby_pheromes_restricted) {
    	Point nextPath = pathBetweenTangents(player_cell, nearby_cells_restricted, nearby_pheromes_restricted);
    	
        if(nextPath.x != 0 && nextPath.y != 0) {
            if(!collides(player_cell, nextPath, nearby_cells_restricted, nearby_pheromes_restricted)) {
                return new Move(nextPath, loadAngleToMemory(memory, nextPath));
            } else {
            	Point vector = getLargestTraversableDistance(
    					nextPath, player_cell, nearby_cells_restricted, nearby_pheromes_restricted);
            	if(vector.norm() > 0.05
            			&& !collides(player_cell, vector, nearby_cells_restricted, nearby_pheromes_restricted)) {
            		return new Move(
            			vector, loadAngleToMemory(memory, vector));
            	}
            }
        } else {
            // continue moving in the same direction as before
            Point vector = extractVectorFromAngle( (int)(memory>>3));
            // check for collisions
            if (!collides( player_cell, vector, nearby_cells_restricted, nearby_pheromes_restricted))
            return new Move(vector, memory);
        }
        

        // Generate a random new direction to travel
        if(!collides(player_cell, new Point(0,0), nearby_cells_restricted, nearby_pheromes_restricted)) {
            return new Move(new Point(0,0), memory);
            
        } else {
        	Cell closest = getClosest(player_cell, nearby_cells_restricted);
        	Point direction = getClosestDirection(closest.getPosition(),player_cell.getPosition());
        	getLargestTraversableDistance(direction,
        			player_cell,
        			nearby_cells_restricted,
        			nearby_pheromes_restricted);
        	return new Move(direction, memory);
        }
	}

	/*
     * Returns in direction player_cell --> other
     * Second one is rotated more counter clockwise than first
     */
    private List<Point> getTangentDirections(Cell player_cell, GridObject other) {
    	List<Point> out = new ArrayList<Point>();
    	double r1 = player_cell.getDiameter()/2;
    	double r2 = 0;
    	if(other instanceof Cell) {
    		r2 = ((Cell)other).getDiameter()/2;
    	}
    	
    	double d = getDistance(player_cell.getPosition(), other.getPosition());
    	double theta2 = Math.asin((r1+r2)/d);
    	Point center_to_center = new Point(other.getPosition().x-player_cell.getPosition().x,
    			other.getPosition().y-player_cell.getPosition().y);
    	center_to_center = getUnitVector(center_to_center);
    	out.add(rotate_counter_clockwise(center_to_center, -theta2));
    	out.add(rotate_counter_clockwise(center_to_center, theta2));
    	
    	
    	return out;
    }
    
    
    // check if moving player_cell by vector collides with any nearby cell or hostile pherome
    private boolean collides(Cell player_cell, Point vector, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
    Iterator<Cell> cell_it = nearby_cells.iterator();
    Point destination = player_cell.getPosition().move(vector);
    while (cell_it.hasNext()) {
        Cell other = cell_it.next();
        if ( destination.distance(other.getPosition()) 
        		< 0.021*player_cell.getDiameter() 
        		+ 0.5*player_cell.getDiameter() 
        		+ 0.5*other.getDiameter() 
        		+ 0.00011) 
        return true;
    }
    Iterator<Pherome> pherome_it = nearby_pheromes.iterator();
    while (pherome_it.hasNext()) {
        Pherome other = pherome_it.next();
        if (other.player != player_cell.player 
        		&& destination.distance(other.getPosition()) 
        		< 0.011*player_cell.getDiameter() + 0.5*player_cell.getDiameter() + 0.0001) 
        return true;
    }
    return false;
    }

    // convert an angle (in 2-deg increments) to a vector with magnitude
    // Cell.move_dist (max allowed movement distance)
    private Point extractVectorFromAngle(int arg) {
    double theta =  (double)arg * (2*Math.PI)/ANGLE_PRECISION;
    double dx = Cell.move_dist * Math.cos(theta);
    double dy = Cell.move_dist * Math.sin(theta);
    return new Point(dx, dy);
    }
    private double getDistanceDirect(Point first, Point second) {
		double dist_square = (first.x - second.x)*(first.x - second.x) + (first.y - second.y)*(first.y - second.y);
    	double dist = Math.sqrt(dist_square);
    	return dist;
	}

    private double getDistance(Point first, Point second) {
    	double x = second.x;
    	double y = second.y;
    	double dist = 100;
    	for(int area_x = -1; area_x <= 1; area_x ++) {
    		for(int area_y = -1; area_y <= 1; area_y ++) {
    			x = second.x + area_x*100;
    			y = second.y + area_y*100;
    			double d = getDistanceDirect(first, new Point(x,y));
    			if( dist > d) {
    				dist = d;
    			}
    		}
    	}
    	//System.out.println("distance to " + second.x + " " + second.y +"= " + dist);
    	return dist;
    }
    //of first to second
    private Point getClosestDirection(Point first, Point second) {
    	//System.out.println(first.x +" " + first.y +" " + second.x +" "+ second.y);
    	double x = second.x;
    	double y = second.y;
    	double dist = 100;
    	Point best = null;
    	for(int area_x = -1; area_x <= 1; area_x ++) {
    		for(int area_y = -1; area_y <= 1; area_y ++) {
    			x = second.x + area_x*100;
    			y = second.y + area_y*100;
    			double d = getDistanceDirect(first, new Point(x ,y ));
    			if( dist > d) {
    				dist = d;
    				best = new Point(x-first.x,y-first.y);
    			}
    		}
    	}
		//if(best == null) System.out.println("best null");
    	return getUnitVector(best);
    }
    
    private Point getUnitVector(Point point) {
    	double x = point.x, y = point.y;
    	double norm = Math.hypot(x, y);
    	x /= norm;
    	y /= norm;
		
    	return new Point(x, y);
    }
    
}
