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
                .filter(riskTerritory -> riskTerritory.getOccupantPlayerId() == game.getCurrentPlayer())
                .collect(Collectors.groupingBy(RiskTerritory::getContinentId,
                        Collectors.summingDouble(RiskTerritory::getTroops)));
        var continentalTroopsShare = Stream.concat(playerContinentalUnits.entrySet().stream(), continentalUnits.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue,
                        (value1, value2) -> value1 / value2))
                .entrySet()
                .stream()
                .filter(integerDoubleEntry -> integerDoubleEntry.getValue() > 0) //filter out continents where we don't have any troops
                .filter(integerDoubleEntry -> 1.0 - integerDoubleEntry.getValue() > TROOPS_RELATION_THRESHOLD)
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey);

        if (continentalTroopsShare.isPresent()) {
            var territoriesRelations = board
                    .getTerritories()
                    .entrySet()
                    .stream()
                    .filter(integerRiskTerritoryEntry -> integerRiskTerritoryEntry.getValue().getContinentId() == continentalTroopsShare.get())
                    .filter(integerRiskTerritoryEntry -> integerRiskTerritoryEntry.getValue().getOccupantPlayerId() == game.getCurrentPlayer())
                    .map(integerRiskTerritoryEntry -> {
                        var enemyTroops = board.neighboringEnemyTerritories(integerRiskTerritoryEntry.getKey())
                                .stream()
                                .reduce(Integer::sum)
                                .map(Integer::doubleValue);
                        return enemyTroops.
                                map(noTroops -> new AbstractMap.SimpleImmutableEntry<>(integerRiskTerritoryEntry.getKey(), integerRiskTerritoryEntry.getValue().getTroops() / noTroops))
                                .orElseGet(() -> new AbstractMap.SimpleImmutableEntry<>(integerRiskTerritoryEntry.getKey(), 1d));
                    })
                    .sorted(Comparator.comparingDouble(AbstractMap.SimpleImmutableEntry::getValue))
                    .collect(Collectors.toList());
            for (int i = territoriesRelations.size() - 1; i == 0; i--) {
                if (1d - territoriesRelations.get(i).getValue() > TROOPS_RELATION_THRESHOLD) {
                    return RiskAction.select(territoriesRelations.get(i).getKey());
                }
            }
            log.warn("Did not find fitting territory in selected continent");
            return Util.selectRandom(game.getPossibleActions());
        } else {
            return board
                    .getTerritories()
                    .entrySet()
                    .stream()
                    .filter(integerRiskTerritoryEntry -> integerRiskTerritoryEntry.getValue().getOccupantPlayerId() == game.getCurrentPlayer())
                    .map(integerRiskTerritoryEntry -> {
                        var enemyTroops = board.neighboringEnemyTerritories(integerRiskTerritoryEntry.getKey())
                                .stream()
                                .reduce(Integer::sum)
                                .map(Integer::doubleValue);
                        return enemyTroops.
                                map(noTroops -> new AbstractMap.SimpleImmutableEntry<>(integerRiskTerritoryEntry.getKey(), integerRiskTerritoryEntry.getValue().getTroops() / noTroops))
                                .orElseGet(() -> new AbstractMap.SimpleImmutableEntry<>(integerRiskTerritoryEntry.getKey(), 1d));
                    })
                    .min(Comparator.comparingDouble(AbstractMap.SimpleImmutableEntry::getValue))
                    .map(integerDoubleSimpleImmutableEntry -> RiskAction.select(integerDoubleSimpleImmutableEntry.getKey()))
                    .orElse(Util.selectRandom(game.getPossibleActions()));
        }
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
