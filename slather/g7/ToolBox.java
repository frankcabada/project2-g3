package slather.g7;

import java.util.Set;

import slather.sim.Cell;
import slather.sim.Pherome;
import slather.sim.Point;

public class ToolBox {
	
	/* Return an angle between 0~2PI */
	public static double getCosine(Point myPoint,Point otherPoint){
		double diffX=otherPoint.x-myPoint.x;
		double diffY=otherPoint.y-myPoint.y;
		double diffDist=Math.sqrt(diffX*diffX+diffY*diffY);
		/* Avoid divide zero */
		if(diffDist==0.0)
			return 0.0;
		
		double angle=Math.acos(diffY/diffDist);
		if(otherPoint.x<myPoint.x)
			return 2*Math.PI-angle;
		else if(otherPoint.x>myPoint.x)
			return angle;
		else{/*Same x*/
			if(otherPoint.y>=myPoint.y)
				return 0.0;
			else 
				return Math.PI;
		}
	}
	/* Return a angle subtraction result between 0~2PI */
	public static double angleDiff(double startAngle,double endAngle){
		if(endAngle>startAngle)
			return endAngle-startAngle;
		else
			return 2*Math.PI+endAngle-startAngle;
	}
	
	public static Point newDirection(Point me,double theta){
		double diffX=Math.sin(theta);
		double diffY=Math.cos(theta);
		return new Point(me.x+diffX,me.y+diffY);
	}
	/* Normalize the distance to default of 1mm */
	public static Point normalizeDistance(Point... points) {
		double offX = 0.0;
		double offY = 0.0;
		for (Point p : points) {
			offX += p.x;
			offY += p.y;
		}
		// Normalize the force to 1mm length
		double hypotenuse = Math.sqrt(offX * offX + offY * offY);

		Point toReturn;
		if (hypotenuse == 0.0) {
			toReturn = new Point(0.0, 0.0);
		} else {
			offX /= hypotenuse;
			offY /= hypotenuse;
			toReturn = new Point(offX, offY);
		}

		// System.out.println("The normalized direction is X: " + toReturn.x + "
		// Y: " + toReturn.y);
		return toReturn;
	}

	/* Normalize the distance to the specified length */
	public static Point normalizeDistance(double length, Point... points) {
		double offX = 0.0;
		double offY = 0.0;
		for (Point p : points) {
			offX += p.x;
			offY += p.y;
		}
		// Normalize the force to 1mm length
		double hypotenuse = Math.sqrt(offX * offX + offY * offY);

		Point toReturn;
		if (hypotenuse == 0.0) {
			toReturn = new Point(0.0, 0.0);
		} else {
			offX /= hypotenuse;
			offY /= hypotenuse;
			toReturn = new Point(offX * length, offY * length);
		}

		System.out.println("The normalized direction is X: " + toReturn.x + " Y: " + toReturn.y);
		return toReturn;
	}
}
