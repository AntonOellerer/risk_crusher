package at.ac.tuwien.ifs.sge.leeroy.phase;

import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;

public class PhaseUtils {
    /**
     * Check whether there are still unoccupied territories on the board
     *
     * @param board The board to check
     * @return Whether there are still unoccupied territories
     */
    public static boolean stillUnoccupiedTerritories(RiskBoard board) {
        return board.getTerritories().values().stream().anyMatch(riskTerritory -> riskTerritory.getTroops() == 0);
    }
}
