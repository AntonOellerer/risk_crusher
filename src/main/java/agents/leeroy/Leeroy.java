package agents.leeroy;

import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import phase.Phase;
import phase.PhaseUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class Leeroy<G extends Game<A, RiskBoard>, A> extends AbstractGameAgent<G, A> implements GameAgent<G, A> {
    Phase currentPhase = Phase.INITIAL_SELECT;
    Node initialPlacementRoot;
    private int playerNumber;
    private int numberOfPlayers;

    public Leeroy(Logger log) {
        super(log);
        log.warn("Instantiated");
    }

    @Override
    public A computeNextAction(G game, long computationTime, TimeUnit timeUnit) {
        super.setTimers(computationTime, timeUnit);
        log.warn("Computing action");
        Risk risk = (Risk) game;
        setPhase(risk);
        log.warn("Set phase");
        if (currentPhase == Phase.INITIAL_SELECT) {
            log.warn("Placing initial selection");
            return (A) selectInitialCountry(risk);
        }
        return (A) risk.determineNextAction();
    }

    private void setPhase(Risk game) {
        if (currentPhase == Phase.INITIAL_SELECT
                && PhaseUtils.stillUnoccupiedTerritories(game.getBoard())) {
            if (this.initialPlacementRoot == null) {
                this.initialPlacementRoot = new InitialPlacementNode(game.getCurrentPlayer(),
                        -1,
                        null,
                        GameUtils.getOccupiedEntries(game),
                        GameUtils.getUnoccupiedEntries(game));
            } else {
                setNewInitialPlacementRoot(game);
            }
        } else if (currentPhase == Phase.INITIAL_SELECT) {
            currentPhase = Phase.INITIAL_REINFORCE;
        } else if (currentPhase == Phase.INITIAL_REINFORCE
                && PhaseUtils.initialPlacementFinished(game.getBoard(), playerNumber, GameUtils.getNumberOfStartingTroops(numberOfPlayers))) {
            currentPhase = Phase.REINFORCE;
        } else if (currentPhase == Phase.REINFORCE && !PhaseUtils.inReinforcing(game)) {
            currentPhase = Phase.ATTACK;
        }
    }

    private RiskAction selectInitialCountry(Risk game) {
        performMCTS(initialPlacementRoot, GameUtils.partialInitialExpansionFunction(game), GameUtils.partialInitialEvaluationFunction(game));
        var bestNode = initialPlacementRoot.getSuccessors().stream().max(Comparator.comparingDouble(Node::getWinScore)).get();
        //Graph moved one node forward after the action
        initialPlacementRoot = bestNode;
        return RiskAction.select(bestNode.getId());
    }

    private void setNewInitialPlacementRoot(Risk game) {
        initialPlacementRoot = initialPlacementRoot
                .getSuccessors()
                .stream()
                .filter(node -> ((InitialPlacementNode) node).getOccupiedTerritories().equals(GameUtils.getOccupiedEntries(game)))
                .filter(node -> ((InitialPlacementNode) node).getUnoccupiedTerritories().equals(GameUtils.getUnoccupiedEntries(game)))
                .findFirst()
                .get();
    }

    private void performMCTS(Node node, Function<Node, Node> nodeSelectionFunction, Function<Node, Integer> evaluationFunction) {
        while (!this.shouldStopComputation()) {
            log.warn("Starting one iteration");
            var selectedNode = select(node);
            log.warn("Selected node");
            var successors = selectedNode.getSuccessors(); //expand
            log.warn("Selected successors");
            if (successors.isEmpty()) {
                backpropagate(node.getPlayer(), selectedNode, evaluationFunction.apply(selectedNode));
            } else {
                var explorationNode = nodeSelectionFunction.apply(selectedNode);
                log.warn("Explored node");
                int playOutResult = playOutGame(explorationNode, nodeSelectionFunction, evaluationFunction);
                log.warn("Played out results");
                backpropagate(node.getPlayer(), explorationNode, playOutResult);
                log.warn("Backpropagated");
            }
        }
    }

    private Node select(Node rootNode) {
        Node bestNode = rootNode;
        while (bestNode.isExpanded() && !bestNode.getSuccessors().isEmpty()) {
            bestNode = findBestSuccessor(bestNode);
        }
        return bestNode;
    }

    private Node findBestSuccessor(Node node) {
        var visited = node.getVisitCount();
        return Collections.max(node.getSuccessors(), Comparator.comparingDouble(nodeA -> getUCTValue(visited, nodeA)));
    }

    private double getUCTValue(int parentVisited, Node node) {
        if (node.getVisitCount() == 0) {
            return Integer.MAX_VALUE;
        } else {
            return (node.getWinScore() / node.getVisitCount()) + 1.414 * Math.sqrt(Math.log(parentVisited) / node.getVisitCount());
        }
    }

    private int playOutGame(Node explorationNode, Function<Node, Node> nodeSelectionFunction, Function<Node, Integer> evaluationFunction) {
        var currentNode = explorationNode;
        while (!currentNode.getSuccessors().isEmpty()) {
            currentNode = nodeSelectionFunction.apply(currentNode);
        }
        log.warn("Simulated to the bottom");
        return evaluationFunction.apply(currentNode);
    }

    private void backpropagate(int player, Node explorationNode, int playOutResult) {
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


    private Integer performAlphaBetaSearch(Node node,
                                           boolean isMaximizingPlayer,
                                           int alpha,
                                           int beta,
                                           Function<Node, Integer> evaluationFunction) {
        if (node.isLeafNode()) {
            return evaluationFunction.apply(node);
        }
        if (isMaximizingPlayer) {
            int bestValue = Integer.MIN_VALUE;
            for (Node successorNode : node.getSuccessors()) {
                bestValue = Math.max(bestValue, performAlphaBetaSearch(successorNode, false, alpha, beta, evaluationFunction));
                alpha = Math.max(alpha, bestValue);
                if (alpha >= beta) {
                    break;
                }
            }
            return bestValue;
        } else {
            int bestValue = Integer.MAX_VALUE;
            for (Node successorNode : node.getSuccessors()) {
                bestValue = Math.min(bestValue, performAlphaBetaSearch(successorNode, true, alpha, beta, evaluationFunction));
                beta = Math.min(beta, bestValue);
                if (beta <= alpha) {
                    break;
                }
            }
            return bestValue;
        }
    }

    @Override
    public void setUp(int numberOfPlayers, int playerNumber) {
        super.setUp(numberOfPlayers, playerNumber);
        this.playerNumber = playerNumber;
        this.numberOfPlayers = numberOfPlayers;
        log.warn("Set up");
    }

    @Override
    public void tearDown() {

    }

    @Override
    public void ponderStart() {

    }

    @Override
    public void ponderStop() {

    }

    @Override
    public void destroy() {

    }
}
