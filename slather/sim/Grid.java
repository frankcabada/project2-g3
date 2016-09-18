package slather.sim;

import java.util.*;

public class Grid {

    ArrayList< ArrayList< HashSet<GridObject>>> grid; // for computing distances
    ArrayList<Cell> cells = new ArrayList<Cell>(); // for shuffling order of movement
    int N;
    double d;
    double side;

    class GridObjectsContainer {
	public Set<Cell> nearby_cells;
	public Set<Pherome> nearby_pheromes;
	public GridObjectsContainer() {
	    this.nearby_cells = new HashSet<Cell>();
	    this.nearby_pheromes = new HashSet<Pherome>();
	}
    }
    
    public Grid(double side, double d) {
	this.d = d;
	N = (int) Math.round(Math.ceil(side / d));
	grid = new ArrayList< ArrayList< HashSet<GridObject> > >(N);
	this.side = side;
	for (int i=0; i<N; i++) {
	    grid.add(i, new ArrayList< HashSet<GridObject> >(N));
	    for (int j=0; j<N; j++) 
		grid.get(i).add(j, new HashSet<GridObject>());
	}

    }

    public void add(GridObject q) {
	grid.get(getI(q)).get(getJ(q)).add(q);
	if (q instanceof Cell)
	    cells.add((Cell)q);
    }

    public boolean shiftsGrid(Cell q, Point v) {
	Point dest = q.getPosition().move(v);
	if (getI(q) != getI(dest) || getJ(q) != getJ(dest))
	    return true;
	else
	    return false;
    }

    public void readd(Cell q) {
	grid.get(getI(q)).get(getJ(q)).add(q);
    }

    public void remove(Cell q) {
	grid.get(getI(q)).get(getJ(q)).remove(q);
    }

    public ArrayList<Cell> shuffle_cells() {
	Collections.shuffle(cells);
	return cells;
    }

    public void age_pheromes() {
	for (int i = 0; i<N; i++) {
	    for (int j = 0; j<N; j++) {
		Iterator<GridObject> it = grid.get(i).get(j).iterator();
		while (it.hasNext()) {
		    GridObject next = it.next();
		    if (next instanceof Pherome) {
			Pherome casted_next = (Pherome) next;
			if (casted_next.step())
			    it.remove();
		    }
		}
	    }
	}
    }

    public GridObjectsContainer get_nearby(Cell q) {
	GridObjectsContainer output = new GridObjectsContainer();
	for (int i = (getI(q)-2+N) %N; i != (getI(q)+2+N) %N; i=(i+1)%N) {
	    for (int j = (getJ(q)-2+N) %N; j != (getJ(q)+2+N) %N; j=(j+1)%N) {
		Iterator<GridObject> it = grid.get(i).get(j).iterator();
		while (it.hasNext()) {
		    GridObject next = it.next();
		    if (q.distance(next) < d) {
			if (next instanceof Cell && next != q)
			    output.nearby_cells.add( (Cell)next);
			else if (next instanceof Pherome)
			    output.nearby_pheromes.add( (Pherome)next);
		    }
		}
	    }
	}
	return output;
    }

    public String objects_state() {
	StringBuffer cell_buf = new StringBuffer();
	StringBuffer pherome_buf = new StringBuffer();
	boolean cell_first = true;
	boolean pherome_first = true;
	for (int i = 0; i<N; i++) {
	    for (int j = 0; j<N; j++) {
		Iterator<GridObject> it = grid.get(i).get(j).iterator();
		while (it.hasNext()) {
		    GridObject next = it.next();
		    if (next instanceof Cell) {
			if (cell_first)
			    cell_first = false;
			else
			    cell_buf.append(";");
			Cell casted_next = (Cell)next;
			cell_buf.append(next.player + "," + next.getPosition().x + "," + next.getPosition().y + "," + casted_next.getDiameter());			
		    }
		    else if (next instanceof Pherome) {
			if (pherome_first)
			    pherome_first = false;
			else
			    pherome_buf.append(";");
			pherome_buf.append(next.player + "," + next.getPosition().x + "," + next.getPosition().y);     
		    }
		}
	    }
	}
	return cell_buf.toString() + "\n" + pherome_buf.toString();
    }

    private int getI(GridObject q) {
	return getI(q.getPosition());
    }

    private int getI(Point p) {
	return (int)Math.round(Math.floor(p.x / d));
    }

    private int getJ(GridObject q) {
	return getJ(q.getPosition());
    }    
    private int getJ(Point p) {
	return (int)Math.round(Math.floor(p.y / d));
    }
    
}
