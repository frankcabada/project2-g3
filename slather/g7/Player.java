package slather.g7;

import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;

import slather.g7.strategies.*;
import slather.sim.Cell;
import slather.sim.GridObject;
import slather.sim.Move;
import slather.sim.Pherome;
import slather.sim.Point;

public class Player implements slather.sim.Player {

	private Random gen;
	private Strategy strategy;

	public static int T, T1 = 2, T2 = 8;
	public static double D, D1 = 2.0, D2 = 6.0;
	public static int num_def_sides;
	public static double vision = 7.0;

	// This map will contain all the scenario -> strategy info
	public static HashMap<Scenario, StrategyType> strategyPerScenario;

	@Override
	public void init(double d, int t, int side_length) {
		/* Redirect output stream */
		PrintStream stream=System.out;
		System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
		    @Override public void write(int b) {}
		}) {
		    @Override public void flush() {}
		    @Override public void close() {}
		    @Override public void write(int b) {}
		    @Override public void write(byte[] b) {}
		    @Override public void write(byte[] buf, int off, int len) {}
		    @Override public void print(boolean b) {}
		    @Override public void print(char c) {}
		    @Override public void print(int i) {}
		    @Override public void print(long l) {}
		    @Override public void print(float f) {}
		    @Override public void print(double d) {}
		    @Override public void print(char[] s) {}
		    @Override public void print(String s) {}
		    @Override public void print(Object obj) {}
		    @Override public void println() {}
		    @Override public void println(boolean x) {}
		    @Override public void println(char x) {}
		    @Override public void println(int x) {}
		    @Override public void println(long x) {}
		    @Override public void println(float x) {}
		    @Override public void println(double x) {}
		    @Override public void println(char[] x) {}
		    @Override public void println(String x) {}
		    @Override public void println(Object x) {}
		    @Override public java.io.PrintStream printf(String format, Object... args) { return this; }
		    @Override public java.io.PrintStream printf(java.util.Locale l, String format, Object... args) { return this; }
		    @Override public java.io.PrintStream format(String format, Object... args) { return this; }
		    @Override public java.io.PrintStream format(java.util.Locale l, String format, Object... args) { return this; }
		    @Override public java.io.PrintStream append(CharSequence csq) { return this; }
		    @Override public java.io.PrintStream append(CharSequence csq, int start, int end) { return this; }
		    @Override public java.io.PrintStream append(char c) { return this; }
		});
		
		T = t;
		D = d;
		gen = new Random();
		strategy = new ClusterStrategy();
		num_def_sides = Integer.min(16, Integer.max(4, T));
		initScenarioStrategyMapping(d, t);
		
		/* Return print stream to out */
		System.setOut(stream);
	}

	@Override
	public Move play(Cell player_cell, byte memory, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
//		System.out.println("Memory is " + memory);
		
		/* Redirect output stream */
		PrintStream stream=System.out;
		System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
		    @Override public void write(int b) {}
		}) {
		    @Override public void flush() {}
		    @Override public void close() {}
		    @Override public void write(int b) {}
		    @Override public void write(byte[] b) {}
		    @Override public void write(byte[] buf, int off, int len) {}
		    @Override public void print(boolean b) {}
		    @Override public void print(char c) {}
		    @Override public void print(int i) {}
		    @Override public void print(long l) {}
		    @Override public void print(float f) {}
		    @Override public void print(double d) {}
		    @Override public void print(char[] s) {}
		    @Override public void print(String s) {}
		    @Override public void print(Object obj) {}
		    @Override public void println() {}
		    @Override public void println(boolean x) {}
		    @Override public void println(char x) {}
		    @Override public void println(int x) {}
		    @Override public void println(long x) {}
		    @Override public void println(float x) {}
		    @Override public void println(double x) {}
		    @Override public void println(char[] x) {}
		    @Override public void println(String x) {}
		    @Override public void println(Object x) {}
		    @Override public java.io.PrintStream printf(String format, Object... args) { return this; }
		    @Override public java.io.PrintStream printf(java.util.Locale l, String format, Object... args) { return this; }
		    @Override public java.io.PrintStream format(String format, Object... args) { return this; }
		    @Override public java.io.PrintStream format(java.util.Locale l, String format, Object... args) { return this; }
		    @Override public java.io.PrintStream append(CharSequence csq) { return this; }
		    @Override public java.io.PrintStream append(CharSequence csq, int start, int end) { return this; }
		    @Override public java.io.PrintStream append(char c) { return this; }
		});

		try {
			// Check the type of cell
			String memStr = DefenderMemory.byteToString(memory);
			char defOrExp = memStr.charAt(0);
			// System.out.println("Cell type flag: "+defOrExp);
			
			/*
			 * 0: Explorer 1: Defender
			 */
			Memory m;
			//dummy memory
			if(memStr.equals("00000000")){
				DummyMemory thisMem=new DummyMemory();
				m=thisMem;
			}else if (defOrExp == '0') {
				ExplorerMemory thisMem = ExplorerMemory.getNewObject();
				thisMem.initialize(memory);
				m = thisMem;
			} else if (defOrExp == '1') {
				DefenderMemory thisMem = DefenderMemory.getNewObject();
				thisMem.initialize(memory);
				m = thisMem;
			} else {
				System.out.println("The cell is not recognized with bit " + defOrExp);
				m = DefenderMemory.getNewObject();
			}

			// Convert the byte memory to binary string for easier usage
			Scenario currentScenario = getScenario(player_cell, nearby_cells, nearby_pheromes);
			StrategyType st = strategyPerScenario.get(currentScenario);
			Move toTake;
			if (st == null) {
				System.out.println(
						"Error: Corresponding strategy for scenario " + currentScenario.name() + " is not found!");
				System.out.println("Fall back to ClusterStrategy");
				toTake = strategy.generateMove(player_cell, m, nearby_cells, nearby_pheromes);
				System.out.println("New memory for ClusterStrategy " + toTake.memory);
			} else if (st == StrategyType.REPRODUCE) {
				System.out.println("Retrieved strategy " + st.name() + " for scenario " + currentScenario.name());
				Strategy mindSet = StrategyFactory.getStrategyByType(st);
				toTake = mindSet.generateMove(player_cell, m, nearby_cells, nearby_pheromes);
				System.out.println("Reproducing.");
				return toTake;
			} else if (st == StrategyType.CIRCLE){
				System.out.println("Retrieved strategy " + st.name() + " for scenario " + currentScenario.name());
				Strategy mindSet = StrategyFactory.getStrategyByType(st);
				DefenderMemory thisMem = DefenderMemory.getNewObject();
				thisMem.initialize(memory);
				m = thisMem;
				toTake = mindSet.generateMove(player_cell, m, nearby_cells, nearby_pheromes);
				System.out.println("Circling");
				return toTake;
			} else if (st == StrategyType.EXPLORER){
				System.out.println("Retrieved strategy " + st.name() + " for scenario " + currentScenario.name());
				Strategy mindSet = StrategyFactory.getStrategyByType(st);
				ExplorerMemory thisMem = ExplorerMemory.getNewObject();
				thisMem.initialize(memory);
				m = thisMem;
				toTake = mindSet.generateMove(player_cell, m, nearby_cells, nearby_pheromes);
				System.out.println("Exploring");
				return toTake;
			} else {
				System.out.println("Retrieved strategy " + st.name() + " for scenario " + currentScenario.name());
				Strategy mindSet = StrategyFactory.getStrategyByType(st);
				toTake = mindSet.generateMove(player_cell, m, nearby_cells, nearby_pheromes);
				System.out.println("New memory for flexible Strategy is " + toTake.memory);
			}

			/*
			 * Todo: Add distance check to ensure that we can grow after move.
			 */
			/*Point dir = toTake.vector;
			System.out.println("Checking the distance for growth.");
			dir = ToolBox.checkSpaceForGrowth(player_cell, dir, nearby_cells, nearby_pheromes);
			toTake = new Move(dir, toTake.memory);
			System.out.println("Checked direction with regard to growth is x:" + dir.x + " y:" + dir.y);
			
			 If we cannot move in the chosen direction, keep a distance with things around 
			if(Math.abs(dir.x) < 0.01 && Math.abs(dir.y) < 0.01){
				System.out.println("The cell cannot move in this direction. Fall back to keeping a distance with everything.");
				toTake = new AutismStrategy().generateMove(player_cell, m, nearby_cells, nearby_pheromes);
				dir = ToolBox.checkSpaceForGrowth(player_cell, dir, nearby_cells, nearby_pheromes);
				toTake = new Move(dir, toTake.memory);
			}else{
				System.out.println("Moving in this turn");
			}*/
			return toTake;
		} catch (Exception e) {
			System.out.println("========= Exception in player 7 =========");
			e.printStackTrace();
			return null;
		} finally{

			/* Return print stream to out */
			System.setOut(stream);
		}
	}

	private Scenario getScenario(Cell myCell, Set<Cell> nearby_cells, Set<Pherome> nearby_pheromes) {
		Set<Cell> friends = new HashSet<>();
		Set<Cell> enemies = new HashSet<>();
		for (Cell c : nearby_cells) {
			if (c.player == myCell.player)
				friends.add(c);
			else
				enemies.add(c);
		}
		if (myCell.getDiameter() >= 2.0) {
			return Scenario.MAX_SIZE;
		}
		if (ScenarioClass.isAboutToReproduce(myCell, nearby_cells, nearby_pheromes)) {
			return Scenario.ALMOST_REPRODUCTION;
		}
		if (ScenarioClass.isEmpty(myCell, nearby_cells, nearby_pheromes)) {
			return Scenario.EMPTYNESS;
		}
		if (ScenarioClass.isOnlyMyPherome(myCell, nearby_cells, nearby_pheromes)) {
			return Scenario.ONE_PHEROMONE;
		}
		if (ScenarioClass.isClusterBorder(myCell, nearby_cells, nearby_pheromes)) {
			return Scenario.CLUSTER_BORDER;
		}
		if ((enemies.isEmpty() && friends.size() < ToolBox.cellThreshold) || (friends.isEmpty() && enemies.size() < ToolBox.cellThreshold)){
			return Scenario.EMPTYNESS;
		}
		if (ScenarioClass.isOnlyFriends(myCell, nearby_cells, nearby_pheromes)) {
			return Scenario.FRIENDS;
		}
		if (ScenarioClass.isOnlyEnemies(myCell, nearby_cells, nearby_pheromes)) {
			return Scenario.ENEMIES;
		}
		if (ScenarioClass.isNearbyFriends(myCell, nearby_cells, nearby_pheromes)) {
			return Scenario.NEARBY_FRIENDS;
		}
		if (ScenarioClass.isNearbyEnemies(myCell, nearby_cells, nearby_pheromes)) {
			return Scenario.NEARBY_ENEMIES;
		}

		return Scenario.DISPERSED;
	}

	public Strategy getStrategy() {
		return strategy;
	}

	public void initScenarioStrategyMapping(double d, int t) {
		if (d >= D2) {
			if (t <= T1) {
				strategyPerScenario = StrategyMaps.bigDsmallT();
			} else if (t <= T2) {
				strategyPerScenario = StrategyMaps.bigDmediumT();
			} else {
				strategyPerScenario = StrategyMaps.bigDbigT();
			}
		}else if (d>=D1){
			if (t <= T1) {
				strategyPerScenario = StrategyMaps.smallDsmallT();
			} else if (t <= T2) {
				strategyPerScenario = StrategyMaps.smallDmediumT();
			} else {
				strategyPerScenario = StrategyMaps.smallDbigT();
			}
			
		}else {
			if (t <= T1) {
				strategyPerScenario = StrategyMaps.mediumDsmallT();
			} else if (t <= T2) {
				strategyPerScenario = StrategyMaps.mediumDmediumT();
			} else {
				strategyPerScenario = StrategyMaps.mediumDbigT();
			}
		}
		
//		strategyPerScenario = StrategyMaps.defaultMapInit();
	}
	

}