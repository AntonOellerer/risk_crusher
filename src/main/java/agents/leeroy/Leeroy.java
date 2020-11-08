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

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class Leeroy<G extends Game<A, RiskBoard>, A> extends AbstractGameAgent<G, A> implements GameAgent<G, A> {
    Phase currentPhase = Phase.INITIAL_SELECT;
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

    private void setPhase(Risk risk) {
        if (currentPhase == Phase.INITIAL_SELECT
                && !PhaseUtils.stillUnoccupiedTerritories(risk.getBoard())) {
            currentPhase = Phase.INITIAL_REINFORCE;
        } else if (currentPhase == Phase.INITIAL_REINFORCE
                && PhaseUtils.initialPlacementFinished(risk.getBoard(), playerNumber, GameUtils.getNumberOfStartingTroops(numberOfPlayers))) {
            currentPhase = Phase.REINFORCE;
        } else if (currentPhase == Phase.REINFORCE && !PhaseUtils.inReinforcing(risk)) {
            currentPhase = Phase.ATTACK;
        }
    }

    private RiskAction selectInitialCountry(Risk risk) {
        InitialPlacementNode startNode = new InitialPlacementNode(-1, GameUtils.getOccupiedEntries(risk), GameUtils.getUnoccupiedEntries(risk));
        int bestValue = Integer.MIN_VALUE;
        Optional<RiskAction> bestAction = Optional.empty();
        for (Node successorNode : startNode.getSuccessors()) {
            int value = performAlphaBetaSearch(successorNode,
                    false,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    GameUtils.partialEvaluationFunction(risk));
            if (value > bestValue) {
                bestValue = value;
                bestAction = Optional.of(RiskAction.select(successorNode.getId()));
            }
        }
        return bestAction.orElseThrow();
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
