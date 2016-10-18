package slather.g7;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class StrategyDecider {
	
	private static String[] SIM_ARGS = {};
	private static boolean PRINT_STUFF = false;
	public static int combinationsCovered = 0;
	
	private static boolean oneDone = false;
	
	public static PrintWriter writer = null;

	public static void main(String[] args) throws FileNotFoundException {
		SIM_ARGS = args;
		writer = new PrintWriter("scores.txt");
		
		if (!PRINT_STUFF) {
			// Disabling system output
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
		}
		
		HashMap<Scenario, List<StrategyType>> possibleStrategies = getPossibleStrategies();
		generateAllCombinationsAndRunEach(possibleStrategies, new HashMap<>());
		
		writer.close();
		
		System.exit(0);
	}

	private static HashMap<Scenario, List<StrategyType>> getPossibleStrategies() {
		int num_scenarios = Scenario.values().length;
		HashMap<Scenario, List<StrategyType>> possibleStrategies = new HashMap<>(num_scenarios);
		
		// Pregnant cell
		List<StrategyType> preg = new LinkedList<>();
		preg.add(StrategyType.REPRODUCE);
		
		// Near reproduction
		List<StrategyType> rep = new LinkedList<>();
		rep.add(StrategyType.FREE_SPACE);
		
		// Nothing close-by
		List<StrategyType> emp = new LinkedList<>();
		emp.add(StrategyType.FREE_SPACE);
		
		// One pheromone
		List<StrategyType> onep = new LinkedList<>();
		onep.add(StrategyType.FREE_SPACE);
		onep.add(StrategyType.TO_ENEMIES);
		onep.add(StrategyType.CIRCLE);
		
		// Friends
		List<StrategyType> fri = new LinkedList<>();
		fri.add(StrategyType.FREE_SPACE);
		fri.add(StrategyType.KEEP_DISTANCE);
		fri.add(StrategyType.TO_ENEMIES);
		fri.add(StrategyType.TO_FRIENDS);
		fri.add(StrategyType.EXPLORER);
//		fri.add(StrategyType.VECTOR_COMBINATION);
		
		// Nearby friends
		List<StrategyType> nfri = new LinkedList<>();
		nfri.add(StrategyType.FREE_SPACE);
		nfri.add(StrategyType.KEEP_DISTANCE);
		nfri.add(StrategyType.TO_ENEMIES);
		nfri.add(StrategyType.TO_FRIENDS);
		nfri.add(StrategyType.EXPLORER);
//		nfri.add(StrategyType.VECTOR_COMBINATION);
		
		// Enemies
		List<StrategyType> ene = new LinkedList<>();
		ene.add(StrategyType.FREE_SPACE);
		ene.add(StrategyType.ATTACK);
		ene.add(StrategyType.KEEP_DISTANCE);
		ene.add(StrategyType.CIRCLE);
		
		// Nearby enemies
		List<StrategyType> nene = new LinkedList<>();
		nene.add(StrategyType.FREE_SPACE);
		nene.add(StrategyType.ATTACK);
		nene.add(StrategyType.KEEP_DISTANCE);
		nene.add(StrategyType.CIRCLE);
		nene.add(StrategyType.MOVE_TO_TARGET);
		
		// Cluster border
		List<StrategyType> clus = new LinkedList<>();
		clus.add(StrategyType.FREE_SPACE);
//		clus.add(StrategyType.VECTOR_COMBINATION);
		clus.add(StrategyType.TO_ENEMIES);
		clus.add(StrategyType.EXPLORER);
		clus.add(StrategyType.CIRCLE);
		clus.add(StrategyType.ATTACK);
		
		// Dispersed -- Checking for all strategies
		List<StrategyType> disp = Arrays.asList(StrategyType.values());
		
		possibleStrategies.put(Scenario.ALMOST_REPRODUCTION, rep);
		possibleStrategies.put(Scenario.EMPTYNESS, emp);
		possibleStrategies.put(Scenario.ONE_PHEROMONE, onep);
		possibleStrategies.put(Scenario.FRIENDS, fri);
		possibleStrategies.put(Scenario.NEARBY_FRIENDS, nfri);
		possibleStrategies.put(Scenario.ENEMIES, ene);
		possibleStrategies.put(Scenario.NEARBY_ENEMIES, nene);
		possibleStrategies.put(Scenario.CLUSTER_BORDER, clus);
		possibleStrategies.put(Scenario.DISPERSED, disp);
		possibleStrategies.put(Scenario.MAX_SIZE, preg);
		
		return possibleStrategies;
	}

	private static void generateAllCombinationsAndRunEach(HashMap<Scenario, List<StrategyType>> possibleStrategies,
															HashMap<Scenario, StrategyType> setStrategies) {
		
		int num_scenarios = Scenario.values().length;
		
		// Base case - recursion bottoms out
		if (setStrategies.size() == num_scenarios) {
			if (combinationsCovered < 3) {
				Player.strategyPerScenario = setStrategies;
				writer.write("Strategies: " + setStrategies.toString());
				
				slather.sim.Simulator.main(SIM_ARGS);

				combinationsCovered++;
				oneDone = true;
			}
			return;
		}
		
		// Find a scenario we didn't set a strategy for
		Scenario unsetScenario = null;
		for (Scenario scenario : Scenario.values()) {
			if (! setStrategies.containsKey(scenario)) {
				unsetScenario = scenario;
			}
		}
		
		// For every possible strategy
		for (StrategyType strat : possibleStrategies.get(unsetScenario)) {
			// Set the strategy
			setStrategies.put(unsetScenario, strat);
			// Set remaining strategies and run
			generateAllCombinationsAndRunEach(possibleStrategies, setStrategies);
			// Remove this strategy
			setStrategies.remove(unsetScenario);
		}
	}

}
