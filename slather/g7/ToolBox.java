package slather.g7;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import slather.sim.Cell;
import slather.sim.GridObject;
import slather.sim.Pherome;
import slather.sim.Point;
import java.math.*;

public class ToolBox {

	public static double EXPLORER_PROBABILITY = 0;
	public static Random gen = new Random();
	public static double diameterBeforeReproduction = 1.92;
	public static double friendsAngleThreshold = Math.PI / 3;
	public static double enemiesAngleThreshold = Math.PI / 3;

	/*
	 * Due to wrap around, the difference between two points in coordinate x and
	 * y should be in range (-50,50]
	 */
	public static Point pointDistance(Point me, Point you) {
		double diffX = you.x - me.x;
		if (diffX > 50)
			diffX -= 100;
		else if (diffX < -50)
			diffX += 100;
		double diffY = you.y - me.y;
		if (diffY > 50)
			diffY -= 100;
		else if (diffY < -50)
			diffY += 100;
		return new Point(diffX, diffY);
	}

	/* Return an angle between 0~2PI */
	public static double getCosine(Point myPoint, Point otherPoint) {
		Point diffPoint=pointDistance(myPoint,otherPoint);
		double diffX = diffPoint.x;
		double diffY = diffPoint.y;
		double diffDist = Math.sqrt(diffX * diffX + diffY * diffY);
		/* Avoid divide zero */
		if (diffDist == 0.0)
			return 0.0;

		// double angle=Math.acos(diffY/diffDist);
		double angle = Math.asin(diffY / diffDist);
		double targetY;
		if(Math.abs(otherPoint.y-myPoint.y)<50)
			targetY=otherPoint.y;
		else{
			if(otherPoint.y>myPoint.y)
				targetY=otherPoint.y-100;
			else
				targetY=otherPoint.y+100;
		}
		double targetX;
		if(Math.abs(otherPoint.x-myPoint.x)<50)
			targetX=otherPoint.x;
		else{
			if(otherPoint.x>myPoint.x)
				targetX=otherPoint.x-100;
			else
				targetX=otherPoint.x+100;
		}
		
		if (targetY< myPoint.y)
			if (targetX >= myPoint.x)
				return 2 * Math.PI + angle;
			else
				return Math.PI - angle;
		else if (targetY > myPoint.y)
			return angle;
		else {/* Same x */
			if (targetX >= myPoint.x)
				return 0.0;
			else
				return Math.PI;
		}
	}

	/* Return a angle subtraction result between 0~2PI */
	public static double angleDiff(double startAngle, double endAngle) {
		if (endAngle > startAngle)
			return endAngle - startAngle;
		else
			return 2 * Math.PI + endAngle - startAngle;
	}

	/* The distance is 1. */
	public static Point newDirection(Point me, double theta) {
		double diffX = Math.cos(theta);
		double diffY = Math.sin(theta);
		// return new Point(me.x + diffX, me.y + diffY);
		return new Point(diffX, diffY);
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

	// Calculate the distance with no regard to radius
	public static double calcRawDist(GridObject a, GridObject b) {
		Point ap = a.getPosition();
		Point bp = b.getPosition();
		Point diffPoint = pointDistance(ap, bp);
		return Math.sqrt(diffPoint.x * diffPoint.x + diffPoint.y * diffPoint.y);
	}

	/*
	 * chaseIt>0: chase the cell. chaseIt<0: run away from the cell.
	 * 
	 */
	public static Point joinForcesFromCells(Cell myCell, Set<Cell> cells, double weightForCell, int chaseIt,
			boolean needRadius) {
		Point myPos = myCell.getPosition();
		double offX = 0.0;
		double offY = 0.0;
		for (Cell c : cells) {
			Point cPos = c.getPosition();
			/* The smaller the distance, the larger the force */
			Point diff = ToolBox.pointDistance(cPos, myPos);
			// The force is proportional to the mass of the cell, which depends
			// on the square of diameter in a 2d environment
			if (needRadius) {
				offX += chaseIt * diff.x * weightForCell * (c.getDiameter() * c.getDiameter());
				offY += chaseIt * diff.y * weightForCell * (c.getDiameter() * c.getDiameter());
			} else {
				offX += chaseIt * diff.x * weightForCell;
				offY += chaseIt * diff.y * weightForCell;
			}

		}

		// System.out.println("The merged force from cells is X: " + offX + " Y:
		// " + offY);

		return new Point(offX, offY);
	}

	public static Point joinGravityFromCells(Cell myCell, Set<Cell> cells, double weightForCell, int chaseIt) {
		Point myPos = myCell.getPosition();
		double myRadius = myCell.getDiameter();
		double offX = 0.0;
		double offY = 0.0;
		for (Cell c : cells) {
			Point cPos = c.getPosition();
			/* The smaller the distance, the larger the force */
			Point diff = ToolBox.pointDistance(cPos, myPos);
			// G=m1m2/r^2
			if (Math.abs(diff.x) < 0.00001)// avoid overflow in division
				offX += 0.0;
			else
				offX += chaseIt * weightForCell * c.getDiameter() * c.getDiameter() * myRadius * myRadius
						/ (diff.x * Math.abs(diff.x));
			if (Math.abs(diff.y) < 0.00001)
				offY += 0.0;
			else
				offY += chaseIt * weightForCell * c.getDiameter() * c.getDiameter() * myRadius * myRadius
						/ (diff.y * Math.abs(diff.y));
		}
		// System.out.println("The merged force from cells is X: " + offX + " Y:
		// " + offY);

		return new Point(offX, offY);
	}

	public static Point joinGravityFromPheromes(Cell myCell, Set<Pherome> pheromes, double weightForPherome,
			int chaseIt) {
		Point myPos = myCell.getPosition();
		double myRadius = myCell.getDiameter();
		double offX = 0.0;
		double offY = 0.0;
		for (Pherome p : pheromes) {
			Point pPos = p.getPosition();
			/* The smaller the distance, the larger the force */
			Point diff = ToolBox.pointDistance(pPos, myPos);
			// G=m1m2/r^2
			if (Math.abs(diff.x) < 0.00001)// avoid overflow in division
				offX += 0.0;
			else
				offX += chaseIt * weightForPherome * myRadius * myRadius / (diff.x * Math.abs(diff.x));
			if (Math.abs(diff.y) < 0.00001)
				offY += 0.0;
			else
				offY += chaseIt * weightForPherome * myRadius * myRadius / (diff.y * Math.abs(diff.y));
		}
		// System.out.println("The merged force from cells is X: " + offX + " Y:
		// " + offY);

		return new Point(offX, offY);
	}

	/*
	 * chaseIt>0: chase the pheromone; chaseIt<0: run away from the pheromone
	 * 
	 */
	public static Point joinForcesFromPheromes(Cell myCell, Set<Pherome> pheromes, double weight, int chaseIt) {
		Point myPos = myCell.getPosition();
		double offX = 0.0;
		double offY = 0.0;
		for (Pherome p : pheromes) {
			Point pPos = p.getPosition();
			/* The smaller the distance, the larger the force */
			Point diff = ToolBox.pointDistance(pPos, myPos);
			// The force is proportional to the mass of the cell, which depends
			// on the square of diameter in a 2d environment
			offX += chaseIt * diff.x * weight;
			offY += chaseIt * diff.y * weight;
		}

		// System.out.println("The merged force from pheromes is X: " + offX + "
		// Y: " + offY);
		return new Point(offX, offY);
	}

	public static Point addRandomnessAndGenerateFinalDirection(Point direction) {
		double desiredMean = 0.0;
		double desiredStandardDeviation = 0.0;// Set a very small deviation so
		// Set a very small deviation so
		// that the whole force thing
		// still makes sense
		double offX = direction.x + gen.nextGaussian() * desiredStandardDeviation + desiredMean;
		double offY = direction.y + gen.nextGaussian() * desiredStandardDeviation + desiredMean;

		Point newDir = ToolBox.normalizeDistance(new Point(offX, offY));
		// System.out.println("The randomized direction has X: " + newDir.x + "
		// Y: " + newDir.y);
		return newDir;
	}

	public static Point generateRandomDirection() {
		double x = gen.nextDouble() - 0.5;
		double y = gen.nextDouble() - 0.5;
		return new Point(x, y);
	}

	/*
	 * Distance related function
	 */
	public static Set<Cell> limitVisionOnCells(Cell player_cell, Set<Cell> nearby_cells, double limit) {
		Set<Cell> restrictedNearbyCells = new HashSet<>();
		for (Cell cell : nearby_cells) {
			if (player_cell.distance(cell) <= limit) {
				restrictedNearbyCells.add(cell);
			}
		}
		return restrictedNearbyCells;
	}

	public static Set<Pherome> limitVisionOnPheromes(Cell player_cell, Set<Pherome> nearby_pheromes, double limit) {
		Set<Pherome> restrictedNearbyPhermoes = new HashSet<>();
		for (Pherome pherome : nearby_pheromes) {
			if (player_cell.distance(pherome) <= limit) {
				restrictedNearbyPhermoes.add(pherome);
			}
		}
		return restrictedNearbyPhermoes;
	}

	public static Point checkSpaceForGrowth(Cell player_cell, Point vector, Set<Cell> nearby_cells,
			Set<Pherome> nearby_pheromes) {
		System.out.println("Recevied vector is " + vector.x + vector.y);
		nearby_cells = ToolBox.limitVisionOnCells(player_cell, nearby_cells, 2.0);
		nearby_pheromes = ToolBox.limitVisionOnPheromes(player_cell, nearby_pheromes, 1.1);
		System.out.println("Checking " + nearby_cells.size() + " cells and " + nearby_pheromes.size() + " pheromes");

		Iterator<Cell> cell_it = nearby_cells.iterator();
		double newDiameter = 1.01 * player_cell.getDiameter();
		double dirX, dirY;
		dirX = vector.x;
		dirY = vector.y;
		double newDistanceToMove;

		while (cell_it.hasNext()) {
			Cell other = cell_it.next();
			Point destination = player_cell.getPosition().move(new Point(dirX, dirY));

			if (destination.distance(other.getPosition()) < 0.5 * newDiameter + 0.5 * other.getDiameter()
					+ 0.00011) {
				newDistanceToMove = (Math.pow(dirX,2) + Math.pow(dirY,2)) - 0.01*player_cell.getDiameter();
				
				dirX = (Math.sqrt(newDistanceToMove)) * dirX;
				dirY = (Math.sqrt(newDistanceToMove)) * dirY;

				destination = player_cell.getPosition().move(new Point(dirX, dirY));
				System.out.println("Updating new direction due to cell to "+destination.x+", "+destination.y);
			}
		}
		Iterator<Pherome> pherome_it = nearby_pheromes.iterator();
		while (pherome_it.hasNext()) {
			Pherome other = pherome_it.next();
			Point destination = player_cell.getPosition().move(new Point(dirX, dirY));

			if (other.player != player_cell.player
					&& destination.distance(other.getPosition()) < 0.5 * newDiameter + 0.0001) {
				newDistanceToMove = (Math.pow(dirX,2) + Math.pow(dirY,2)) - 0.01*player_cell.getDiameter();
				
				dirX = (Math.sqrt(newDistanceToMove)) * dirX;
				dirY = (Math.sqrt(newDistanceToMove)) * dirY;
				destination = player_cell.getPosition().move(new Point(dirX, dirY));
				System.out.println("Updating new direction to "+destination.x+", "+destination.y);
			}
		}
		Point toReturn=new Point(dirX,dirY);
		System.out.println("Final direction:"+toReturn.x+", "+toReturn.y);
		return new Point(dirX, dirY);
	}
}
