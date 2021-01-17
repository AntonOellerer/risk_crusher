package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import at.ac.tuwien.ifs.sge.leeroy.mcts.ActionNode;
import at.ac.tuwien.ifs.sge.leeroy.mcts.AttackMctsActionSupplier;
import at.ac.tuwien.ifs.sge.leeroy.mcts.MctsActionSupplier;
import at.ac.tuwien.ifs.sge.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * This agent saves the tree in between actions of the same turn, afterwards it is discarded
 * The steps "reinforcement", "attacking" and "occupying" are implemented by this agent.
 */
public class CachedMctsLeeroy extends LeeroyMctsAttack {

    private List<ActionNode> simulationSuccessors = new ArrayList<>();

    /**
     * Create a new Risk agent using MCTS operating on one tree per (risk) turn.
     *
     * @param log The logger to use
     */
    public CachedMctsLeeroy(Logger log) {
        super(log);
    }

    /**
     * Get a RiskAction for reinforcing a territory owned by the player (or to trade in cards)
     * This method is just a caching wrapper for the reinforce function employed by LeeroyMctsAttack
     *
     * @param risk      The risk game
     * @param riskBoard The risk board (separate so it can be cached)
     * @return The risk action to reinforce a players territory.
     */
    @Override
    protected RiskAction reinforce(Risk risk, RiskBoard riskBoard) {
        return this.performAction(risk, REINFORCE_TIMEOUT_PENALTY);
    }

    /**
     * Get a RiskAction for attacking an enemy territory.
     * This method is just a caching wrapper for the attackTerritory function employed by LeeroyMctsAttack
     *
     * @param risk      The risk game
     * @param riskBoard The risk board (separate so it can be cached)
     * @return The risk action to attack an enemy territory.
     */
    @Override
    protected RiskAction attackTerritory(Risk risk, RiskBoard riskBoard) {
        return this.performAction(risk, ATTACK_TIMEOUT_PENALTY);
    }

    /**
     * Get a RiskAction for occupying a territory just taken by the player
     * This method is just a caching wrapper for the occupy function employed by LeeroyMctsAttack
     *
     * @param risk The risk game
     * @return The risk action occupy a newly conquered territory.
     */
    @Override
    protected RiskAction occupyTerritory(Risk risk) {
        return this.performAction(risk, OCCUPY_TIMEOUT_PENALTY);
    }

    /**
     * Employ MCTS for finding the best action to take.
     * The tree is cached per round and discarded whenever we detect the enemy has just acted.
     *
     * @param risk           The risk game
     * @param timeoutPenalty The penalty for timeout
     * @return The risk action to perform
     */
    protected RiskAction performAction(Risk risk, int timeoutPenalty) {
        MctsActionSupplier actionSupplier = new AttackMctsActionSupplier(
                () -> this.shouldStopComputation(timeoutPenalty));
        actionSupplier.setRootNode(getRootNode(risk));
        ActionNode bestNode = actionSupplier.findBestNode();
        if (bestNode != null) {
            simulationSuccessors = bestNode.getSuccessors();
            return bestNode.getAction();
        }
        // mcts stopped before any node was evaluated - mostly caused by unstable opponent agents
        simulationSuccessors = new ArrayList<>();
        return Util.selectRandom(risk.getPossibleActions());
    }

    /**
     * choose root node for mcts
     * tries to reuse successors from previous simulation run(s), if none exist creates new root
     *
     * @return The new root node for performing MCTS on.
     */
    protected ActionNode getRootNode(Risk game) {
        RiskAction lastAction = game.getPreviousAction();
        ActionNode defaultRoot = new ActionNode(game.getCurrentPlayer(), null, game, null);
        if (lastAction == null || simulationSuccessors == null || simulationSuccessors.isEmpty()) {
            return defaultRoot;
        }
        return simulationSuccessors
                .stream()
                .filter(simNode -> simNode.getAction().equals(lastAction))
                .findFirst()
                .orElse(defaultRoot);
    }
}
