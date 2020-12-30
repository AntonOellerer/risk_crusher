package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;

import java.util.Set;
import java.util.stream.Collectors;

public class AttackActionSupplier {

    private static final double RISK_THRESHOLD = 0.5;

    public static Set<RiskAction> createActions(Risk risk, Integer maxAttackerCnt) {
        final Set<Integer> sourceTerritoryIds = risk
                .getBoard()
                .getTerritoriesOccupiedByPlayer(risk.getCurrentPlayer());
        var attackActions = sourceTerritoryIds
              .stream()
              .flatMap(tId -> AttackActionSupplier.createActions(risk, tId, maxAttackerCnt).stream())
              .collect(Collectors.toSet());
        return attackActions;
    }

    private static Set<RiskAction> createActions(Risk risk, Integer srcTerritoryId, Integer maxAttackerCnt) {
        final Set<Integer> targetTerritoryIds = risk
                .getBoard()
                .neighboringEnemyTerritories(srcTerritoryId);
        return targetTerritoryIds
                .stream()
                .flatMap(tId -> AttackActionSupplier.createActions(risk, srcTerritoryId, tId, maxAttackerCnt).stream())
                .collect(Collectors.toSet());
    }

    /**
     * atm only full-attack is considered
     * @param risk
     * @param srcTerritoryId
     * @param targetTerritoryId
     * @return
     */
    private static Set<RiskAction> createActions(Risk risk, Integer srcTerritoryId, Integer targetTerritoryId, Integer maxAttackerCnt) {
        int maxAttackTroops = risk.getBoard().getMobileTroops(srcTerritoryId);
        final int defenderTroops = risk.getBoard().getTerritoryTroops(targetTerritoryId);

        if (BattleSimulator.getWinProbability(maxAttackTroops, defenderTroops) >= RISK_THRESHOLD) {
            maxAttackTroops = maxAttackerCnt != null ? Math.min(maxAttackTroops, maxAttackerCnt) : maxAttackTroops; // return only valid attacks
            return Set.of(RiskAction.attack(srcTerritoryId, targetTerritoryId, maxAttackTroops));
        }
        return Set.of();
    }
}
