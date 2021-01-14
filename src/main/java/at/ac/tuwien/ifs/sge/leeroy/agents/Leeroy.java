package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;
import at.ac.tuwien.ifs.sge.leeroy.phase.Phase;
import at.ac.tuwien.ifs.sge.leeroy.phase.PhaseUtils;
import at.ac.tuwien.ifs.sge.util.Util;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Leeroy<G extends Game<A, RiskBoard>, A> extends AbstractGameAgent<G, A> implements GameAgent<G, A> {

    private final int INITIAL_SELECT_TIMEOUT_PENALTY = 10000000;
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

        // :-( other agents might influence ours
        System.gc();

        log.info("Computing action");
        Risk risk = (Risk) game;
        A nextAction;
        try {
            setPhase(game.getBoard());
            if (currentPhase == Phase.INITIAL_SELECT) {
                setNewInitialPlacementRoot(risk);
                nextAction = (A) selectInitialCountry(risk);
            } else if (game.getBoard().isReinforcementPhase()) {
                nextAction = (A) reinforce(risk);
            } else if (game.getBoard().isAttackPhase()) {
                nextAction = (A) attackTerritory(risk);
            } else if (game.getBoard().isOccupyPhase()) {
                nextAction = (A) occupyTerritory(risk);
            } else if (game.getBoard().isFortifyPhase()) {
                nextAction = (A) fortifyTerritory(risk);
            } else {
                nextAction = (A) Util.selectRandom(risk.getPossibleActions());
            }
        } catch(Exception e) {
            e.printStackTrace();
            nextAction = (A) Util.selectRandom(risk.getPossibleActions());
        }
        if (nextAction == null) {
            log.err("No action; state " + currentPhase + "; " + game );
            nextAction = (A) Util.selectRandom(risk.getPossibleActions());
        }
        if (! game.isValidAction(nextAction)) {
            log.err("Invalid action" + nextAction + "; state " + currentPhase + "; " + game );
            nextAction = (A) Util.selectRandom(risk.getPossibleActions());
        }
        return nextAction;
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

    protected RiskAction reinforce(Risk game) {
        if (hasToTradeInCards(game)) {
            return tradeInCards(game);
        }

        return HeuristicReinforce.reinforce(playerNumber, game);
    }

    private boolean hasToTradeInCards(Risk game) {
        return game.getBoard().hasToTradeInCards(playerNumber);
    }

    private RiskAction tradeInCards(Risk game) {
        return Util.selectRandom(game
                .getPossibleActions()
                .stream()
                .filter(RiskAction::isCardIds)
                .collect(Collectors.toSet()));
    }

    private Node searchBestNode(Node node, Function<Node, Node> expansionFunction, Function<Node, Integer> evaluationFunction) {
        performMCTS(node, expansionFunction, evaluationFunction);
        return node.getSuccessors().stream().max(Comparator.comparingDouble(Node::getWinScore)).get();
    }

    protected RiskAction attackTerritory(Risk risk) {
        Set<RiskAction> attackActions = AttackActionSupplier.createActions(risk, null);
        Optional<RiskAction> highAtkAction = attackActions
                .stream()
                .sorted(Comparator.comparingInt(action -> action.troops() *-1))
                .findFirst();
        if (highAtkAction.isPresent()) {
            RiskAction atkAction = highAtkAction.get();
            return RiskAction.attack(atkAction.attackingId(), atkAction.defendingId(), Math.min(3, atkAction.troops()));
        }
        return RiskAction.endPhase();
    }

    protected RiskAction occupyTerritory(Risk risk) {
        return RiskAction.occupy(risk.getPossibleActions().size());
    }

    private RiskAction fortifyTerritory(Risk risk) {
        // TODO: MCTS
        Set<RiskAction> fortificationActions = FortificationActionSupplier.createActions(risk);
        Optional<RiskAction> bestAction = fortificationActions
                .stream()
                .sorted(Comparator.comparingInt(action -> action.troops() *-1))
                .findFirst();
        return bestAction.orElse(RiskAction.endPhase()); // if no action was found we just end the at.ac.tuwien.ifs.sge.leeroy.phase
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
        while (!this.shouldStopComputation(INITIAL_SELECT_TIMEOUT_PENALTY)) {
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
