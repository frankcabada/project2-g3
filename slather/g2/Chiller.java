package slather.g2;

import slather.sim.Cell;
import slather.sim.Point;
import slather.sim.Move;
import slather.sim.Pherome;
import java.util.*;

import slather.g2.util.Vector;


public class Chiller extends Player {

    public void init(double d, int t, int side_length) {
        System.out.println("Chiller init");

        this.RANDOM_GENERATOR = new Random(System.currentTimeMillis());
        this.VISION = d;
        this.TAIL_LENGTH = t;
        this.BOARD_SIZE = side_length;

    }

    public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        return new Move(new Point(0,0), memory);
    }

    public Move reproduce(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        System.out.println("Chiller reproduce");
        return new Move(true, memory, memory);
    }

}
