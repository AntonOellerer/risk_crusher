package util.game;

import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import util.heuristics.TerritoryBonusProvider;
import util.logging.RiskLogger;
import util.logging.RiskLoggerProvider;

import java.util.HashMap;
import java.util.Map;

import static util.logging.RiskLogger.RiskLoggerType.*;

/***
 * used to obtain global game information within each turn
 */
public class TransparentRisk extends Risk {

    private String gameName;
    final private static int MAP_OUTPUT_INTERVAL = 1;

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

        if (getActionRecords().size() % MAP_OUTPUT_INTERVAL == 0) {
            RiskLoggerProvider.getInstance().forGame(this.gameName).getRiskLogger(MAP).info(
                    getGame().getActionRecords().size() + ":\t" +
                    getGame().getPreviousAction() + "\n" +
                    getGame().toTextRepresentation());
        }

        return new TransparentRisk((Risk) super.doAction(riskAction), this.gameName);
    }

    private void logActiveAction(RiskAction riskAction) {
        int activePlayer = getCurrentPlayer();
        int actionNr = getActionRecords().size();
        RiskLogger rlp = RiskLoggerProvider.getInstance().forGame(this.gameName);

        Map<Integer, Double> continentOccupationRate = new HashMap<>();
        for (Integer continentId : TerritoryBonusProvider.getInstance().getContinentIds()) {
            continentOccupationRate.put(continentId, 0.0);
        }
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
            // continent based information
            int continentId = getBoard().getTerritories().get(occupiedTerritory).getContinentId();
            double occupationRate = continentOccupationRate.get(continentId) + 1.0/TerritoryBonusProvider.getInstance().getTerritorySize(continentId);
            continentOccupationRate.put(continentId, occupationRate);
        }
        long nrTotalTroops = nrFrontlineTroops + nrBackupTroops;
        long nrTotalTerritories = nrFrontlineTerritories + nrBackupTerritories;

        rlp.getRiskLogger(TROOP_SIZE_EV).info(actionNr + "," + activePlayer + "," + nrTotalTroops + "," +
                nrFrontlineTroops + "," + nrBackupTroops);
        rlp.getRiskLogger(OCCUPIED_TERRITORY_COUNT).info(actionNr + "," + activePlayer + "," + nrTotalTerritories +
                "," + nrFrontlineTerritories + "," + nrBackupTerritories);
        rlp.getRiskLogger(CONTINENT_OCCUPATION_RATES).info(actionNr + "," + activePlayer +
                createContinentRateOutput(continentOccupationRate));
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    private String createContinentRateOutput(Map<Integer, Double> occupancyRateMap) {
        StringBuilder sb = new StringBuilder();
        for (Double or : occupancyRateMap.values()) {
            sb.append(", " + or);
        }
        return sb.toString();
    }
}
