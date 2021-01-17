package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.game.ActionRecord;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;

import java.util.HashSet;
import java.util.Set;

/**
 * This class is responsible for generating occupation actions.
 * The actions generated depend on the location of the territories on
 * the board. If one territory is neighbour to an enemy territory, the
 * number of troops there is maximized, same if one territory is closer
 * to the frontline.
 * If the distance is the same (or both are on the frontline) actions are generated
 * for moving the maximum amount, moving the minimum amount, and moving based on
 * the ratio of neighbouring enemy troops.
 */
public class OccupyActionSupplier {

    /**
     * Generate actions for occupying a newly conquered territory.
     * This method should only be used if the attack action is not at the ready,
     * since retrieving it from the actions history takes a lot of time.
     *
     * @param risk      The risk game
     * @param riskBoard The risk board (separate for caching)
     * @return A set of actions for occupying a newly conquered territory.
     */
    public static Set<RiskAction> createActions(Risk risk, RiskBoard riskBoard) {
        int attackActionId = risk.getNumberOfActions() - 2;
        ActionRecord attackRecord = risk.getActionRecords().get(attackActionId);
        RiskAction attackAction = (RiskAction) attackRecord.getAction();

        return createActions(risk, riskBoard, attackAction);
    }

    /**
     * Generate actions for occupying a newly conquered territory, signaled w/
     * the passed RiskAction
     *
     * @param risk         The risk game
     * @param riskBoard    The risk board (separate for caching)
     * @param attackAction The attack action leading to conquering the territory.
     * @return A set of actions for occupying a newly conquered territory.
     */
    public static Set<RiskAction> createActions(Risk risk, RiskBoard riskBoard, RiskAction attackAction) {
        int srcTerritory = attackAction.attackingId();
        int targetTerritory = attackAction.defendingId();

        Set<Integer> srcEnemyNeighbors = riskBoard.neighboringEnemyTerritories(srcTerritory);
        Set<Integer> targetEnemyNeighbors = riskBoard.neighboringEnemyTerritories(targetTerritory);
        boolean isSrcSafe = srcEnemyNeighbors.isEmpty();
        boolean isTargetSafe = targetEnemyNeighbors.isEmpty();

        if (isTargetSafe && !isSrcSafe) {
            return Set.of(RiskAction.occupy(1)); // min troops
        } else if (isSrcSafe && !isTargetSafe) {
            return Set.of(RiskAction.occupy(riskBoard.getFortifyableTroops(srcTerritory))); // max troops
        }

        if (isTargetSafe && isSrcSafe) {
            // if none of the territories is at the frontline - check which is closer
            int srcFrontlineDistance = getFrontlineDistance(risk, riskBoard, srcTerritory);
            int targetFrontlineDistance = getFrontlineDistance(risk, riskBoard, targetTerritory);

            if (srcFrontlineDistance < targetFrontlineDistance) {
                return Set.of(RiskAction.occupy(1)); // min troops
            } else if (srcFrontlineDistance > targetFrontlineDistance) {
                return Set.of(RiskAction.occupy(riskBoard.getFortifyableTroops(srcTerritory))); // max troops
            }
            // if distance is equal let mcts decide
            return Set.of(
                    RiskAction.occupy(1),
                    RiskAction.occupy(riskBoard.getFortifyableTroops(srcTerritory)));
        }

        // both are frontline: evaluate - move all, move min, move according to territory ratio
        Set<RiskAction> evaluatedActions = new HashSet<>(Set.of(
                RiskAction.occupy(1),
                RiskAction.occupy(riskBoard.getFortifyableTroops(srcTerritory))));

        double targetFrontlineTerritoryRatio = targetEnemyNeighbors.size() * 1.0 / (targetEnemyNeighbors.size() + srcEnemyNeighbors.size());
        long occupyTroops = Math.round(riskBoard.getFortifyableTroops(srcTerritory) * targetFrontlineTerritoryRatio);
        if (occupyTroops > 1) {
            evaluatedActions.add(RiskAction.occupy((int) occupyTroops));
        }

        return evaluatedActions; // let mcts decide -> room for improvements
    }

    /**
     * returns the distance to the closest frontline territory
     *
     * @param risk            The risk game
     * @param sourceTerritory The territory to get the closest frontile distance for
     * @return The distance to the closest frontline
     */
    private static int getFrontlineDistance(Risk risk, RiskBoard riskBoard, int sourceTerritory) {
        Set<Integer> curLevelTerritories = Set.of(sourceTerritory);
        Set<Integer> evaluatedTerritories = new HashSet<>();
        Set<Integer> nextLevelTerritories;
        int curDistance = 0;
        while (!curLevelTerritories.isEmpty()) {
            nextLevelTerritories = new HashSet<>();
            for (Integer srcId : curLevelTerritories) {
                if (! riskBoard.neighboringEnemyTerritories(srcId).isEmpty()) {
                    return curDistance;
                }
                evaluatedTerritories.add(srcId);
                Set<Integer> newNeighbors = riskBoard.neighboringFriendlyTerritories(srcId);
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
