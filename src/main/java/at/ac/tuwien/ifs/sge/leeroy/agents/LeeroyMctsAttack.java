package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.leeroy.mcts.ActionNode;
import at.ac.tuwien.ifs.sge.leeroy.mcts.AttackMctsActionSupplier;
import at.ac.tuwien.ifs.sge.leeroy.mcts.MctsActionSupplier;
import at.ac.tuwien.ifs.sge.util.Util;

/***
 * extends Leeroy with Mcts Attack/Occupy selection
 */
public class LeeroyMctsAttack extends Leeroy {

    protected final int ATTACK_TIMEOUT_PENALTY = 1;
    protected final int OCCUPY_TIMEOUT_PENALTY = 2;
    protected final int REINFORCE_TIMEOUT_PENALTY = 2;

    public LeeroyMctsAttack(Logger log) {
        super(log);
    }

    @Override
    protected RiskAction reinforce(Risk risk) {
        MctsActionSupplier actionSupplier = new AttackMctsActionSupplier(
                () -> this.shouldStopComputation(REINFORCE_TIMEOUT_PENALTY));
        actionSupplier.setRootNode(new ActionNode(risk.getCurrentPlayer(), null, risk, null));
        ActionNode bestNode = actionSupplier.findBestNode();
        return bestNode != null ? bestNode.getAction() : Util.selectRandom(risk.getPossibleActions());
    }

    @Override
    protected RiskAction attackTerritory(Risk risk) {
        MctsActionSupplier actionSupplier = new AttackMctsActionSupplier(
                () -> this.shouldStopComputation(ATTACK_TIMEOUT_PENALTY));
        actionSupplier.setRootNode(new ActionNode(risk.getCurrentPlayer(), null, risk, null));
        ActionNode bestNode = actionSupplier.findBestNode();
        return bestNode != null ? bestNode.getAction() : Util.selectRandom(risk.getPossibleActions());
    }

    @Override
    protected RiskAction occupyTerritory(Risk risk) {
        MctsActionSupplier actionSupplier = new AttackMctsActionSupplier(
                () -> this.shouldStopComputation(OCCUPY_TIMEOUT_PENALTY));
        actionSupplier.setRootNode(new ActionNode(risk.getCurrentPlayer(), null, risk, null));
        ActionNode bestNode = actionSupplier.findBestNode();
        return bestNode != null ? bestNode.getAction() : Util.selectRandom(risk.getPossibleActions());
    }
}
