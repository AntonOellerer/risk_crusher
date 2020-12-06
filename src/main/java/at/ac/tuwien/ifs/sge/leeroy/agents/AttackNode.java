package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class AttackNode implements Node {

    private final int player;
    private final Node parent;
    private final Risk game;
    private List<Node> successors = null;
    private double winScore;
    private int visitCount = 0;

    @Override
    public List<Node> getSuccessors() {
        if (successors == null) {
            successors = AttackActionSupplier
                    .createActions(game)
                    .stream()
                    .map(ra -> new AttackNode(player, this, (Risk) game.doAction(ra)))
                    .collect(Collectors.toList());
        }

        return successors;
    }

    @Override
    public boolean isLeafNode() {
        return getSuccessors().isEmpty();
    }

    @Override
    public int getId() {
        return 0;
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
