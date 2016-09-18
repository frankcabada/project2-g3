package slather.sim;

public class Point {

    public final double x;
    public final double y;
    private static int side_length = 100;

    public Point(double x, double y) {
	this.x = x;
	this.y = y;
    }

    public double distance(Point p) { // distance on a torus
	double dx = Math.min ( Math.max(x,p.x) - Math.min(x,p.x), Math.min(x,p.x)+side_length - Math.max(x,p.x) );
	double dy = Math.min ( Math.max(y,p.y) - Math.min(y,p.y), Math.min(y,p.y)+side_length - Math.max(y,p.y) );	
	return Math.hypot( dx, dy );
    }

    public double norm() {
	return Math.hypot(x,y);
    }

    public Point move(Point v) {
	return new Point( (x+v.x+side_length) % side_length, (y+v.y+side_length) % side_length );
    }

    protected static void set_side_length(int new_side_length) {
        side_length = new_side_length;
    }
    
}
