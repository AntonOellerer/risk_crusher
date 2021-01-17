package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class generates attack actions
 * At the moment, we only generate all-out attacks to keep the branching factor low.
 */
public class AttackActionSupplier {

    private static final double RISK_THRESHOLD = 0.5;


    /**
     * Create all attack actions we want to be considered.
     * At the moment, we only generate all-out attacks to keep the branching factor low.
     *
     * @param risk           The risk game
     * @param board          The risk board (This is the board of the game, it is cached in the agent to save time & space)
     * @param maxAttackerCnt The maximum amount of attackers we can use to attack
     * @return All attack actions we want to be considered (In an MCTS for example)
     */
    public static Set<RiskAction> createActions(Risk risk, RiskBoard board, Integer maxAttackerCnt) {
        final Set<Integer> sourceTerritoryIds = board
                .getTerritoriesOccupiedByPlayer(risk.getCurrentPlayer());
        return sourceTerritoryIds
                .stream()
                .flatMap(tId -> AttackActionSupplier.createActions(board, tId, maxAttackerCnt).stream())
                .collect(Collectors.toSet());
    }

    private static Set<RiskAction> createActions(RiskBoard board, Integer srcTerritoryId, Integer maxAttackerCnt) {
        final Set<Integer> targetTerritoryIds = board
                .neighboringEnemyTerritories(srcTerritoryId);
        return targetTerritoryIds
                .stream()
                .flatMap(tId -> AttackActionSupplier.createActions(board, srcTerritoryId, tId, maxAttackerCnt).stream())
                .collect(Collectors.toSet());
    }

    /**
     * atm only full-attack is considered
     * If the win probability is too low, an empty set is returned
     *
     * @param srcTerritoryId    The territory the attack is launched from
     * @param targetTerritoryId The territory being attacked
     * @param board             The risk board
     * @param maxAttackerCnt    The maximum amount of units to use
     * @return The possible attack actions deemed advantageous (only full out attack at the moment), empty if the win
     * probability is too low
     */
    private static Set<RiskAction> createActions(RiskBoard board, Integer srcTerritoryId, Integer targetTerritoryId, Integer maxAttackerCnt) {
        int maxAttackTroops = board.getMobileTroops(srcTerritoryId);
        final int defenderTroops = board.getTerritoryTroops(targetTerritoryId);

        if (BattleSimulator.getWinProbability(maxAttackTroops, defenderTroops) >= RISK_THRESHOLD) {
            maxAttackTroops = maxAttackerCnt != null ? Math.min(maxAttackTroops, maxAttackerCnt) : maxAttackTroops; // return only valid attacks
            return Set.of(RiskAction.attack(srcTerritoryId, targetTerritoryId, maxAttackTroops));
        }
        return Set.of();
    }
}
