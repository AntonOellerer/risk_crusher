package util.game;

import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import util.logging.RiskLogger;
import util.logging.RiskLoggerProvider;

import static util.logging.RiskLogger.RiskLoggerType.TROOP_SIZE_EV;
import static util.logging.RiskLogger.RiskLoggerType.OCCUPIED_TERRITORY_COUNT;

/***
 * used to obtain global game information within each turn
 */
public class TransparentRisk extends Risk {

    private String gameName;

    /**
     * required for reflection
     * @param yaml
     * @param numberOfPlayers
     */
    public TransparentRisk(String yaml, int numberOfPlayers) {
        super(yaml, numberOfPlayers);
    }

    /***
     * required for reflection
     */
    public TransparentRisk() {
        super();
    }

    public TransparentRisk(Risk risk, String gameName)
    {
        super(risk);
        this.gameName = gameName;
    }

    public Game<RiskAction, RiskBoard> doAction(RiskAction riskAction) {
        int activePlayer = getCurrentPlayer();

        if (activePlayer >= 0) {
            logActiveAction(riskAction);
        } else {
            // casualties, draw cards, bonus,...
        }

        return new TransparentRisk((Risk) super.doAction(riskAction), this.gameName);
    }

    private void logActiveAction(RiskAction riskAction) {
        int activePlayer = getCurrentPlayer();
        int actionNr = getActionRecords().size();
        RiskLogger rlp = RiskLoggerProvider.getInstance().forGame(this.gameName);

        long nrFrontlineTroops = 0;
        long nrBackupTroops = 0;
        int nrFrontlineTerritories = 0;
        int nrBackupTerritories = 0;
        for (Integer occupiedTerritory : getBoard().getTerritoriesOccupiedByPlayer(activePlayer)) {
            long nrTroops = getBoard().getTerritoryTroops(occupiedTerritory);
            if (getBoard().neighboringEnemyTerritories(occupiedTerritory).isEmpty()) {
                nrBackupTroops += nrTroops;
                nrBackupTerritories += 1;
            } else {
                nrFrontlineTroops += nrTroops;
                nrFrontlineTerritories += 1;
            }
        }
        long nrTotalTroops = nrFrontlineTroops + nrBackupTroops;
        long nrTotalTerritories = nrFrontlineTerritories + nrBackupTerritories;

        rlp.getRiskLogger(TROOP_SIZE_EV).info(actionNr + "," + activePlayer + "," + nrTotalTroops + "," +
                nrFrontlineTroops + "," + nrBackupTroops);
        rlp.getRiskLogger(OCCUPIED_TERRITORY_COUNT).info(actionNr + "," + activePlayer + "," + nrTotalTerritories +
                "," + nrFrontlineTerritories + "," + nrBackupTerritories);
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }
}
