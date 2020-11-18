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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Leeroy<G extends Game<A, RiskBoard>, A> extends AbstractGameAgent<G, A> implements GameAgent<G, A> {
    private final double TROOPS_RELATION_THRESHOLD = 0.2;
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
        // @anton TODO: change to v1.0.1 game.getBoard.is__Phase() ??
        setPhase(risk);
        if (currentPhase == Phase.INITIAL_SELECT) {
            return (A) selectInitialCountry(risk);
        } else if (currentPhase == Phase.INITIAL_REINFORCE || currentPhase == Phase.REINFORCE) {
            return (A) reinforce(risk);
        }
        return (A) Util.selectRandom(risk.getPossibleActions());
    }

    private void setPhase(Risk game) {
        if (currentPhase == Phase.INITIAL_SELECT
                && PhaseUtils.stillUnoccupiedTerritories(game.getBoard())) {
            if (this.initialPlacementRoot == null) {
                log.info(Phase.INITIAL_SELECT);
                this.initialPlacementRoot = new InitialPlacementNode((game.getCurrentPlayer() + 1) % 2,
                        -1,
                        null,
                        GameUtils.getOccupiedEntries(game),
                        GameUtils.getUnoccupiedEntries(game));
            } else {
                setNewInitialPlacementRoot(game);
            }
        } else if (currentPhase == Phase.INITIAL_SELECT) {
            log.inf(Phase.INITIAL_REINFORCE);
            currentPhase = Phase.INITIAL_REINFORCE;
        } else if (currentPhase == Phase.INITIAL_REINFORCE
                && PhaseUtils.initialPlacementFinished(game.getBoard(), playerNumber, GameUtils.getNumberOfStartingTroops(numberOfPlayers))) {
            log.info(Phase.REINFORCE);
            currentPhase = Phase.REINFORCE;
        } else if (currentPhase == Phase.REINFORCE && !PhaseUtils.inReinforcing(game)) {
            log.info(Phase.ATTACK);
            currentPhase = Phase.ATTACK;
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

        var board = game.getBoard();
        var continentalUnits = board
                .getTerritories()
                .values()
                .stream()
                .collect(Collectors.groupingBy(RiskTerritory::getContinentId,
                        Collectors.summingDouble(RiskTerritory::getTroops)));

        var playerContinentalUnits = board
                .getTerritories()
                .values()
                .stream()
                .filter(riskTerritory -> riskTerritory.getOccupantPlayerId() == this.playerNumber)
                .collect(Collectors.groupingBy(RiskTerritory::getContinentId,
                        Collectors.summingDouble(RiskTerritory::getTroops)));

        var continentEstimatedBest = Stream
                .concat(playerContinentalUnits.entrySet().stream(),
                        continentalUnits.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue,
                        (value1, value2) -> value1 / value2))
                .entrySet()
                .stream()
                .filter(integerDoubleEntry -> integerDoubleEntry.getValue() < 0.99)
                .map(integerDoubleEntry -> new AbstractMap.SimpleImmutableEntry<>(integerDoubleEntry.getKey(),
                        Math.abs(TROOPS_RELATION_THRESHOLD - integerDoubleEntry.getValue())))
                .min(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElseThrow();

        return board
                .getTerritories()
                .entrySet()
                .stream()
                .filter(integerRiskTerritoryEntry -> integerRiskTerritoryEntry.getValue().getContinentId() == continentEstimatedBest)
                .filter(riskTerritory -> game.getPossibleActions().stream().anyMatch(action -> action.reinforcedId() == riskTerritory.getKey()))
                .map(riskTerritory -> board.neighboringEnemyTerritories(riskTerritory.getKey())
                        .stream()
                        .map(board::getTerritoryTroops)
                        .reduce(Integer::sum)
                        .map(Integer::doubleValue)
                        .map(noEnemyTroops -> new AbstractMap.SimpleImmutableEntry<>(riskTerritory.getKey(),
                                riskTerritory.getValue().getTroops()
                                        / (noEnemyTroops + riskTerritory.getValue().getTroops())))
                        .orElse(new AbstractMap.SimpleImmutableEntry<>(riskTerritory.getKey(), 1d)))
//                .filter(integerDoubleSimpleImmutableEntry -> integerDoubleSimpleImmutableEntry.getValue() < 0.99)
                .map(riskTerritory -> new AbstractMap.SimpleImmutableEntry<>(riskTerritory.getKey(),
                        Math.abs(TROOPS_RELATION_THRESHOLD - riskTerritory.getValue())))
                .min(Comparator.comparingDouble(AbstractMap.SimpleImmutableEntry::getValue))
                .map(integerDoubleSimpleImmutableEntry -> RiskAction.reinforce(integerDoubleSimpleImmutableEntry.getKey(),
                        getTroopsToReinforce(integerDoubleSimpleImmutableEntry.getKey(), game)))
                .orElseThrow();
    }

    private boolean hasToPlayCards(Risk game) {
        return game.getPossibleActions()
                .stream()
                .allMatch(RiskAction::isCardIds);
    }

    private int getTroopsToReinforce(Integer key, Risk game) {
        return game.getPossibleActions()
                .stream()
                .filter(riskAction -> riskAction.reinforcedId() == key)
                .map(RiskAction::troops)
                .max(Integer::compareTo)
                .orElseThrow();
    }

    private Node searchBestNode(Node node, Function<Node, Node> expansionFunction, Function<Node, Integer> evaluationFunction) {
        performMCTS(node, expansionFunction, evaluationFunction);
        return node.getSuccessors().stream().max(Comparator.comparingDouble(Node::getWinScore)).get();
    }

    private void setNewInitialPlacementRoot(Risk game) {
        var occupiedEntries = GameUtils.getOccupiedEntries(game);
        initialPlacementRoot = initialPlacementRoot
                .getSuccessors()
                .stream()
                .filter(node -> equal(((InitialPlacementNode) node).getOccupiedTerritories(), occupiedEntries))
                .findFirst()
                .get();
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
