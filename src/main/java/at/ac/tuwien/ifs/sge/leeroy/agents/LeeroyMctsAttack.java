package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import at.ac.tuwien.ifs.sge.leeroy.mcts.ActionNode;
import at.ac.tuwien.ifs.sge.leeroy.mcts.AttackMctsActionSupplier;
import at.ac.tuwien.ifs.sge.leeroy.mcts.MctsActionSupplier;
import at.ac.tuwien.ifs.sge.util.Util;

/***
 * extends Leeroy with MCTS Reinforce/Attack/Occupy selection
 * It builds a new MCTS tree after each turn.
 */
public class LeeroyMctsAttack extends Leeroy {

    protected final int ATTACK_TIMEOUT_PENALTY = 1;
    protected final int OCCUPY_TIMEOUT_PENALTY = 2;
    protected final int REINFORCE_TIMEOUT_PENALTY = 2;

    /**
     * Generate a new MCTS-based Leeroy
     *
     * @param log The logger to use
     */
    public LeeroyMctsAttack(Logger log) {
        super(log);
    }

    /**
     * Get a reinforcement action for the game.
     * This method uses MCTS.
     *
     * @param risk  The risk game
     * @param board The current risk board (separate so it can be cached)
     * @return A risk action to reinforce a player's territory (or a play cards action)
     */
    @Override
    protected RiskAction reinforce(Risk risk, RiskBoard board) {
        return performMCTS(risk);
    }

    /**
     * Get a attack action for the game.
     * This method uses MCTS.
     *
     * @param risk  The risk game
     * @param board The current risk board (separate so it can be cached)
     * @return A risk action to attack an enemy's territory (or a phase end action)
     */
    @Override
    protected RiskAction attackTerritory(Risk risk, RiskBoard board) {
        return performMCTS(risk);
    }

    /**
     * Get an action to reinforce a newly conquered territory .
     * This method uses MCTS.
     *
     * @param risk The risk game
     * @return A risk action to occupy a new territory (or a phase end action)
     */
    @Override
    protected RiskAction occupyTerritory(Risk risk) {
        return performMCTS(risk);
    }

    private RiskAction performMCTS(Risk risk) {
        MctsActionSupplier actionSupplier = new AttackMctsActionSupplier(
                () -> this.shouldStopComputation(OCCUPY_TIMEOUT_PENALTY));
        actionSupplier.setRootNode(new ActionNode(risk.getCurrentPlayer(), null, risk, null));
        ActionNode bestNode = actionSupplier.findBestNode();
        return bestNode != null ? bestNode.getAction() : Util.selectRandom(risk.getPossibleActions());
    }
}
