package slather.g5;

import slather.sim.Cell;
import slather.sim.Point;
import slather.sim.Move;
import slather.sim.Pherome;
import java.util.*;


public class AggresivePlayer implements slather.sim.Player {
    
    private Random gen;

    int t_;
    double d_;

    public void init(double d, int t,int side_length) {
        gen = new Random();
        t_ = t;
        d_ = d;
    }

    public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        // reproduce whenever possible
        if (player_cell.getDiameter() >= 2) {
            return new Move(true, (byte)0, (byte)0);
        }

        // Offensive strategy
        if(memory > 0) {
            int cellX = 0;
            int cellY = 0;

            // Look at nearby cells and go toward opposing players 
            // and away from friendly cells
            for (Cell c : nearby_cells) {
                if(c.player != player_cell.player) {
                    // TODO: If we're being encroached, perhaps the strategy should
                    //       be shifted so we move away from all cells until we're less encroached.
                    cellX += c.getPosition().x - player_cell.getPosition().x;
                    cellY += c.getPosition().y - player_cell.getPosition().y;
                    // TODO: Weight the contribution by distance to cell
                    //       This could help, probably for large d? maybe not?
                } else {
                    cellX -= c.getPosition().x - player_cell.getPosition().x;
                    cellY -= c.getPosition().y - player_cell.getPosition().y;
                }
            }
            for (Pherome p :nearby_pheromes ) {
                if (p.player != player_cell.player) {
                    cellX += p.getPosition().x - player_cell.getPosition().x;
                    cellY += p.getPosition().y - player_cell.getPosition().y;
                }
            }

            if(Math.hypot(cellX, cellY) == 0) { // If there are no nearby cells or the desired destination is to stay put
                // continue moving in the same direction as before
                Point vector = extractVectorFromAngle( (int)memory);
                // check for collisions
                if (!collides( player_cell, vector, nearby_cells, nearby_pheromes))
                return new Move(vector, memory);
            } else {
                // otherwise move toward enemies and away from friendlies
                return new Move(new Point(cellX / Math.hypot(cellX, cellY), 
                                          cellY / Math.hypot(cellX, cellY)),
                                          (byte)((Math.atan2(cellY, cellX))/2));
            }
        }

        // TODO: this is probably stale code
        if (memory > 0) { // follow previous direction unless it would cause a collision
            Point vector = extractVectorFromAngle( (int)memory);
            // check for collisions
            if (!collides( player_cell, vector, nearby_cells, nearby_pheromes))
            return new Move(vector, memory);
        }

        return null;
        // // Generate a random new direction to travel
        // for (int i=0; i<4; i++) {
        //     int arg = gen.nextInt(180)+1;
        //     Point vector = extractVectorFromAngle(arg);
        //     if (!collides(player_cell, vector, nearby_cells, nearby_pheromes)) 
        //     return new Move(vector, (byte) arg);
        // }

        // // if all tries fail, just chill in place
        // return new Move(new Point(0,0), (byte)0);
    }

    // Broken nextDirection function for circle strategy
    private Point nextDirection(Cell player_cell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {

        Pherome closest_to_one_mm = null;
        double min_distance = Double.MAX_VALUE;

        if (nearby_pheromes.isEmpty()) {
            for (int i=0; i<4; i++) {
                int arg = gen.nextInt(180)+1;
                Point vector = extractVectorFromAngle(arg);
                if (!collides(player_cell, vector, nearby_cells, nearby_pheromes)) {
                    return vector;
                }
            }
        } else {
            for (Pherome p : nearby_pheromes) {

                double distance = Math.abs(1 - player_cell.distance(p));
                if(distance < min_distance) {
                    min_distance = distance;
                    closest_to_one_mm = p;
                }
            }

            Point cur_direction = new Point(player_cell.getPosition().x - closest_to_one_mm.getPosition().x, 
                                            player_cell.getPosition().y - closest_to_one_mm.getPosition().y);

            double angle = Math.atan2(cur_direction.y, cur_direction.x);

            double next_angle = angle + ((2*Math.PI)/ t_);
            double dx = Cell.move_dist * Math.cos(next_angle);
            double dy = Cell.move_dist * Math.sin(next_angle);
            return new Point(dx, dy);
        }

        return new Point(0,0);
    }

    // check if moving player_cell by vector collides with any nearby cell or hostile pherome
    private boolean collides(Cell player_cell, Point vector, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
    Iterator<Cell> cell_it = nearby_cells.iterator();
    Point destination = player_cell.getPosition().move(vector);
    while (cell_it.hasNext()) {
        Cell other = cell_it.next();
        if ( destination.distance(other.getPosition()) < 0.5*player_cell.getDiameter() + 0.5*other.getDiameter() + 0.00011) 
        return true;
    }
    Iterator<Pherome> pherome_it = nearby_pheromes.iterator();
    while (pherome_it.hasNext()) {
        Pherome other = pherome_it.next();
        if (other.player != player_cell.player && destination.distance(other.getPosition()) < 0.5*player_cell.getDiameter() + 0.0001) 
        return true;
    }
    return false;
    }

    // convert an angle (in 2-deg increments) to a vector with magnitude Cell.move_dist (max allowed movement distance)
    private Point extractVectorFromAngle(int arg) {
    double theta = Math.toRadians( 2* (double)arg );
    double dx = Cell.move_dist * Math.cos(theta);
    double dy = Cell.move_dist * Math.sin(theta);
    return new Point(dx, dy);
    }

}
