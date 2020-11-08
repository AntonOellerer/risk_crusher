package agents.leeroy;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GameUtils {
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

    public static Function<Node, Integer> partialEvaluationFunction(Risk risk) {
        return (node) -> evaluateInitialBoard(node, risk);
    }

    private static Integer evaluateInitialBoard(Node node, Risk risk) {
        if (node.getSuccessors().isEmpty()) {
            int playerId = risk.getCurrentPlayer();
            RiskBoard riskBoard = risk.getBoard();
            Set<Integer> territoriesOccupiedByPlayer = riskBoard.getTerritoriesOccupiedByPlayer(playerId);
            Set<Set<Integer>> areas = getAreas(territoriesOccupiedByPlayer, riskBoard);
            int totalNumberOfNeighbors = getNeighbors(territoriesOccupiedByPlayer, riskBoard).size();
            int numberOfContinentsOccupied = getContinentsOccupied(territoriesOccupiedByPlayer, riskBoard).size();
            return -1 * areas.size() * totalNumberOfNeighbors * numberOfContinentsOccupied;
        } else {
            throw new NotImplementedException();
        }
    }

    private static Set<Set<Integer>> getAreas(Set<Integer> territoriesOccupiedByPlayer, RiskBoard board) {
        Set<Set<Integer>> areas = new HashSet<>();
        Set<Integer> visitedNodes = new HashSet<>();
        for (Integer territory : territoriesOccupiedByPlayer) {
            if (!visitedNodes.contains(territory)) {
                Set<Integer> area = new HashSet<>();
                area.add(territory);
                List<Integer> neighbours = new ArrayList<>(board.neighboringTerritories(territory));
                for (Integer neighbour : neighbours) {
                    if (territoriesOccupiedByPlayer.contains(neighbour)) {
                        area.add(neighbour);
                        neighbours.addAll(board.neighboringTerritories(neighbour));
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
}
