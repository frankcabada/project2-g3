package slather.g7;

import java.util.HashSet;
import java.util.Set;

import slather.sim.Cell;
import slather.sim.Pherome;

public class ScenarioClass{
	
	public static boolean isEmpty(Cell myCell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes){
		return (nearby_cells.isEmpty() && nearby_pheromes.isEmpty());
	}
	
	
	public static boolean isAboutToReproduce(Cell myCell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes){
		return (myCell.getDiameter() > ToolBox.diameterBeforeReproduction);
	}
	
	
	public static boolean isOnlyMyPherome(Cell myCell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes){
	
	Set<Cell> friends = new HashSet<>();
	Set<Cell> enemies = new HashSet<>();
	for (Cell c : nearby_cells) {
		if (c.player == myCell.player)
			friends.add(c);
		else
			enemies.add(c);
	}
	boolean flag = (friends.isEmpty() && enemies.isEmpty());
	if (flag){
		for (Pherome p : nearby_pheromes) {
			if (p.player != myCell.player){
				return false;
			}
	}
	}
	return flag;
	
	
	}
	
	public static boolean isOnlyFriends(Cell myCell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes){
		
		Set<Cell> friends = new HashSet<>();
		Set<Cell> enemies = new HashSet<>();
		for (Cell c : nearby_cells) {
			if (c.player == myCell.player)
				friends.add(c);
			else
				enemies.add(c);
			
		}
		
		return (!friends.isEmpty() && enemies.isEmpty());
	}
	public static boolean isOnlyEnemies(Cell myCell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes){
		
		Set<Cell> friends = new HashSet<>();
		Set<Cell> enemies = new HashSet<>();
		for (Cell c : nearby_cells) {
			if (c.player == myCell.player)
				friends.add(c);
			else
				enemies.add(c);
			
		}
		
		return (friends.isEmpty() && !enemies.isEmpty());
	}
	
	public static boolean isNearbyFriends(Cell myCell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes){
		
		Set<Cell> friends = new HashSet<>();
		Set<Cell> enemies = new HashSet<>();
		for (Cell c : nearby_cells) {
			if (c.player == myCell.player)
				friends.add(c);
			else
				enemies.add(c);
			
		}
		double distMin = 50;
		if (!friends.isEmpty() && !enemies.isEmpty()){
		for (Cell p : friends){
			if (myCell.distance(p) <= distMin){
				distMin = myCell.distance(p);
			}
			
			
		}
		/* If any enemy is closer to the cell than a friend, this returns false */
		for (Cell p : enemies){
			if (myCell.distance(p) <= distMin){
				return false;
			}
		
		}
		return true;
		}
		return false;
		
		
		
	}

	
	public static boolean isNearbyEnemies(Cell myCell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes){
		
		Set<Cell> friends = new HashSet<>();
		Set<Cell> enemies = new HashSet<>();
		for (Cell c : nearby_cells) {
			if (c.player == myCell.player)
				friends.add(c);
			else
				enemies.add(c);
			
		}
		double distMin = 50;
		if (!friends.isEmpty() && !enemies.isEmpty()){
		for (Cell p : enemies){
			if (myCell.distance(p) <= distMin){
				distMin = myCell.distance(p);
			}
			
			
		}
		for (Cell p : friends){
			if (myCell.distance(p) <= distMin){
				return false;
			}
		
		}
		return true;
		}
		return false;
		
		
		
		
		
		
	}
	public static boolean isClusterBorder(Cell myCell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes){
		Set<Cell> enemies = new HashSet<>();
		Set<Cell> friends = new HashSet<>();
		for (Cell c : nearby_cells) {
			if (c.player == myCell.player)
				friends.add(c);
			else
				enemies.add(c);
			
		}
		double max_angle_friends = 0;
		double max_angle_enemies = 0;
		if (!friends.isEmpty() && !enemies.isEmpty()){

		for (Cell p : friends){
			for (Cell c: friends){
				if (ToolBox.getCosine(p.getPosition(), c.getPosition()) > max_angle_friends && (ToolBox.getCosine(p.getPosition(), c.getPosition()) < Math.PI )){
					max_angle_friends = ToolBox.getCosine(p.getPosition(), c.getPosition());
				}
			}
		}
		for (Cell p : enemies){
			for (Cell c: enemies){
				if (ToolBox.getCosine(p.getPosition(), c.getPosition()) > max_angle_enemies && (ToolBox.getCosine(p.getPosition(), c.getPosition()) < Math.PI)){
					max_angle_enemies = ToolBox.getCosine(p.getPosition(), c.getPosition());
				}
			}
		}
		
		return ((max_angle_friends > ToolBox.friendsAngleThreshold) && (max_angle_enemies > ToolBox.enemiesAngleThreshold));
		}
		return false;
		


	}
	
	
	

	
}
