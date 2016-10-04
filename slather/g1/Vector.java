package slather.g1;

import slather.sim.Point;

/**
 * Created by David on 9/19/2016.
 */
public class Vector {

    private double x, y;

    public Vector(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector(double x1, double y1, double x2, double y2) {
        this.x = x1 - x2;
        this.y = y1 - y2;
    }

    public Vector(Point p1, Point p2) {
        this.x = p1.x - p2.x;
        this.y = p1.y - p2.y;
    }

    public Vector invert() {
        return new Vector(-x, -y);
    }

    public Vector add(Vector other) {
        return new Vector(this.x + other.x, this.y + other.y);
    }

    public Point add(Point p) {
        return new Point(this.x + p.x, this.y + p.y);
    }

    public Vector multiply(double scalar) {
        return new Vector(this.x * scalar, this.y * scalar);
    }

    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Point toPoint() {
        return new Point(this.x, this.y);
    }

    public String toString() {
        return "Vector(" + this.x + "," + this.y + ")";
    }

}
