package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class generates fortification actions
 */
public class FortificationActionSupplier {

    /**
     * recursively checks if fortification can be done - returns all possible fortification action for frontline territories
     * i.e if there are territories T1 neighboring frontline territories T0, all actions T1->T0 are returned
     * if T1->T0 is empty, T2-> T1 is returned and so on
     *
     * @param risk  The game
     * @param board The game board (separately so it can be cached)
     * @return The fortification actions which should be considered
     */
    public static Set<RiskAction> createActions(Risk risk, RiskBoard board) {
        final Set<Integer> frontlineTerritoryIds = board
                .getTerritoriesOccupiedByPlayer(risk.getCurrentPlayer())
                .stream()
                .filter(tId -> board.neighboringEnemyTerritories(tId).size() > 0)
                .collect(Collectors.toSet());

        return FortificationActionSupplier.createActions(board, frontlineTerritoryIds, new HashSet<>(frontlineTerritoryIds));
    }

    private static Set<RiskAction> createActions(RiskBoard board, Set<Integer> neighborTerritoryIds, Set<Integer> checkedTerritoryIds) {
        Set<RiskAction> levelActions = new HashSet<>();
        Set<Integer> nextLevelTerritoryIds = new HashSet<>();
        for (Integer tId : neighborTerritoryIds) {
            final Set<Integer> subseqTerritorySet = board
                    .neighboringFriendlyTerritories(tId)
                    .stream()
                    .filter(ntId -> !checkedTerritoryIds.contains(ntId))
                    .collect(Collectors.toSet());

            levelActions.addAll(subseqTerritorySet
                    .stream()
                    .map(stId -> RiskAction.fortify(stId, tId, board.getMobileTroops(stId)))
                    .filter(action -> action.troops() > 0)
                    .collect(Collectors.toSet()));
            nextLevelTerritoryIds.addAll(subseqTerritorySet);
        }
        if (! levelActions.isEmpty() || nextLevelTerritoryIds.isEmpty()) {
            // some found on level or last level (i.e. no fortification possible)
            return levelActions;
        }
        checkedTerritoryIds.addAll(nextLevelTerritoryIds);
        return FortificationActionSupplier.createActions(board, nextLevelTerritoryIds, checkedTerritoryIds);
    }
}
