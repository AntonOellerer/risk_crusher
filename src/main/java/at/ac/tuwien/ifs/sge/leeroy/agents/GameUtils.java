package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GameUtils {
    private static final Logger logger = Logger.getLogger(GameUtils.class.getName());
    private static Map<Integer, List<Integer>> continentTerritories;

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
            Set<Set<Integer>> areas = getAreas(territoriesOccupiedByPlayer, riskBoard);
            int totalNumberOfNeighbors = getNeighbors(territoriesOccupiedByPlayer, riskBoard).size();
            int numberOfContinentsOccupied = getContinentsOccupied(territoriesOccupiedByPlayer, riskBoard).size();
            int totalContinentBonus = getTotalContinentBonus(playerId, riskBoard);
            int totalContinentMalus = getTotalContinentMalus(playerId, riskBoard);
            return -1 * areas.size()
                    * totalNumberOfNeighbors
                    * numberOfContinentsOccupied
                    / (totalContinentBonus + 1)
                    * totalContinentMalus;
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

    public static Set<Integer> getEnemyNeighbors(Set<Integer> territoriesOccupiedByPlayer, RiskBoard riskBoard) {
        Set<Integer> neighbors = new HashSet<>();
        for (Integer territory : territoriesOccupiedByPlayer) {
            neighbors.addAll(riskBoard.neighboringEnemyTerritories(territory));
        }
        return neighbors;
    }

    public static int getUnusedTroops(Set<Integer> territoriesOccupiedByPlayer, RiskBoard riskBoard) {
        int unusedTroopSum = 0;
        for (Integer territory : territoriesOccupiedByPlayer) {
            if (riskBoard.neighboringEnemyTerritories(territory).isEmpty()) {
                unusedTroopSum += riskBoard.getMobileTroops(territory);
            }
        }
        return unusedTroopSum;
    }

    public static int getFrontlineMargin(Set<Integer> territoriesOccupiedByPlayer, RiskBoard riskBoard) {
        Set<Integer> neighbors = new HashSet<>();
        int margin = 0;
        for (Integer territory : territoriesOccupiedByPlayer) {
            Set<Integer> territoryNeighbors = riskBoard.neighboringEnemyTerritories(territory);
            if (territoryNeighbors.isEmpty()) {
                continue;
            }
            margin += riskBoard.getTerritoryTroops(territory); // add troops of player's territory
            for (Integer enemyTerritory : territoryNeighbors) {
                if (neighbors.contains(enemyTerritory)) {
                    continue;
                }
                margin -= riskBoard.getTerritoryTroops(enemyTerritory); // subtract troops of new enemy's territory
                neighbors.add(enemyTerritory);
            }
        }
        return margin;
    }

    private static Set<Integer> getContinentsOccupied(Set<Integer> territoriesOccupiedByPlayer, RiskBoard riskBoard) {
        Set<Integer> continents = new HashSet<>();
        for (Integer territory : territoriesOccupiedByPlayer) {
            continents.add(riskBoard.getTerritories().get(territory).getContinentId());
        }
        return continents;
    }

    private static int getTotalContinentBonus(int playerId, RiskBoard riskBoard) {
        return getContinentBonusForPlayer(playerId, riskBoard);
    }

    public static int getTotalContinentMalus(int playerId, RiskBoard riskBoard) {
        int totalMalus = 0;
        for (int i = 0; i < riskBoard.getNumberOfPlayers(); i++) {
            if (i != playerId) {
                totalMalus += getContinentBonusForPlayer(i, riskBoard);
            }
        }
        return totalMalus;
    }

    public static int getContinentBonusForPlayer(int player, RiskBoard riskBoard) {
        if (continentTerritories == null) {
            continentTerritories = getContinentTerritories(riskBoard);
        }
        var territoriesOccupiedByPlayer = riskBoard.getTerritoriesOccupiedByPlayer(player);
        return continentTerritories.entrySet()
                .stream()
                .filter(continentTerritories -> territoriesOccupiedByPlayer.containsAll(continentTerritories.getValue()))
                .map(continentTerritories -> riskBoard.getContinentBonus(continentTerritories.getKey()))
                .reduce(Integer::sum)
                .orElse(0);
    }

    private static Map<Integer, List<Integer>> getContinentTerritories(RiskBoard riskBoard) {
        return riskBoard.getTerritories()
                .entrySet()
                .stream()
                .collect(Collectors.groupingBy(pair -> pair.getValue().getContinentId(),
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
    }

    public static Function<Node, Node> partialInitialExpansionFunction(Risk game) {
        return (node) -> initialExpansionFunction(node, game);
    }

    private static Node initialExpansionFunction(Node node, Risk game) {
        var board = game.getBoard();
        return Collections.min(node.getSuccessors(),
                Comparator.comparingInt(nodeToEvaluate ->
                        getAreas(board.getTerritoriesOccupiedByPlayer(nodeToEvaluate.getPlayer()), board).size()));
    }

    public static boolean isReinforcementAction(RiskAction riskAction) {
        return riskAction.attackingId() == -1 && riskAction.reinforcedId() >= 0 && riskAction.troops() > 0;
    }
}
