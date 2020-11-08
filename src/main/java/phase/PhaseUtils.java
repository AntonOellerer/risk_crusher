package phase;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;

public class PhaseUtils {
    public static boolean stillUnoccupiedTerritories(RiskBoard board) {
        return board.getTerritories().values().stream().anyMatch(riskTerritory -> riskTerritory.getTroops() == 0);
    }

    public static boolean initialPlacementFinished(RiskBoard board, int playerNumber, int numberOfStartingTroops) {
        return board
                .getTerritoriesOccupiedByPlayer(playerNumber)
                .stream()
                .map(board::getTerritoryTroops)
                .reduce(Integer::sum)
                .orElseThrow() == numberOfStartingTroops;
    }

    public static boolean inReinforcing(Risk risk) {
        return risk
                .getPossibleActions()
                .stream()
                .anyMatch(riskAction -> riskAction.attackingId() == -1);
    }

    public static boolean inAttack(Risk risk) {
        return risk
                .getPossibleActions()
                .stream()
                .findFirst()
                .orElseThrow()
                .attackingId() != -1;

    }

    public static boolean inOccupy(Risk risk) {
        return risk
                .getPossibleActions()
                .stream()
                .findFirst()
                .map(riskAction -> riskAction.attackingId() == -2 && riskAction.defendingId() == -2)
                .orElseThrow();
    }
}
