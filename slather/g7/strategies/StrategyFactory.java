package slather.g7.strategies;

import slather.g7.*;

public class StrategyFactory {

	public static Strategy getStrategyByType(StrategyType st) {
		switch (st) {
		case FREE_SPACE:
			return new TrampStrategy(); // TrampStrategy
		// VECTOR_COMBINATION is gone
		case MOVE_TO_TARGET:
			return new FollowerStrategy();// FollowerStrategy
		case TO_ENEMIES:
			return new SpearmanStrategy();// SpearmanStrategy
		case TO_FRIENDS:
			return new SheepStrategy();// SheepStrategy
		case EXPLORER:
			return new ExplorerStrategy();// ExplorerStrategy
		case KEEP_DISTANCE:
			return new AutismStrategy();// AutismStrategy
		case ATTACK:
			return new ThugStrategy();// ThugStrategy
		case CIRCLE:
			return new RangerStrategy();// RangerStrategy
		case REPRODUCE:
			return new ReproductionStrategy();
		}
		return new ClusterStrategy();//Won't reach this line if everything is fine
	}
}
