package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;
import at.ac.tuwien.ifs.sge.util.Util;
import org.javatuples.Pair;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class containing code to select the best territory to reinforce heuristically.
 * The selection generally tries to select a node having an already advantageous, but not overwhelming
 * troop force compared to the enemy's neighbouring nodes, on a continent where the player has realistic chances of
 * occupying it.
 */
public class HeuristicReinforce {
    private static final double TROOPS_RELATION_TERRITORIES_THRESHOLD = 0.8;
    private static final double TROOPS_RELATION_CONTINENT_THRESHOLD = 0.9;

    private static final Logger logger = Logger.getLogger(HeuristicReinforce.class.getName());

    /**
     * Get a risk action to reinforce the territory deemed best by the algorithm
     *
     * @param playerNumber The acting player
     * @param game         The risk game
     * @param board        The risk board (separate so it can be cached)
     * @return A risk action for reinforcing the territory deemed most advantageous
     */
    public static RiskAction reinforce(int playerNumber, Risk game, RiskBoard board) {
        return reinforce(playerNumber, board, game.getPossibleActions(), board.getTerritories());
    }

    /**
     * Get a risk action to reinforce the territory deemed best by the algorithm
     * This method allows choosing the possible actions and territories externally, so the player can restrict the
     * heuristic themselves.
     * Note that the actions and territories have to be reduced in tandem for the algorithm to work as intended.
     *
     * @param playerNumber    The acting player
     * @param board           The risk board (separate so it can be cached)
     * @param possibleActions The actions which could be taken (separate so they can be reduced externally)
     * @param territories     All the territories of the game (separate so they can be reduced externally)
     * @return A risk action for reinforcing the territory deemed most advantageous
     */
    public static RiskAction reinforce(int playerNumber, RiskBoard board, Set<RiskAction> possibleActions, Map<Integer, RiskTerritory> territories) {
        var playerUnitsOnContinent = territories
                .values()
                .stream()
                .filter(riskTerritory -> riskTerritory.getOccupantPlayerId() == playerNumber)
                .collect(Collectors.groupingBy(RiskTerritory::getContinentId,
                        Collectors.summingInt(RiskTerritory::getTroops)));

        var totalUnitsOnContinent = territories
                .values()
                .stream()
                .filter(continent -> playerUnitsOnContinent.containsKey(continent.getContinentId()))
                .collect(Collectors.groupingBy(RiskTerritory::getContinentId,
                        Collectors.summingInt(RiskTerritory::getTroops)));


        var continentEstimatedBest = Stream
                .concat(playerUnitsOnContinent.entrySet().stream(),
                        totalUnitsOnContinent.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey,
                        playerTroopsOnContinent -> (double) playerTroopsOnContinent.getValue(),
                        (playerTroopsOnContinent, allTroopsOnContinent) -> playerTroopsOnContinent / allTroopsOnContinent))
                .entrySet()
                .stream()
                .filter(continent -> continent.getValue() < 0.99)
                .map(integerDoubleEntry -> new Pair<>(integerDoubleEntry.getKey(),
                        Math.abs(TROOPS_RELATION_CONTINENT_THRESHOLD - integerDoubleEntry.getValue())))
                .min(Comparator.comparingDouble(Pair::getValue1))
                .map(Pair::getValue0);

        return territories
                .entrySet()
                .stream()
                //If there is a clean cut between continent ownerships, we do not find a preferred continent, so we have to extend the search to the full board
                .filter(integerRiskTerritoryEntry -> continentEstimatedBest.map(continentId -> integerRiskTerritoryEntry.getValue().getContinentId() == continentId).orElse(true))
                .filter(riskTerritory -> possibleActions.stream().anyMatch(action -> action.reinforcedId() == riskTerritory.getKey()))
                .filter(riskTerritory -> board.neighboringEnemyTerritories(riskTerritory.getKey()).size() > 0)
                .map(riskTerritory -> board.neighboringEnemyTerritories(riskTerritory.getKey())
                        .stream()
                        .map(board::getTerritoryTroops)
                        .reduce(Integer::sum)
                        .map(Integer::doubleValue)
                        .map(noEnemyTroops -> new Pair<>(riskTerritory.getKey(),
                                riskTerritory.getValue().getTroops()
                                        / (noEnemyTroops + riskTerritory.getValue().getTroops())))
                        .orElseThrow())
                .map(riskTerritory -> new Pair<>(riskTerritory.getValue0(),
                        Math.abs(TROOPS_RELATION_TERRITORIES_THRESHOLD - riskTerritory.getValue1())))
                .min(Comparator.comparingDouble(Pair::getValue1))
                .map(territory -> RiskAction.reinforce(territory.getValue0(),
                        getTroopsToReinforce(territory.getValue0(), possibleActions)))
                .orElseGet(() -> {
                    logger.info("Could not get reinforcement through conventional heuristic");
                    return Util.selectRandom(possibleActions);
                });

    }

    private static int getTroopsToReinforce(Integer key, Set<RiskAction> possibleActions) {
        return possibleActions
                .stream()
                .filter(riskAction -> riskAction.reinforcedId() == key)
                .map(RiskAction::troops)
                .max(Integer::compareTo)
                .orElseThrow();
    }
}
