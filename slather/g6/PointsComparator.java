package slather.g6;

import java.util.Comparator;

import slather.sim.Point;

public class PointsComparator implements Comparator<Point> {
	@Override
	public int compare(Point a, Point b) {
		//compare two objects return the largest angle
		double one = Math.atan2(a.y, a.x);
		double two = Math.atan2(b.y, b.x);
		if (one == two)
			return 0;
		return one < two ? -1 : 1;
	}
}