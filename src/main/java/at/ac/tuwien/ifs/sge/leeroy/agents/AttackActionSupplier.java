package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;

import java.util.Set;
import java.util.stream.Collectors;

public class AttackActionSupplier {

    private static final double RISK_THRESHOLD = 0.5;

    public static Set<RiskAction> createActions(Risk risk, RiskBoard board, Integer maxAttackerCnt) {
        final Set<Integer> sourceTerritoryIds = board
                .getTerritoriesOccupiedByPlayer(risk.getCurrentPlayer());
        var attackActions = sourceTerritoryIds
              .stream()
              .flatMap(tId -> AttackActionSupplier.createActions(risk, board, tId, maxAttackerCnt).stream())
              .collect(Collectors.toSet());
        return attackActions;
    }

    private static Set<RiskAction> createActions(Risk risk, RiskBoard board, Integer srcTerritoryId, Integer maxAttackerCnt) {
        final Set<Integer> targetTerritoryIds = board
                .neighboringEnemyTerritories(srcTerritoryId);
        return targetTerritoryIds
                .stream()
                .flatMap(tId -> AttackActionSupplier.createActions(risk, board, srcTerritoryId, tId, maxAttackerCnt).stream())
                .collect(Collectors.toSet());
    }

    /**
     * atm only full-attack is considered
     * @param risk
     * @param srcTerritoryId
     * @param targetTerritoryId
     * @return
     */
    private static Set<RiskAction> createActions(Risk risk, RiskBoard board, Integer srcTerritoryId, Integer targetTerritoryId, Integer maxAttackerCnt) {
        int maxAttackTroops = board.getMobileTroops(srcTerritoryId);
        final int defenderTroops = board.getTerritoryTroops(targetTerritoryId);

        if (BattleSimulator.getWinProbability(maxAttackTroops, defenderTroops) >= RISK_THRESHOLD) {
            maxAttackTroops = maxAttackerCnt != null ? Math.min(maxAttackTroops, maxAttackerCnt) : maxAttackTroops; // return only valid attacks
            return Set.of(RiskAction.attack(srcTerritoryId, targetTerritoryId, maxAttackTroops));
        }
        return Set.of();
    }
}
