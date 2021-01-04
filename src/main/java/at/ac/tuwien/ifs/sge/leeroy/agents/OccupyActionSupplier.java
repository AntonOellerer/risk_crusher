package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.game.ActionRecord;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;

import java.util.HashSet;
import java.util.Set;

public class OccupyActionSupplier {

    public static Set<RiskAction> createActions(Risk risk) {
        int attackActionId = risk.getNumberOfActions() - 2;
        ActionRecord attackRecord = risk.getActionRecords().get(attackActionId);
        RiskAction attackAction = (RiskAction)attackRecord.getAction();

        return createActions(risk, attackAction);
    }

    public static Set<RiskAction> createActions(Risk risk, RiskAction attackAction) {
        int srcTerritory = attackAction.attackingId();
        int targetTerritory = attackAction.defendingId();

        Set<Integer> srcEnemyNeighbors = risk.getBoard().neighboringEnemyTerritories(srcTerritory);
        Set<Integer> targetEnemyNeighbors = risk.getBoard().neighboringEnemyTerritories(targetTerritory);
        boolean isSrcSafe = srcEnemyNeighbors.isEmpty();
        boolean isTargetSafe = targetEnemyNeighbors.isEmpty();

        if (isTargetSafe && ! isSrcSafe) {
            return Set.of(RiskAction.occupy(1)); // min troops
        } else if (isSrcSafe && ! isTargetSafe) {
            return Set.of(RiskAction.occupy(risk.getBoard().getFortifyableTroops(srcTerritory))); // max troops
        }

        if (isTargetSafe && isSrcSafe) {
            // if none of the territories is at the frontline - check which is closer
            int srcFrontlineDistance = getFrontlineDistance(risk, srcTerritory);
            int targetFrontlineDistance = getFrontlineDistance(risk, targetTerritory);

            if (srcFrontlineDistance < targetFrontlineDistance) {
                return Set.of(RiskAction.occupy(1)); // min troops
            } else if (srcFrontlineDistance > targetFrontlineDistance) {
                return Set.of(RiskAction.occupy(risk.getBoard().getFortifyableTroops(srcTerritory))); // max troops
            }
            // if distance is equal let mcts decide
            return Set.of(
                    RiskAction.occupy(1),
                    RiskAction.occupy(risk.getBoard().getFortifyableTroops(srcTerritory)));
        }

        // both are frontline: evaluate - move all, move min, move according to territory ratio
        Set<RiskAction> evaluatedActions = new HashSet<>(Set.of(
                RiskAction.occupy(1),
                RiskAction.occupy(risk.getBoard().getFortifyableTroops(srcTerritory))));

        double targetFrontlineTerritoryRatio = targetEnemyNeighbors.size() * 1.0 / (targetEnemyNeighbors.size() + srcEnemyNeighbors.size());
        long occupyTroops = Math.round(risk.getBoard().getFortifyableTroops(srcTerritory) * targetFrontlineTerritoryRatio);
        if (occupyTroops > 1) {
            evaluatedActions.add(RiskAction.occupy((int) occupyTroops));
        }

        return evaluatedActions; // let mcts decide -> room for improvements
    }

    /**
     * returns the distance to the closest frontline territory
     * @param risk
     * @param sourceTerritory
     * @return
     */
    private static int getFrontlineDistance(Risk risk, int sourceTerritory) {
        Set<Integer> curLevelTerritories = Set.of(sourceTerritory);
        Set<Integer> evaluatedTerritories = new HashSet<>();
        Set<Integer> nextLevelTerritories;
        int curDistance = 0;
        while (!curLevelTerritories.isEmpty()) {
            nextLevelTerritories = new HashSet<>();
            for (Integer srcId : curLevelTerritories) {
                if (! risk.getBoard().neighboringEnemyTerritories(srcId).isEmpty()) {
                    return curDistance;
                }
                evaluatedTerritories.add(srcId);
                Set<Integer> newNeighbors = risk.getBoard().neighboringFriendlyTerritories(srcId);
                newNeighbors.removeAll(evaluatedTerritories);
                newNeighbors.removeAll(curLevelTerritories);
                nextLevelTerritories.addAll(newNeighbors);
            }
            curDistance += 1;
            curLevelTerritories = nextLevelTerritories;
        }

        if (risk.isGameOver()) {
            return Integer.MAX_VALUE;
        }
        throw new IllegalStateException("Found disconnected region - illegal risk board");
    }
}
