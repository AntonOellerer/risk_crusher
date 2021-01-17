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

/**
 * Leeroy is our base risk agent.
 * When using this class for playing a risk game, most actions are selected
 * heuristically (in the initial occupation phase, MCTS is used).
 *
 * @param <G> The type of game (has to be a Risk game for Leeroy)
 * @param <A> The type of actions (have to be RiskActions for Leeroy)
 */
public class Leeroy<G extends Game<A, RiskBoard>, A> extends AbstractGameAgent<G, A> implements GameAgent<G, A> {

    private final int INITIAL_SELECT_TIMEOUT_PENALTY = 10000000;
    Phase currentPhase = Phase.INITIAL_SELECT;
    Node initialPlacementRoot;
    private int playerNumber;
    private int numberOfPlayers;

    /**
     * Generate a new Leeroy agent
     *
     * @param log The logger to use
     */
    public Leeroy(Logger log) {
        super(log);
    }

    /**
     * Get the next action Leeroy wants to do based on the current game.
     *
     * @param game            The game (has to be a risk game)
     * @param computationTime The time Leeroy has for computing an action
     * @param timeUnit        The time unit the computation time has.
     * @return The next (risk) action to do
     */
    @Override
    public A computeNextAction(G game, long computationTime, TimeUnit timeUnit) {
        super.setTimers(computationTime, timeUnit);

        // :-( other agents might influence ours
        System.gc();

        log.info("Computing action");
        Risk risk = (Risk) game;
        RiskBoard board = risk.getBoard();
        A nextAction;
        try {
            setPhase(board);
            if (currentPhase == Phase.INITIAL_SELECT) {
                setNewInitialPlacementRoot(risk, board);
                nextAction = (A) selectInitialCountry(risk, board);
            } else if (board.isReinforcementPhase()) {
                nextAction = (A) reinforce(risk, board);
            } else if (board.isAttackPhase()) {
                nextAction = (A) attackTerritory(risk, board);
            } else if (board.isOccupyPhase()) {
                nextAction = (A) occupyTerritory(risk);
            } else if (board.isFortifyPhase()) {
                nextAction = (A) fortifyTerritory(risk, board);
            } else {
                nextAction = (A) Util.selectRandom(risk.getPossibleActions());
            }
        } catch (Exception e) {
            e.printStackTrace();
            nextAction = (A) Util.selectRandom(risk.getPossibleActions());
        }
        if (nextAction == null) {
            log.err("No action; state " + currentPhase + "; " + game);
            nextAction = (A) Util.selectRandom(risk.getPossibleActions());
        }
        if (!game.isValidAction(nextAction)) {
            log.err("Invalid action" + nextAction + "; state " + currentPhase + "; " + game);
            nextAction = (A) Util.selectRandom(risk.getPossibleActions());
        }
        return nextAction;
    }

    /**
     * Detect and set the phase we are currently in
     *
     * @param board The board to detect the phase
     */
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

    /**
     * Select an unoccupied country we want to occupy
     * This method employs MCTS for selecting the node we hope results in the best
     * board for us at the end of the initial occupation phase.
     *
     * @param game  The risk game
     * @param board The risk board (separate so it can be cached)
     * @return A risk action to occupy an unoccupied country
     */
    private RiskAction selectInitialCountry(Risk game, RiskBoard board) {
        //Graph moved one node forward after the action
        initialPlacementRoot = searchBestNode(initialPlacementRoot, GameUtils.partialInitialExpansionFunction(board), GameUtils.partialInitialEvaluationFunction(game, board));
        return RiskAction.select(initialPlacementRoot.getId());
    }

    /**
     * Get an action to reinforce the country the heuristic deems best for reinforcing.
     * If we are forced to trade in cards, it is done.
     * Refer to the HeuristicReinforce class for more in-depth explanation of the employed heuristic.
     *
     * @param game  The current risk game
     * @param board The current risk board (separate so it can be cached)
     * @return A risk action for reinforcing the territory of the player
     */
    protected RiskAction reinforce(Risk game, RiskBoard board) {
        if (hasToTradeInCards(board)) {
            return tradeInCards(game);
        }

        return HeuristicReinforce.reinforce(playerNumber, game, board);
    }

    private boolean hasToTradeInCards(RiskBoard board) {
        return board.hasToTradeInCards(playerNumber);
    }

    private RiskAction tradeInCards(Risk game) {
        return Util.selectRandom(game
                .getPossibleActions()
                .stream()
                .filter(RiskAction::isCardIds)
                .collect(Collectors.toSet()));
    }

    /**
     * Get the node we estimate best for winning
     *
     * @param node               The root node
     * @param expansionFunction  The function to expand nodes
     * @param evaluationFunction The function to evaluate nodes
     * @return The node we estimate best for winning
     */
    private Node searchBestNode(Node node, Function<Node, Node> expansionFunction, Function<Node, Integer> evaluationFunction) {
        performMCTS(node, expansionFunction, evaluationFunction);
        return node.getSuccessors().stream().max(Comparator.comparingDouble(Node::getWinScore)).get();
    }

    /**
     * Get the risk action deemed the best for attacking.
     * For more in-depth explanation of the action generation function, refer to the AttackActionSupplier class
     * From the generated attack actions, we select the one involving the highest number of troops (unbounded by
     * attacking rules) and then generate the correct attack action.
     * If an attack action can not be generated, we end the game.
     *
     * @param risk  The current risk game
     * @param board The current risk board (separate for caching)
     * @return The risk attack action deemed best
     */
    protected RiskAction attackTerritory(Risk risk, RiskBoard board) {
        Set<RiskAction> attackActions = AttackActionSupplier.createActions(risk, board, null);
        Optional<RiskAction> highAtkAction = attackActions
                .stream()
                .max(Comparator.comparingInt(RiskAction::troops));
        if (highAtkAction.isPresent()) {
            RiskAction atkAction = highAtkAction.get();
            return RiskAction.attack(atkAction.attackingId(), atkAction.defendingId(), Math.min(3, atkAction.troops()));
        }
        return RiskAction.endPhase();
    }

    /**
     * Generate the occupation action after conquering a territory.
     * This function always moves the maximum amount of troops to the new territory.
     *
     * @param risk The risk game
     * @return The action to occupy the new territory.
     */
    protected RiskAction occupyTerritory(Risk risk) {
        return RiskAction.occupy(risk.getPossibleActions().size());
    }

    /**
     * Get a fortification function for moving troops from one territory to another one at the end of a turn.
     * Refer to the FortificationActionSupplier for a more in-depth explanation of the generated actions.
     * From the generated attack actions, we select the one involving the highest number of troops.
     * If none is yielded, we just end the phase.
     *
     * @param risk  The risk game
     * @param board The risk board (separate so it can be cached)
     * @return The risk action for fortifying from one player territory to another
     */
    private RiskAction fortifyTerritory(Risk risk, RiskBoard board) {
        Set<RiskAction> fortificationActions = FortificationActionSupplier.createActions(risk, board);
        Optional<RiskAction> bestAction = fortificationActions
                .stream()
                .max(Comparator.comparingInt(RiskAction::troops));
        return bestAction.orElse(RiskAction.endPhase()); // if no action was found we just end the at.ac.tuwien.ifs.sge.leeroy.phase
    }

    private void setNewInitialPlacementRoot(Risk game, RiskBoard board) {
        if (this.initialPlacementRoot == null) {
            log.info(Phase.INITIAL_SELECT);
            this.initialPlacementRoot = new InitialPlacementNode((game.getCurrentPlayer() + 1) % 2,
                    -1,
                    null,
                    GameUtils.getOccupiedEntries(board),
                    GameUtils.getUnoccupiedEntries(board));
        } else {
            var occupiedEntries = GameUtils.getOccupiedEntries(board);
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

    /**
     * Set up Leeroy for playing a game (has to be called before starting a game with Leeroy)
     *
     * @param numberOfPlayers How many players will play the game
     * @param playerNumber    Which player number Leeroy has
     */
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
