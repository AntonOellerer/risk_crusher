package agents.leeroy;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GameUtils {
    private static final Logger logger = Logger.getLogger(GameUtils.class.getName());

    public static int getNumberOfStartingTroops(int players) {
        switch (players) {
            case 2:
                return 50;
            case 3:
                return 35;
            case 4:
                return 30;
            case 5:
                return 25;
            case 6:
                return 20;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static List<Map.Entry<Integer, RiskTerritory>> getUnoccupiedEntries(Risk risk) {
        return risk
                .getBoard()
                .getTerritories()
                .entrySet()
                .stream()
                .filter(integerRiskTerritoryEntry -> integerRiskTerritoryEntry.getValue().getOccupantPlayerId() == -1)
                .collect(Collectors.toList());
    }

    public static List<Map.Entry<Integer, RiskTerritory>> getOccupiedEntries(Risk risk) {
        return risk
                .getBoard()
                .getTerritories()
                .entrySet()
                .stream()
                .filter(integerRiskTerritoryEntry -> integerRiskTerritoryEntry.getValue().getOccupantPlayerId() != -1)
                .collect(Collectors.toList());
    }

    public static Function<Node, Integer> partialInitialEvaluationFunction(Risk risk) {
        return (node) -> evaluateInitialBoard(node, risk);
    }

    private static Integer evaluateInitialBoard(Node node, Risk risk) {
        if (node.getSuccessors().isEmpty()) {
            int playerId = risk.getCurrentPlayer();
            RiskBoard riskBoard = risk.getBoard();
            Set<Integer> territoriesOccupiedByPlayer = getTerritoriesOccupiedByPlayer(playerId, ((InitialPlacementNode) node).getOccupiedTerritories());
            logger.warning("Got occupied territories");
            Set<Set<Integer>> areas = getAreas(territoriesOccupiedByPlayer, riskBoard);
            logger.warning("Got areas");
            int totalNumberOfNeighbors = getNeighbors(territoriesOccupiedByPlayer, riskBoard).size();
            logger.warning("Got neighbors");
            int numberOfContinentsOccupied = getContinentsOccupied(territoriesOccupiedByPlayer, riskBoard).size();
            logger.warning("Got continents");
            return -1 * areas.size() * totalNumberOfNeighbors * numberOfContinentsOccupied;
        } else {
            throw new NotImplementedException();
        }
    }

    private static Set<Integer> getTerritoriesOccupiedByPlayer(int playerId, List<Map.Entry<Integer, RiskTerritory>> territories) {
        return territories
                .stream()
                .filter(integerRiskTerritoryEntry -> integerRiskTerritoryEntry.getValue().getOccupantPlayerId() == playerId)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private static Set<Set<Integer>> getAreas(Set<Integer> territoriesOccupiedByPlayer, RiskBoard board) {
        Set<Set<Integer>> areas = new HashSet<>();
        Set<Integer> visitedNodes = new HashSet<>();
        for (Integer territory : territoriesOccupiedByPlayer) {
            if (!visitedNodes.contains(territory)) {
                Set<Integer> area = new HashSet<>();
                area.add(territory);
                Queue<Integer> neighbours = new LinkedList<>(board.neighboringTerritories(territory));
                Set<Integer> visitedNeighbors = new HashSet<>();
                while (!neighbours.isEmpty()) {
                    var neighbour = neighbours.remove();
                    if (!visitedNeighbors.contains(neighbour)) {
                        if (territoriesOccupiedByPlayer.contains(neighbour)) {
                            area.add(neighbour);
                            neighbours.addAll(board.neighboringTerritories(neighbour));
                        }
                        visitedNeighbors.add(neighbour);
                    }
                }
                areas.add(area);
                visitedNodes.add(territory);
            }
        }
        return areas;
    }

    private static Set<Integer> getNeighbors(Set<Integer> territoriesOccupiedByPlayer, RiskBoard riskBoard) {
        Set<Integer> neighbors = new HashSet<>();
        for (Integer territory : territoriesOccupiedByPlayer) {
            neighbors.addAll(riskBoard.neighboringTerritories(territory));
        }
        return neighbors;
    }

    private static Set<Integer> getContinentsOccupied(Set<Integer> territoriesOccupiedByPlayer, RiskBoard riskBoard) {
        Set<Integer> continents = new HashSet<>();
        for (Integer territory : territoriesOccupiedByPlayer) {
            continents.add(riskBoard.getTerritories().get(territory).getContinentId());
        }
        return continents;
    }

    public static Function<Node, Node> partialInitialExpansionFunction(Risk game) {
        return (node) -> initialExpansionFunction(node, game);
    }

    private static Node initialExpansionFunction(Node node, Risk game) {
        var board = game.getBoard();
        return Collections.max(node.getSuccessors(),
                Comparator.comparingInt(nodeToEvaluate ->
                        getAreas(board.getTerritoriesOccupiedByPlayer(nodeToEvaluate.getPlayer()), board).size()));
    }
}
