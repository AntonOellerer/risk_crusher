package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class FortificationActionSupplier {



    public static Set<RiskAction> createActions(Risk risk) {
        final Set<Integer> frontlineTerritoryIds = risk
                .getBoard()
                .getTerritoriesOccupiedByPlayer(risk.getCurrentPlayer())
                .stream()
                .filter(tId -> risk.getBoard().neighboringEnemyTerritories(tId).size() > 0)
                .collect(Collectors.toSet());

        return FortificationActionSupplier.createActions(risk, frontlineTerritoryIds, new HashSet<>(frontlineTerritoryIds));
    }

    /**
     * recursively checks if fortification can be done - returns all possible fortification action for the closest fortification actions
     * i.e if there are territories T1 neighboring frontline territories T0, all actions T1->T0 are returned
     * if T1->T0 is empty, T2-> T1 is returned and so on
     * @param risk
     * @param neighborTerritoryIds
     * @param checkedTerritoryIds
     * @return
     */
    public static Set<RiskAction> createActions(Risk risk, Set<Integer> neighborTerritoryIds, Set<Integer> checkedTerritoryIds) {
        Set<RiskAction> levelActions = new HashSet<>();
        Set<Integer> nextLevelTerritoryIds = new HashSet<>();
        for (Integer tId : neighborTerritoryIds) {
            final Set<Integer> subseqTerritorySet = risk
                    .getBoard()
                    .neighboringFriendlyTerritories(tId)
                    .stream()
                    .filter(ntId -> ! checkedTerritoryIds.contains(ntId))
                    .collect(Collectors.toSet());

            levelActions.addAll(subseqTerritorySet
                    .stream()
                    .map(stId -> RiskAction.fortify(stId, tId, risk.getBoard().getMobileTroops(stId)))
                    .filter(action -> action.troops() > 0)
                    .collect(Collectors.toSet()));
            nextLevelTerritoryIds.addAll(subseqTerritorySet);
        }
        if (! levelActions.isEmpty() || nextLevelTerritoryIds.isEmpty()) {
            // some found on level or last level (i.e. no fortification possible)
            return levelActions;
        }
        checkedTerritoryIds.addAll(nextLevelTerritoryIds);
        return FortificationActionSupplier.createActions(risk, nextLevelTerritoryIds, checkedTerritoryIds);
    }
}
