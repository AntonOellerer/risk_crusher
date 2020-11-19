package agents.leeroy;

import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;
import at.ac.tuwien.ifs.sge.util.Util;
import phase.Phase;
import phase.PhaseUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class Leeroy<G extends Game<A, RiskBoard>, A> extends AbstractGameAgent<G, A> implements GameAgent<G, A> {
    Phase currentPhase = Phase.INITIAL_SELECT;
    Node initialPlacementRoot;
    private int playerNumber;
    private int numberOfPlayers;

    public Leeroy(Logger log) {
        super(log);
    }

    @Override
    public A computeNextAction(G game, long computationTime, TimeUnit timeUnit) {
        super.setTimers(computationTime, timeUnit);
        log.info("Computing action");
        Risk risk = (Risk) game;
        RiskBoard board = game.getBoard();
        setPhase(board);
        if (currentPhase == Phase.INITIAL_SELECT) {
            setNewInitialPlacementRoot(risk);
            return (A) selectInitialCountry(risk);
        } else if (game.getBoard().isAttackPhase()) {
            return (A) attackTerritory(risk);
        } else if (currentPhase == Phase.INITIAL_REINFORCE || currentPhase == Phase.REINFORCE) {
            return (A) reinforce(risk);
        }
        return (A) Util.selectRandom(risk.getPossibleActions());
    }

    private void setPhase(RiskBoard board) {
        if (currentPhase == Phase.INITIAL_SELECT && PhaseUtils.stillUnoccupiedTerritories(board)) {
            currentPhase = Phase.INITIAL_SELECT;
        } else if (board.isReinforcementPhase()) {
            currentPhase = Phase.REINFORCE;
        } else if (board.isAttackPhase()) {
            currentPhase = Phase.ATTACK;
        } else if (board.isFortifyPhase()) {
            currentPhase = Phase.FORTIFY;
        } else if (board.isOccupyPhase()) {
            currentPhase = Phase.FORTIFY;
        }
    }

    private RiskAction selectInitialCountry(Risk game) {
        //Graph moved one node forward after the action
        initialPlacementRoot = searchBestNode(initialPlacementRoot, GameUtils.partialInitialExpansionFunction(game), GameUtils.partialInitialEvaluationFunction(game));
        return RiskAction.select(initialPlacementRoot.getId());
    }

    private RiskAction reinforce(Risk game) {
        if (hasToPlayCards(game)) {
            return Util.selectRandom(game.getPossibleActions());
        }

        return HeuristicReinforce.reinforce(playerNumber, game);
    }

    private boolean hasToPlayCards(Risk game) {
        return game.getPossibleActions()
                .stream()
                .allMatch(RiskAction::isCardIds);
    }

    private Node searchBestNode(Node node, Function<Node, Node> expansionFunction, Function<Node, Integer> evaluationFunction) {
        performMCTS(node, expansionFunction, evaluationFunction);
        return node.getSuccessors().stream().max(Comparator.comparingDouble(Node::getWinScore)).get();
    }

    private RiskAction attackTerritory(Risk risk) {
        // TODO: MCTS
        return Util.selectRandom(AttackActionSupplier.createActions(risk));
    }

    private void setNewInitialPlacementRoot(Risk game) {
        if (this.initialPlacementRoot == null) {
            log.info(Phase.INITIAL_SELECT);
            this.initialPlacementRoot = new InitialPlacementNode((game.getCurrentPlayer() + 1) % 2,
                    -1,
                    null,
                    GameUtils.getOccupiedEntries(game),
                    GameUtils.getUnoccupiedEntries(game));
        } else {

            var occupiedEntries = GameUtils.getOccupiedEntries(game);
            initialPlacementRoot = initialPlacementRoot
                    .getSuccessors()
                    .stream()
                    .filter(node -> equal(((InitialPlacementNode) node).getOccupiedTerritories(), occupiedEntries))
                    .findFirst()
                    .get();
        }
    }

    private boolean equal(List<Map.Entry<Integer, RiskTerritory>> occupiedEntriesA, List<Map.Entry<Integer, RiskTerritory>> occupiedEntriesB) {
        if (occupiedEntriesA.size() != occupiedEntriesB.size()) {
            return false;
        }
        for (Map.Entry<Integer, RiskTerritory> occupiedEntryA : occupiedEntriesA) {
            boolean present = occupiedEntriesB.
                    stream()
                    .filter(integerRiskTerritoryEntry -> integerRiskTerritoryEntry.getKey().equals(occupiedEntryA.getKey()))
                    .anyMatch(integerRiskTerritoryEntry -> integerRiskTerritoryEntry.getValue().getOccupantPlayerId() == occupiedEntryA.getValue().getOccupantPlayerId());
            if (!present) {
                return false;
            }
        }
        return true;
    }

    private void performMCTS(Node node, Function<Node, Node> nodeSelectionFunction, Function<Node, Integer> evaluationFunction) {
        while (!this.shouldStopComputation()) {
            var selectedNode = select(node);
            var successors = selectedNode.getSuccessors(); //expand
            if (successors.isEmpty()) {
                backpropagate(node.getPlayer(), selectedNode, evaluationFunction.apply(selectedNode));
            } else {
                var explorationNode = nodeSelectionFunction.apply(selectedNode);
                int playOutResult = playOutGame(explorationNode, nodeSelectionFunction, evaluationFunction);
                backpropagate(node.getPlayer(), explorationNode, playOutResult);
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

    @Override
    public void setUp(int numberOfPlayers, int playerNumber) {
        super.setUp(numberOfPlayers, playerNumber);
        this.playerNumber = playerNumber;
        this.numberOfPlayers = numberOfPlayers;
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
