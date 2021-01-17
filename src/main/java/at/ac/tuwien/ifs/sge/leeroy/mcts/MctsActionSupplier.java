package at.ac.tuwien.ifs.sge.leeroy.mcts;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;

/**
 * The class implementing the basic MCTS skeleton for the MCTS during the main game turns (reinforce, attack, occupy)
 */
public abstract class MctsActionSupplier {

    protected ActionNode rootNode;
    protected BooleanSupplier shouldStopComputation;

    private static final Logger logger = Logger.getLogger(MctsActionSupplier.class.getName());

    /**
     * Create a new MctsActionSupplier
     *
     * @param shouldStopComputation A BooleanSupplier signaling when the MCTS should stop
     */
    public MctsActionSupplier(BooleanSupplier shouldStopComputation) {
        this.shouldStopComputation = shouldStopComputation;
    }

    /**
     * Get the best successor of the node
     *
     * @param actionNode The node to find the best successor for
     * @return The best successor of the node
     */
    abstract ActionNode getBestAttackSuccessorNode(ActionNode actionNode);

    /**
     * Evaluate an ActionNode
     *
     * @param actionNode The node to evaluate
     * @return The evaluation score
     */
    abstract Integer evaluate(ActionNode actionNode);

    /**
     * Simulate the game starting from the passed node
     *
     * @param explorationNode The node to start the simulation from
     * @return The evaluated score of the simulated steps.
     */
    abstract int simulateGame(ActionNode explorationNode);

    /**
     * Get the successors of a node
     *
     * @param selectedNode The node to get the successors for
     * @return The successors of the node
     */
    abstract List<ActionNode> getSuccessors(ActionNode selectedNode);

    /**
     * Find the best next action via MCTS
     *
     * @return The action deemed best by the MCTS
     */
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

    /**
     * The UCT value of a node
     *
     * @param parentVisited The amount of times the node's parent has been visited
     * @param node          The node to get the UCT value for
     * @return The nodes UCT value
     */
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

    /**
     * Set the root node for the MCTS (can e.g. be used for caching and retrieving the node in between turns)
     *
     * @param rootNode The new root node to use
     */
    public void setRootNode(ActionNode rootNode) {
        this.rootNode = rootNode;
    }
}
