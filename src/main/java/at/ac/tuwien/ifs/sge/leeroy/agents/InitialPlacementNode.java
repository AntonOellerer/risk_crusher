package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;

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

    @Override
    public List<Node> getSuccessors() {
        if (successors != null) {
            return successors;
        }
        successors = new ArrayList<>();
        //TODO: find way to parameterize this by player size
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

    @Override
    public boolean isLeafNode() {
        return unoccupiedTerritories.isEmpty();
    }

    @Override
    public Optional<Node> getParent() {
        if (parent == null) {
            return Optional.empty();
        }
        return Optional.of(this.parent);
    }


    @Override
    public void incrementVisitCount() {
        visitCount += 1;
    }

    @Override
    public void incrementWinScore(double increment) {
        winScore += increment;
    }

    @Override
    public boolean isExpanded() {
        return successors != null;
    }
}
