package slather.g6;

import slather.sim.Cell;
import slather.sim.Point;
import slather.sim.Move;
import slather.sim.Pherome;
import java.util.*;

public class Player2 implements slather.sim.Player {

    private Random gen;
    private int tailLength;
    private double radius;
    private double dTheta;
    private double p_circle = 0;
    private double size = 100;

    public void init(double d, int t) {
        gen = new Random(System.currentTimeMillis());
        tailLength = t;
        radius = 2 * tailLength / (2*Math.PI);
        dTheta = 1 / radius;
    }

    public Move playCircle(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        double theta = byte2angle( memory );
        double nextTheta = normalizeAngle(theta + dTheta,0);
        byte nextMemory = 0;
        Point vector = null;
        // Try to go any of four normal directions.
        for(int i=0; i<4; ++i) {
            nextMemory = angle2byte( nextTheta, memory );
            // System.out.println("memory,angles " +memory+","+nextMemory+"," +theta+","+nextTheta);
            vector = angle2vector( nextTheta);
            if (!collides( player_cell, vector, nearby_cells, nearby_pheromes))
                return new Move(vector, nextMemory);
            nextTheta += Math.PI/2;
        }

        // if all tries fail, just chill in place
        return new Move(new Point(0,0), (byte)1);
    }

    public Move playScout(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        memory = (byte) (memory& (~ROLE_CIRCLE));
        double acc_x = 0, acc_y = 0;
        Point position = player_cell.getPosition();
        double radius = player_cell.getDiameter() * 0.5;
        for (Cell cell : nearby_cells) {
            Point p = cell.getPosition();
            double dx = p.x - position.x, dy = p.y - position.y;
            if (Math.abs(dx) > Math.abs(p.x + size - position.x)) dx = p.x + size - position.x;
            if (Math.abs(dx) > Math.abs(p.x - size - position.x)) dx = p.x - size - position.x;
            if (Math.abs(dy) > Math.abs(p.y + size - position.y)) dy = p.y + size - position.y;
            if (Math.abs(dy) > Math.abs(p.y - size - position.y)) dy = p.y - size - position.y;

            acc_x -= dx; acc_y -= dy;
        }
        for (Pherome pherome : nearby_pheromes) {
            Point p = pherome.getPosition();
            double dx = p.x - position.x, dy = p.y - position.y;
            if (Math.abs(dx) > Math.abs(p.x + size - position.x)) dx = p.x + size - position.x;
            if (Math.abs(dx) > Math.abs(p.x - size - position.x)) dx = p.x - size - position.x;
            if (Math.abs(dy) > Math.abs(p.y + size - position.y)) dy = p.y + size - position.y;
            if (Math.abs(dy) > Math.abs(p.y - size - position.y)) dy = p.y - size - position.y;

            acc_x -= dx; acc_y -= dy;
        }

        //if (width > 0) 
        if (Math.abs(acc_x) < 1e-7 && Math.abs(acc_y) < 1e-7) acc_x = 1;
        double t = Math.hypot(acc_x, acc_y);
        return new Move(new Point(acc_x / t, acc_y / t), memory);
    }

    //return true if there are more than one nearby friendly cell
    private boolean crowded(Cell player_cell, Set<Cell> nearby_cells){
        Iterator<Cell> itr = nearby_cells.iterator();
        int count = 0;
        while(itr.hasNext()){
            Cell c = itr.next();
            if(c.player == player_cell.player) count++;
        }
        return count >= 1;
    }

    //by default goes scout;
    //everytime we split there's a p_circle chance we go circle; 
    public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
        double theta = byte2angle( memory );
        // reproduce whenever possible
        if (player_cell.getDiameter() >= 2){
            byte memory2 = angle2byte( normalizeAngle(theta + Math.PI,0), memory );
            if(!byteIsCircle(memory) && (gen.nextDouble() < p_circle)) {
                //System.out.println("Generating a circle");
                memory2 = (byte) (memory2|ROLE_CIRCLE);
            } else {
                //System.out.println("Generating a scout");
                memory2 = (byte) (memory2& (~ROLE_CIRCLE));
            }
            return new Move(true, memory, memory2);
        } 

        //if surrounding is too crowded, play circle
        boolean nextCircle = byteIsCircle(memory);
        if(crowded(player_cell,nearby_cells)) nextCircle = true;
        if(!nextCircle) 
            return playScout(player_cell, memory, nearby_cells, nearby_pheromes);
        return playCircle(player_cell, memory, nearby_cells, nearby_pheromes);
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

    private static final int ANGLE_BITS = 5;
    private static final int ANGLE_MIN = 0;
    private static final int ANGLE_MAX = 1 << ANGLE_BITS;
    private static final int ANGLE_MASK = ANGLE_MAX - 1;

    private static final int ROLE_BITS = 1;
    private static final int ROLE_MIN = ANGLE_MAX;
    private static final int ROLE_MAX = ROLE_MIN + ( 1<<ROLE_BITS );
    private static final int ROLE_MASK = (ROLE_MAX - 1) & ~ANGLE_MASK;
    private static final int ROLE_SCOUT = 0 << ANGLE_BITS;
    private static final int ROLE_CIRCLE = 1 << ANGLE_BITS;

    private boolean byteIsCircle(byte b){
        //System.out.printf("%d %d %d\n",b,ROLE_MASK,ROLE_CIRCLE);
        return (b&ROLE_MASK)==ROLE_CIRCLE;
    }

    private static final double TWOPI = 2*Math.PI;

    private double byte2angle(byte b) {
        // -128 <= b < 128
        // -1 <= b/128 < 1
        // -pi <= a < pi
        return normalizeAngle(TWOPI * (((double) ((b) & ANGLE_MASK) ) / ANGLE_MAX), 0);
    }

    private byte angle2byte(double a, byte b) {
        final double actualAngle = ((normalizeAngle(a,0) / TWOPI)*ANGLE_MAX);
        final int anglePart = (int) (((int)actualAngle) & ANGLE_MASK);
        final byte memoryPart = (byte) ( b & ~ANGLE_MASK);
        // System.out.println("angle2byte "+ memoryPart +","+ anglePart +","+ (normalizeAngle(a,0)/TWOPI) +","+ ANGLE_MAX +","+ a);
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

    private Point angle2vector(double a) {
        double dx = Cell.move_dist * Math.cos(a);
        double dy = Cell.move_dist * Math.sin(a);
        return new Point(dx, dy);
    }

    // convert an angle (in 2-deg increments) to a vector with magnitude Cell.move_dist (max allowed movement distance)
    private Point extractVectorFromAngle(int arg) {
        double theta = Math.toRadians( 2* (double)arg );

        double dx = Cell.move_dist * Math.cos(theta + dTheta);
        double dy = Cell.move_dist * Math.sin(theta + dTheta);
        return new Point(dx, dy);
    }

}
