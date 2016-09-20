package slather.sim;

public class Pherome extends GridObject{

    public final int max_duration;
    private int duration = 0;
    
    public Pherome(Point position, int player, int max_duration) {
	super(position, player);
	this.max_duration = max_duration;
    }

    public double distance(GridObject other) {
	return super.distance(other);
    }

    protected boolean step() {
	return (++duration > max_duration);
    }

    protected void refresh() {
	duration = 0;
    }

}
