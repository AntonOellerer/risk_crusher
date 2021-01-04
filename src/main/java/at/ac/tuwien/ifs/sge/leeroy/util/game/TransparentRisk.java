package at.ac.tuwien.ifs.sge.leeroy.util.game;

import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import at.ac.tuwien.ifs.sge.leeroy.util.heuristics.TerritoryBonusProvider;
import at.ac.tuwien.ifs.sge.leeroy.util.logging.RiskLogger;
import at.ac.tuwien.ifs.sge.leeroy.util.logging.RiskLoggerProvider;

import java.util.HashMap;
import java.util.Map;

import static at.ac.tuwien.ifs.sge.leeroy.util.logging.RiskLogger.RiskLoggerType.*;

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
                            "Player: " + activePlayer + " Next action: " +
                    toReadableAction(riskAction, getBoard()) + "\n" +
                            (shouldPrintMap(riskAction, getBoard()) ? getGame().toTextRepresentation() : ""));
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

    private String toReadableAction(RiskAction action, RiskBoard board) {
        if (action == null) {
            return "Start";
        }
        if (action.attackingId() == -2 && ! board.isOccupyPhase()) {
            return "End of phase";
        }
        if (action.defendingId() == -3) {
            //??
            return String.format("Play cards for bonus (%d) - %s", action.troops(), action.toString());
        }
        if (action.defendingId() == -4) {
            return String.format("Receive bonus (%d)", action.troops());
        }
        if (board.isReinforcementPhase()) {
            return String.format("Reinforce %s (%d)", TerritoryNames.of(action.defendingId()), action.troops());
        } else if (board.isAttackPhase()) {
            if (action.attackingId() == -1) {
                return String.format("Attack lost %d, Defence lost %d", action.attackerCasualties(), action.defenderCasualties());
            }
            return String.format("Attack %s -> %s (%d)", TerritoryNames.of(action.attackingId()),
                    TerritoryNames.of(action.defendingId()), action.troops());
        } else if (board.isOccupyPhase()) {
            return String.format("Occupying with %d", action.troops());
        } else if (board.isFortifyPhase()) {
            return String.format("Fortify %s -> %s (%d)", TerritoryNames.of(action.attackingId()),
                    TerritoryNames.of(action.defendingId()), action.troops());
        }
        return action.toString();
    }

    private boolean shouldPrintMap(RiskAction action, RiskBoard board) {
        return (board.isAttackPhase() && action.attackingId() != -1) || board.isFortifyPhase()
                || board.isReinforcementPhase();
    }
}
