package slather.g2;

import slather.sim.Cell;
import slather.sim.Point;
import slather.sim.Move;
import slather.sim.Pherome;
import java.util.*;

import slather.g2.util.Vector;


public class Circler extends Chiller {

    public double RADIUS;
    public double DELTA_THETA;

    private static final double TWOPI = 2*Math.PI;

    public void init(double d, int t, int side_length) {
        System.out.println("Circler init");

        this.RANDOM_GENERATOR = new Random(System.currentTimeMillis());
        this.VISION = d;
        this.TAIL_LENGTH = t;
        this.BOARD_SIZE = side_length;

        getPropertiesSafe();

        RADIUS = this.RADIUS_TO_TAIL_RATIO * t / (TWOPI);
        DELTA_THETA = 1 / RADIUS;

    }

    public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        double theta = byte2angle( memory );
        double nextTheta = normalizeAngle(theta + DELTA_THETA,0);
        byte nextMemory = 0;
        Vector vector = null;
        // Try to go any of four normal directions.
        for(int i=0; i<4; ++i) {
            nextMemory = angle2byte( nextTheta, memory );
            // System.out.println("memory,angles " +memory+","+nextMemory+"," +theta+","+nextTheta);
            vector = angle2vector( nextTheta);
            if (!this.collides( player_cell, vector, nearby_cells, nearby_pheromes))
                return new Move(vector, nextMemory);
            nextTheta += Math.PI/2;
        }

        // if all tries fail, just chill in place
        return new Move(new Point(0,0), memory);
    }

    public Move reproduce(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        // System.out.println("Circler reproduce");
        return new Move(true, (byte)(memory|ROLE_CIRCLE), memory);
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
        return (byte) ((anglePart | memoryPart));
    }

    private double normalizeAngle(double a, double start) {
        final int num_circles = (int) Math.floor((a - start) / TWOPI);
        return a + num_circles*TWOPI;
    }

    private Vector angle2vector(double a) {
        double dx = Cell.move_dist * Math.cos(a);
        double dy = Cell.move_dist * Math.sin(a);
        return new Vector(dx, dy);
    }


}