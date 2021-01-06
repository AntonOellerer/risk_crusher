package at.ac.tuwien.ifs.sge.leeroy.mcts;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public class ActionNode {

    private final int player;
    private final ActionNode parent;
    private final Risk game;
    private final RiskAction action;
    private List<ActionNode> successors = null;
    private double winScore;
    private int visitCount = 0;

    public List<ActionNode> getSuccessors() {
        return successors;
    }

    public void setSuccessors(List<ActionNode> actionNodes) {
        this.successors = actionNodes;
    }

    public boolean isLeafNode() {
        return action != null && action.isEndPhase();
    }

    public int getId() {
        return 0;
    }

    public Optional<ActionNode> getParent() {
        if (parent == null) {
            return Optional.empty();
        }
        return Optional.of(this.parent);
    }

    public void incrementVisitCount() {
        visitCount += 1;
    }

    public void incrementWinScore(double increment) {
        winScore += increment;
    }

    public boolean isExpanded() {
        return successors != null;
    }
}