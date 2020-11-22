package agents.leeroy;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;
import at.ac.tuwien.ifs.sge.util.Util;
import org.javatuples.Pair;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HeuristicReinforce {
    private static final double TROOPS_RELATION_TERRITORIES_THRESHOLD = 0.8;
    private static final double TROOPS_RELATION_CONTINENT_THRESHOLD = 0.9;

    private static final Logger logger = Logger.getLogger(HeuristicReinforce.class.getName());


    public static RiskAction reinforce(int playerNumber, Risk game) {
        var board = game.getBoard();
        var possibleActions = game.getPossibleActions();

        var playerUnitsOnContinent = board
                .getTerritories()
                .values()
                .stream()
                .filter(riskTerritory -> riskTerritory.getOccupantPlayerId() == playerNumber)
                .collect(Collectors.groupingBy(RiskTerritory::getContinentId,
                        Collectors.summingInt(RiskTerritory::getTroops)));

        var totalUnitsOnContinent = board
                .getTerritories()
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

        return board
                .getTerritories()
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
