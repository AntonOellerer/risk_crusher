package at.ac.tuwien.ifs.sge.leeroy.mcts;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;

public abstract class MctsActionSupplier {

    protected ActionNode rootNode;
    protected BooleanSupplier shouldStopComputation;

    private static final Logger logger = Logger.getLogger(MctsActionSupplier.class.getName());

    public MctsActionSupplier(BooleanSupplier shouldStopComputation) {
        this.shouldStopComputation = shouldStopComputation;
    }

    abstract ActionNode getBestAttackSuccessorNode(ActionNode actionNode);

    abstract Integer evaluate(ActionNode actionNode);

    abstract int simulateGame(ActionNode explorationNode);

    abstract List<ActionNode> getSuccessors(ActionNode selectedNode);

    public ActionNode findBestNode() {
        performMcts();
        if (rootNode.getSuccessors() == null) {
            // stopped before execution (very slow pc or very low time)
            logger.warning("No successors found - slow execution?");
            return null;
        }
        return rootNode.getSuccessors().stream().max(Comparator.comparingDouble(ActionNode::getWinScore)).orElse(null);
    }

    private void performMcts() {
        while (!this.shouldStopComputation.getAsBoolean()) {
            var selectedNode = select();
            var successors = getSuccessors(selectedNode); //expand
            if (successors.isEmpty()) {
                backpropagate(rootNode.getPlayer(), selectedNode, evaluate(selectedNode));
            } else {
                var explorationNode = getBestAttackSuccessorNode(selectedNode);
                getSuccessors(explorationNode);
                int playOutResult = simulateGame(explorationNode);
                backpropagate(rootNode.getPlayer(), explorationNode, playOutResult);
            }
        }
    }

    private ActionNode select() {
        ActionNode bestNode = rootNode;
        while (bestNode.isExpanded() && !bestNode.getSuccessors().isEmpty()) {
            bestNode = findBestSuccessor(bestNode);
        }
        return bestNode;
    }

    private ActionNode findBestSuccessor(ActionNode node) {
        var visited = node.getVisitCount();
        return Collections.max(node.getSuccessors(), Comparator.comparingDouble(nodeA -> getUCTValue(visited, nodeA)));
    }

    protected double getUCTValue(int parentVisited, ActionNode node) {
        if (node.getVisitCount() == 0) {
            return Integer.MAX_VALUE;
        } else {
            return (node.getWinScore() / node.getVisitCount()) + 1.414 * Math.sqrt(Math.log(parentVisited) / node.getVisitCount());
        }
    }

    private void backpropagate(int player, ActionNode explorationNode, int playOutResult) {
        var toUpdate = Optional.of(explorationNode);
        while (toUpdate.isPresent()) {
            var nodeToUpdate = toUpdate.get();
            nodeToUpdate.incrementVisitCount();
            if (nodeToUpdate.getPlayer() == player) {
                nodeToUpdate.incrementWinScore(playOutResult);
            }
            toUpdate = nodeToUpdate.getParent();
        }
    }

    public ActionNode getRootNode() {
        return rootNode;
    }

    public void setRootNode(ActionNode rootNode) {
        this.rootNode = rootNode;
    }
}
