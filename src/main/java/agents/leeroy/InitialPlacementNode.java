package agents.leeroy;

import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class InitialPlacementNode implements Node {
    private final int id;
    private final List<Map.Entry<Integer, RiskTerritory>> occupiedTerritories;
    private final List<Map.Entry<Integer, RiskTerritory>> unoccupiedTerritories;

    @Override
    public List<Node> getSuccessors() {
        List<Node> successors = new LinkedList<>();
        for (Map.Entry<Integer, RiskTerritory> entry : unoccupiedTerritories) {
            var newOccupied = new ArrayList<>(occupiedTerritories);
            newOccupied.add(entry);
            var newUnoccupied = new ArrayList<>(unoccupiedTerritories);
            newUnoccupied.remove(entry);
            successors.add(new InitialPlacementNode(entry.getKey(), newOccupied, newUnoccupied));
        }
        return successors;
    }

    @Override
    public boolean isLeafNode() {
        return unoccupiedTerritories.isEmpty();
    }
}
