package slather.g2;

import slather.sim.Cell;
import slather.sim.Point;
import slather.sim.Move;
import slather.sim.Pherome;
import java.util.*;

import slather.g2.util.Vector;


public class Grider extends Chiller {

    float MIN_DISTANCE;

    public void init(double d, int t, int side_length) {
        System.out.println("Grider init");

        this.RANDOM_GENERATOR = new Random(System.currentTimeMillis());
        this.BOARD_SIZE = side_length;

        this.MIN_DISTANCE = t;

    }

    public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        List<Vector> differences = new ArrayList<Vector>();
        Vector difference = null;
        Vector self_pos = new Vector(player_cell.getPosition());
        Vector other_pos = null;


        for(Cell other : nearby_cells) {
            other_pos = new Vector(other.getPosition());
            difference = other_pos.getTaurusDistance(self_pos, BOARD_SIZE);
            if( Math.hypot( difference.x, difference.y ) <= MIN_DISTANCE ) {
                differences.add( difference );
            }
        }

        difference = new Vector(0,0);
        for(Vector p : differences) {
            difference = difference.add(p);
        }

        difference = difference.multiply(-1);
        difference = difference.unitVector();

        return new Move(difference.getPoint(), memory);
    }

    public Move reproduce(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        // System.out.println("Circler reproduce");
        return new Move(true, (byte)(memory|ROLE_CIRCLE), memory);
    }

}