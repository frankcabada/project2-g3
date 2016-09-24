package slather.sim;

import java.util.*;

public interface Player {

    public void init(double d, int t, int side_length);
    
    public Move play(Cell player_cell,
		     byte memory,
		     Set<Cell> nearby_cells,
		     Set<Pherome> nearby_pheromes);
}
