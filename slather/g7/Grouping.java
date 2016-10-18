package slather.g7;

import java.util.*;

import slather.sim.*;


public class Grouping {
	public Set<Cell> friendCells;
	public Set<Cell> enemyCells;
	public Set<Pherome> friendPheromes;
	public Set<Pherome> enemyPheromes;
	
	public Grouping(Cell myCell,Set<Cell> cells,Set<Pherome> pheromes){
		friendCells=new HashSet<>();
		enemyCells=new HashSet<>();
		friendPheromes=new HashSet<>();
		enemyPheromes=new HashSet<>();
		
		for(Cell c:cells){
			if(c.player==myCell.player)
				friendCells.add(c);
			else
				enemyCells.add(c);
		}
		for(Pherome p:pheromes){
			if(p.player==myCell.player)
				friendPheromes.add(p);
			else
				enemyPheromes.add(p);
		}
	}

}
