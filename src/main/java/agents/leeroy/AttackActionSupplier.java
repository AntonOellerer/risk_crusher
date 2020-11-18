package agents.leeroy;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AttackActionSupplier {

    private static final double RISK_THRESHOLD = 0.5;

    public static Set<RiskAction> createActions(Risk risk) {
        final Set<Integer> sourceTerritoryIds = risk
                .getBoard()
                .getTerritoriesOccupiedByPlayer(risk.getCurrentPlayer());
        var attackActions = sourceTerritoryIds
              .stream()
              .flatMap(tId -> AttackActionSupplier.createActions(risk, tId).stream())
              .collect(Collectors.toSet());
        attackActions.add(RiskAction.endPhase());
        return attackActions;
    }

    private static Set<RiskAction> createActions(Risk risk, Integer srcTerritoryId) {
        final Set<Integer> targetTerritoryIds = risk
                .getBoard()
                .neighboringEnemyTerritories(srcTerritoryId);
        return targetTerritoryIds
                .stream()
                .flatMap(tId -> AttackActionSupplier.createActions(risk, srcTerritoryId, tId).stream())
                .collect(Collectors.toSet());
    }

    /**
     * atm only full-attack is considered
     * @param risk
     * @param srcTerritoryId
     * @param targetTerritoryId
     * @return
     */
    private static Set<RiskAction> createActions(Risk risk, Integer srcTerritoryId, Integer targetTerritoryId) {
        final int maxAttackTroops = risk.getBoard().getMaxAttackingTroops(srcTerritoryId);
        final int defenderTroops = risk.getBoard().getTerritoryTroops(targetTerritoryId);

        if (BattleSimulator.getWinProbability(maxAttackTroops, defenderTroops) >= RISK_THRESHOLD) {
            return Set.of(RiskAction.attack(srcTerritoryId, targetTerritoryId, maxAttackTroops));
        }
        return Set.of();
    }
}
