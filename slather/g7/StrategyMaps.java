package slather.g7;

import java.util.HashMap;

public class StrategyMaps {
	public static HashMap<Scenario, StrategyType> bigDsmallT() {
//		HashMap<Scenario, StrategyType> strategyPerScenario = new HashMap<>();
//		strategyPerScenario.put(Scenario.CLUSTER_BORDER, StrategyType.CIRCLE);
//		strategyPerScenario.put(Scenario.DISPERSED, StrategyType.FREE_SPACE);
//		strategyPerScenario.put(Scenario.EMPTYNESS, StrategyType.FREE_SPACE);
//		strategyPerScenario.put(Scenario.ENEMIES, StrategyType.EXPLORER);
//		strategyPerScenario.put(Scenario.FRIENDS, StrategyType.EXPLORER);
//		strategyPerScenario.put(Scenario.NEARBY_ENEMIES, StrategyType.FREE_SPACE);
//		strategyPerScenario.put(Scenario.NEARBY_FRIENDS, StrategyType.EXPLORER);
//		strategyPerScenario.put(Scenario.ONE_PHEROMONE, StrategyType.FREE_SPACE);
//		strategyPerScenario.put(Scenario.ALMOST_REPRODUCTION, StrategyType.FREE_SPACE);
//		strategyPerScenario.put(Scenario.MAX_SIZE, StrategyType.REPRODUCE);
//		return strategyPerScenario;
		
		HashMap<Scenario, StrategyType> strategyPerScenario = new HashMap<>();
		strategyPerScenario.put(Scenario.CLUSTER_BORDER, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.DISPERSED, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.EMPTYNESS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.FRIENDS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.NEARBY_ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.NEARBY_FRIENDS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ONE_PHEROMONE, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ALMOST_REPRODUCTION, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.MAX_SIZE, StrategyType.REPRODUCE);
		return strategyPerScenario;
	}

	public static HashMap<Scenario, StrategyType> bigDmediumT() {
//		HashMap<Scenario, StrategyType> strategyPerScenario = new HashMap<>();
//		strategyPerScenario.put(Scenario.CLUSTER_BORDER, StrategyType.CIRCLE);
//		strategyPerScenario.put(Scenario.DISPERSED, StrategyType.FREE_SPACE);
//		strategyPerScenario.put(Scenario.EMPTYNESS, StrategyType.FREE_SPACE);
//		strategyPerScenario.put(Scenario.ENEMIES, StrategyType.KEEP_DISTANCE);
//		strategyPerScenario.put(Scenario.FRIENDS, StrategyType.KEEP_DISTANCE);
//		strategyPerScenario.put(Scenario.NEARBY_ENEMIES, StrategyType.KEEP_DISTANCE);
//		strategyPerScenario.put(Scenario.NEARBY_FRIENDS, StrategyType.FREE_SPACE);
//		strategyPerScenario.put(Scenario.ONE_PHEROMONE, StrategyType.FREE_SPACE);
//		strategyPerScenario.put(Scenario.ALMOST_REPRODUCTION, StrategyType.FREE_SPACE);
//		strategyPerScenario.put(Scenario.MAX_SIZE, StrategyType.REPRODUCE);
//		return strategyPerScenario;
		
		HashMap<Scenario, StrategyType> strategyPerScenario = new HashMap<>();
		strategyPerScenario.put(Scenario.CLUSTER_BORDER, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.DISPERSED, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.EMPTYNESS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.FRIENDS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.NEARBY_ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.NEARBY_FRIENDS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ONE_PHEROMONE, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ALMOST_REPRODUCTION, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.MAX_SIZE, StrategyType.REPRODUCE);
		return strategyPerScenario;
	}
	
	public static HashMap<Scenario, StrategyType> bigDbigT() {
//		HashMap<Scenario, StrategyType> strategyPerScenario = new HashMap<>();
//		strategyPerScenario.put(Scenario.CLUSTER_BORDER, StrategyType.EXPLORER);
//		strategyPerScenario.put(Scenario.DISPERSED, StrategyType.FREE_SPACE);
//		strategyPerScenario.put(Scenario.EMPTYNESS, StrategyType.FREE_SPACE);
//		strategyPerScenario.put(Scenario.ENEMIES, StrategyType.CIRCLE);
//		strategyPerScenario.put(Scenario.FRIENDS, StrategyType.FREE_SPACE);
//		strategyPerScenario.put(Scenario.NEARBY_ENEMIES, StrategyType.FREE_SPACE);
//		strategyPerScenario.put(Scenario.NEARBY_FRIENDS, StrategyType.KEEP_DISTANCE);
//		strategyPerScenario.put(Scenario.ONE_PHEROMONE, StrategyType.FREE_SPACE);
//		strategyPerScenario.put(Scenario.ALMOST_REPRODUCTION, StrategyType.FREE_SPACE);
//		strategyPerScenario.put(Scenario.MAX_SIZE, StrategyType.REPRODUCE);
//		return strategyPerScenario;
		
		HashMap<Scenario, StrategyType> strategyPerScenario = new HashMap<>();
		strategyPerScenario.put(Scenario.CLUSTER_BORDER, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.DISPERSED, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.EMPTYNESS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.FRIENDS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.NEARBY_ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.NEARBY_FRIENDS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ONE_PHEROMONE, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ALMOST_REPRODUCTION, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.MAX_SIZE, StrategyType.REPRODUCE);
		return strategyPerScenario;
	}
	
	public static HashMap<Scenario, StrategyType> smallDsmallT() {
		HashMap<Scenario, StrategyType> strategyPerScenario = new HashMap<>();
		strategyPerScenario.put(Scenario.CLUSTER_BORDER, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.DISPERSED, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.EMPTYNESS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.FRIENDS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.NEARBY_ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.NEARBY_FRIENDS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ONE_PHEROMONE, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ALMOST_REPRODUCTION, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.MAX_SIZE, StrategyType.REPRODUCE);
		return strategyPerScenario;
	}
	
	public static HashMap<Scenario, StrategyType> smallDmediumT() {
		HashMap<Scenario, StrategyType> strategyPerScenario = new HashMap<>();
		strategyPerScenario.put(Scenario.CLUSTER_BORDER, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.DISPERSED, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.EMPTYNESS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.FRIENDS, StrategyType.FREE_SPACE);
		strategyPerScenario.put(Scenario.NEARBY_ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.NEARBY_FRIENDS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ONE_PHEROMONE, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ALMOST_REPRODUCTION, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.MAX_SIZE, StrategyType.REPRODUCE);
		return strategyPerScenario;
	}
	
	public static HashMap<Scenario, StrategyType> smallDbigT() {
		HashMap<Scenario, StrategyType> strategyPerScenario = new HashMap<>();
		strategyPerScenario.put(Scenario.CLUSTER_BORDER, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.DISPERSED, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.EMPTYNESS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.FRIENDS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.NEARBY_ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.NEARBY_FRIENDS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ONE_PHEROMONE, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ALMOST_REPRODUCTION, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.MAX_SIZE, StrategyType.REPRODUCE);
		return strategyPerScenario;
	}
	
	public static HashMap<Scenario, StrategyType> mediumDsmallT() {
		HashMap<Scenario, StrategyType> strategyPerScenario = new HashMap<>();
		strategyPerScenario.put(Scenario.CLUSTER_BORDER, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.DISPERSED, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.EMPTYNESS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.FRIENDS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.NEARBY_ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.NEARBY_FRIENDS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ONE_PHEROMONE, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ALMOST_REPRODUCTION, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.MAX_SIZE, StrategyType.REPRODUCE);
		return strategyPerScenario;
	}
	
	public static HashMap<Scenario, StrategyType> mediumDmediumT() {
		HashMap<Scenario, StrategyType> strategyPerScenario = new HashMap<>();
		strategyPerScenario.put(Scenario.CLUSTER_BORDER, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.DISPERSED, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.EMPTYNESS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.FRIENDS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.NEARBY_ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.NEARBY_FRIENDS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ONE_PHEROMONE, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ALMOST_REPRODUCTION, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.MAX_SIZE, StrategyType.REPRODUCE);
		return strategyPerScenario;
	}
	
	public static HashMap<Scenario, StrategyType> mediumDbigT() {
		HashMap<Scenario, StrategyType> strategyPerScenario = new HashMap<>();
		strategyPerScenario.put(Scenario.CLUSTER_BORDER, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.DISPERSED, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.EMPTYNESS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.FRIENDS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.NEARBY_ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.NEARBY_FRIENDS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ONE_PHEROMONE, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ALMOST_REPRODUCTION, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.MAX_SIZE, StrategyType.REPRODUCE);
		return strategyPerScenario;
	}
	
	public static HashMap<Scenario, StrategyType> defaultMapInit() {
		HashMap<Scenario, StrategyType> strategyPerScenario = new HashMap<>();
		strategyPerScenario.put(Scenario.CLUSTER_BORDER, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.DISPERSED, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.EMPTYNESS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.FRIENDS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.NEARBY_ENEMIES, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.NEARBY_FRIENDS, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ONE_PHEROMONE, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.ALMOST_REPRODUCTION, StrategyType.KEEP_DISTANCE);
		strategyPerScenario.put(Scenario.MAX_SIZE, StrategyType.REPRODUCE);
		return strategyPerScenario;
	}
}
