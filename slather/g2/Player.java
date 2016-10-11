package slather.g2;

import slather.sim.Cell;
import slather.sim.Point;
import slather.sim.Move;
import slather.sim.Pherome;
import java.util.*;

import slather.g2.util.Vector;

public class Player implements slather.sim.Player {

    protected Random RANDOM_GENERATOR;
    protected double VISION;
    protected int TAIL_LENGTH;
    protected double BOARD_SIZE;

    protected double PROB_CIRCLE = 0.2;

    private Chiller chiller = null;
    private Circler occupier = null;
    private Scout scout = null;

    // Initialize here, initialize sub strategies here.
    // Take care to initialize VISION, TAIL_LENGTH, BOARD_SIZE, RANDOM_GENERATOR in your subclass
    // Do not make recursive calls to this init.

    public void init(double d, int t, int side_length) {
        System.out.println("Player init");

        this.RANDOM_GENERATOR = new Random(System.currentTimeMillis());
        this.VISION = d;
        this.TAIL_LENGTH = t;
        this.BOARD_SIZE = side_length;

        if (t <= 4) PROB_CIRCLE = 0;

        chiller = new Chiller();
        chiller.init(d, t, side_length);

        occupier = new Circler();
        occupier.init(d, t, side_length);

        scout = new Scout();
        scout.init(d, t, side_length);
    }

    public String move2String(Move m) {
        return
        "Move(" +m.reproduce
            +","+m.vector.x
            +","+m.vector.y
            +","+String.format("%8s", Integer.toBinaryString(m.memory & 0xFF)).replace(' ', '0')
            +","+String.format("%8s", Integer.toBinaryString(m.daughter_memory & 0xFF)).replace(' ', '0')
            +")";
    }

    public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        // reproduce whenever possible
        Move m;
        if (player_cell.getDiameter() >= 2){
            final double randomNumber = RANDOM_GENERATOR.nextDouble();
            if(!byteIsCircle(memory) &&  (randomNumber< PROB_CIRCLE)) {
                m = occupier.reproduce(player_cell, memory, nearby_cells, nearby_pheromes);
            } else {
                m = scout.reproduce(player_cell, memory, nearby_cells, nearby_pheromes);
            }
        } else {
            boolean nextIsCircle = byteIsCircle(memory);
            // nextIsCircle = false;
            // if(scout.crowded(player_cell,nearby_cells)){
            //     nextIsCircle = true;
            // }
            if(nextIsCircle){
                // System.out.println("nextIsCircle");
            }
            if(!nextIsCircle) {
                m = scout.play(player_cell, memory, nearby_cells, nearby_pheromes);
            } else {
                m = occupier.play(player_cell, memory, nearby_cells, nearby_pheromes);
            }
        }

        // System.out.println(move2String(m));

        return m;
    }

    // check if moving player_cell by vector collides with any nearby cell or hostile pherome
    protected boolean collides(Cell player_cell, Vector vector, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        Iterator<Cell> cell_it = nearby_cells.iterator();
        Vector destination = new Vector( player_cell.getPosition().move(vector.getPoint()) );
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

    protected static final int ANGLE_BITS = 5;
    protected static final int ANGLE_MIN = 0;
    protected static final int ANGLE_MAX = 1 << ANGLE_BITS;
    protected static final int ANGLE_MASK = ANGLE_MAX - 1;

    protected static final int ROLE_BITS = 1;
    protected static final int ROLE_MIN = ANGLE_MAX;
    protected static final int ROLE_MAX = ROLE_MIN + ( 1<<ROLE_BITS );
    protected static final int ROLE_MASK = (ROLE_MAX - 1) & ~ANGLE_MASK;
    protected static final int ROLE_SCOUT = 0 << ANGLE_BITS;
    protected static final int ROLE_CIRCLE = 1 << ANGLE_BITS;

    private boolean byteIsCircle(byte b){
        //System.out.printf("%d %d %d\n",b,ROLE_MASK,ROLE_CIRCLE);
        return (b&ROLE_MASK)==ROLE_CIRCLE;
    }

}
