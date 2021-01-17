package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.leeroy.mcts.ActionNode;
import at.ac.tuwien.ifs.sge.leeroy.mcts.AttackMctsActionSupplier;
import at.ac.tuwien.ifs.sge.leeroy.mcts.MctsActionSupplier;
import at.ac.tuwien.ifs.sge.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * adds a cached mcts tree which reuses searches from previous simulation runs
 */
public class CachedMctsLeeroy extends LeeroyMctsAttack {

    private List<ActionNode> simulationSuccessors = new ArrayList<>();

    public CachedMctsLeeroy(Logger log) {
        super(log);
    }

    protected RiskAction reinforce(Risk risk) {
        return this.performAction(risk, REINFORCE_TIMEOUT_PENALTY);
    }

    @Override
    protected RiskAction attackTerritory(Risk risk) {
        return this.performAction(risk, ATTACK_TIMEOUT_PENALTY);
    }

    @Override
    protected RiskAction occupyTerritory(Risk risk) {
        return this.performAction(risk, OCCUPY_TIMEOUT_PENALTY);
    }

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
     * tries to reuse successors from previous simulation run(s), if non exist creates new root
     * @return
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
