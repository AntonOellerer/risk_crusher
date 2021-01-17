package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A class containing utility functions for dealing w/ the game
 */
public class GameUtils {
    private static Map<Integer, List<Integer>> continentTerritories;

    /**
     * Return all territories on the board which are not occupied
     *
     * @param board The board to check
     * @return All unoccupied territories in a map (territoryId, territory)
     */
    public static List<Map.Entry<Integer, RiskTerritory>> getUnoccupiedEntries(RiskBoard board) {
        return board
                .getTerritories()
                .entrySet()
                .stream()
                .filter(integerRiskTerritoryEntry -> integerRiskTerritoryEntry.getValue().getOccupantPlayerId() == -1)
                .collect(Collectors.toList());
    }

    /**
     * Return all territories on the board which are occupied
     *
     * @param board The board to check
     * @return All occupied territories in a map (territoryId, territory)
     */
    public static List<Map.Entry<Integer, RiskTerritory>> getOccupiedEntries(RiskBoard board) {
        return board
                .getTerritories()
                .entrySet()
                .stream()
                .filter(integerRiskTerritoryEntry -> integerRiskTerritoryEntry.getValue().getOccupantPlayerId() != -1)
                .collect(Collectors.toList());
    }

    /**
     * Generating function for a function evaluating a board still in the initial placement phase
     * It can be used to generate the evaluation function near the top of the call tree, when the game and the board are
     * known, which is then passed down, and called for singular nodes later on.
     *
     * @param risk  The risk game (Still in the initial placement phase)
     * @param board The board (passed separately so it can be cached)
     * @return A function taking a node, returning an evaluation score
     */
    public static Function<Node, Integer> partialInitialEvaluationFunction(Risk risk, RiskBoard board) {
        return (node) -> evaluateInitialBoard(node, risk, board);
    }

    private static Integer evaluateInitialBoard(Node node, Risk risk, RiskBoard board) {
        if (node.getSuccessors().isEmpty()) {
            int playerId = risk.getCurrentPlayer();
            Set<Integer> territoriesOccupiedByPlayer = getTerritoriesOccupiedByPlayer(playerId, ((InitialPlacementNode) node).getOccupiedTerritories());
            Set<Set<Integer>> areas = getAreas(territoriesOccupiedByPlayer, board);
            int totalNumberOfNeighbors = getNeighbors(territoriesOccupiedByPlayer, board).size();
            int numberOfContinentsOccupied = getContinentsOccupied(territoriesOccupiedByPlayer, board).size();
            int totalContinentBonus = getTotalContinentBonus(playerId, board);
            int totalContinentMalus = getTotalContinentMalus(playerId, board);
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

    /**
     * A function returning the territories on the board which are occupied by enemies and
     * neighbour to a territory belonging to the player
     *
     * @param territoriesOccupiedByPlayer A set of ids of territories occupied by the player
     * @param riskBoard                   The risk board
     * @return A set of ids of territories occupied by the enemy and neighbour to player's territories.
     */
    public static Set<Integer> getEnemyNeighbors(Set<Integer> territoriesOccupiedByPlayer, RiskBoard riskBoard) {
        Set<Integer> neighbors = new HashSet<>();
        for (Integer territory : territoriesOccupiedByPlayer) {
            neighbors.addAll(riskBoard.neighboringEnemyTerritories(territory));
        }
        return neighbors;
    }

    /**
     * A function to get the number of troops belonging to the player which are not in territories adjacent to
     * enemy territories
     *
     * @param territoriesOccupiedByPlayer A set of ids of territories occupied by the player
     * @param riskBoard                   The risk board
     * @return The amount of troops belonging to the player which are not in territories adjacent to enemy territories
     */
    public static int getUnusedTroops(Set<Integer> territoriesOccupiedByPlayer, RiskBoard riskBoard) {
        int unusedTroopSum = 0;
        for (Integer territory : territoriesOccupiedByPlayer) {
            if (riskBoard.neighboringEnemyTerritories(territory).isEmpty()) {
                unusedTroopSum += riskBoard.getMobileTroops(territory);
            }
        }
        return unusedTroopSum;
    }

    /**
     * Retrieve the difference between the players troops on the frontline and enemy troops on the frontline.
     * If it is positive, it means that the player has more troops on the frontline, if it is negative the enemy
     * has more troops on the frontline.
     *
     * @param territoriesOccupiedByPlayer A set of ids of the territories occupied by the player
     * @param riskBoard                   The risk board
     * @return The difference of players troop on the frontline and enemy troops on the frontline.
     */
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

    /**
     * Get the total troops the players enemies will receive for owning a complete continent.
     *
     * @param playerId  The id of the player
     * @param riskBoard The risk board
     * @return How many troops the players enemy will receive as continent bonuses
     */
    public static int getTotalContinentMalus(int playerId, RiskBoard riskBoard) {
        int totalMalus = 0;
        for (int i = 0; i < riskBoard.getNumberOfPlayers(); i++) {
            if (i != playerId) {
                totalMalus += getContinentBonusForPlayer(i, riskBoard);
            }
        }
        return totalMalus;
    }

    /**
     * Get the total amount of troops the player will receive for occupying full continents.
     *
     * @param player    The id of the player
     * @param riskBoard The risk board
     * @return How many troops the players will receive as continent bonus
     */
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

    /**
     * Generating function for a function selecting the best successor node of a supplied node based on the number of
     * areas the player will occupy if performing this node.
     * An area is defined as a set of node where each node can be reached from every other node without having to enter
     * enemy territory.
     *
     * @param riskBoard The risk game (Still in the initial placement phase)
     * @return A function taking a node, returning the estimated best successor node (the one creating the fewest areas)
     */
    public static Function<Node, Node> partialInitialExpansionFunction(RiskBoard riskBoard) {
        return (node) -> initialExpansionFunction(node, riskBoard);
    }

    private static Node initialExpansionFunction(Node node, RiskBoard riskBoard) {
        return Collections.min(node.getSuccessors(),
                Comparator.comparingInt(nodeToEvaluate ->
                        getAreas(riskBoard.getTerritoriesOccupiedByPlayer(nodeToEvaluate.getPlayer()), riskBoard).size()));
    }

    /**
     * Check if an action is a reinforcement action
     *
     * @param riskAction The action to check
     * @return Whether it is a reinforcement action.
     */
    public static boolean isReinforcementAction(RiskAction riskAction) {
        return riskAction.attackingId() == -1 && riskAction.reinforcedId() >= 0 && riskAction.troops() > 0;
    }
}
