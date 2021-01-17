package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;

/**
 * A node containing the information for the "initial placement" MCTS
 */
@Getter
@RequiredArgsConstructor
public class InitialPlacementNode implements Node {
    private final int player;
    private final int id;
    private final Node parent;
    private final List<Map.Entry<Integer, RiskTerritory>> occupiedTerritories;
    private final List<Map.Entry<Integer, RiskTerritory>> unoccupiedTerritories;
    private List<Node> successors = null;
    private double winScore;
    private int visitCount = 0;

    /**
     * @return The possible successor nodes occupying unoccupied territory.
     */
    @Override
    public List<Node> getSuccessors() {
        if (successors != null) {
            return successors;
        }
        successors = new ArrayList<>();
        //should be parameterizable by player size
        for (Map.Entry<Integer, RiskTerritory> entry : unoccupiedTerritories) {
            var newOccupied = new ArrayList<>(occupiedTerritories);
            var newTerritory = new RiskTerritory(entry.getValue());
            newTerritory.setOccupantPlayerId((player + 1) % 2);
            newOccupied.add(new AbstractMap.SimpleEntry<>(entry.getKey(), newTerritory));
            var newUnoccupied = new ArrayList<>(unoccupiedTerritories);
            newUnoccupied.remove(entry);
            successors.add(new InitialPlacementNode((player + 1) % 2, entry.getKey(), this, newOccupied, newUnoccupied));
        }
        return successors;
    }

    /**
     * @return Whether this node is a leaf node (all territories have been occupied)
     */
    @Override
    public boolean isLeafNode() {
        return unoccupiedTerritories.isEmpty();
    }

    /**
     * @return Get the parent of the node, empty if it is a root node
     */
    @Override
    public Optional<Node> getParent() {
        if (parent == null) {
            return Optional.empty();
        }
        return Optional.of(this.parent);
    }


    /**
     * Generate the amount of times this node has been visited.
     */
    @Override
    public void incrementVisitCount() {
        visitCount += 1;
    }

    /**
     * @param increment Increment the win score of the node
     */
    @Override
    public void incrementWinScore(double increment) {
        winScore += increment;
    }

    /**
     * @return Whether this node has already been expanded
     */
    @Override
    public boolean isExpanded() {
        return successors != null;
    }
}
