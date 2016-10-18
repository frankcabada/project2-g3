package slather.g2;

import slather.sim.Cell;
import slather.sim.Point;
import slather.sim.Move;
import slather.sim.Pherome;
import java.util.*;

import slather.g2.util.Vector;


public class GridCircler extends Chiller {

    private Grider grider = null;
    private Circler circler = null;
    float EPSILON;


    public void init(double d, int t, int side_length) {
        System.out.println("GridCircler init");

        grider = new Grider();
        grider.init(d, t, side_length);

        circler = new Circler();
        circler.init(d, t, side_length);
        // circler.RADIUS = 2.0 * t / (2*Math.PI);

        this.EPSILON = (float)0.001;

    }

    public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        Move m = null;

        m = grider.play(player_cell, memory, nearby_cells, nearby_pheromes);
        if(m.vector.norm() < this.EPSILON ){
            m = circler.play(player_cell, memory, nearby_cells, nearby_pheromes);
        }

        // if all tries fail, just chill in place
        return m;
    }

    public Move reproduce(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        // System.out.println("Circler reproduce");
        return new Move(true, (byte)(memory|ROLE_CIRCLE), memory);
    }

    private static final double TWOPI = 2*Math.PI;

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

    private double normalizeAngle(double a, double start) {
        if( a < start ) {
            return normalizeAngle( a+TWOPI, start);
        } else if (a >= (start+TWOPI)) {
            return normalizeAngle( a-TWOPI, start);
        } else {
            return a;
        }
    }

    private Vector angle2vector(double a) {
        double dx = Cell.move_dist * Math.cos(a);
        double dy = Cell.move_dist * Math.sin(a);
        return new Vector(dx, dy);
    }


}