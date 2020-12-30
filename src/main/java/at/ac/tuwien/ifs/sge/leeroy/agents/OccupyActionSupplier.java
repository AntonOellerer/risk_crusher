package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.game.ActionRecord;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;

import java.util.Set;

public class OccupyActionSupplier {

    public static Set<RiskAction> createActions(Risk risk) {
        int attackActionId = risk.getNumberOfActions() - 2;
        ActionRecord attackRecord = risk.getActionRecords().get(attackActionId);
        RiskAction attackAction = (RiskAction)attackRecord.getAction();

        return createActions(risk, attackAction);
    }

    public static Set<RiskAction> createActions(Risk risk, RiskAction attackAction) {
        Set<RiskAction> possibleActions = risk.getPossibleActions();
        int srcTerritory = attackAction.attackingId();
        int targetTerritory = attackAction.defendingId();

        boolean isSrcSafe = risk.getBoard().neighboringEnemyTerritories(srcTerritory).isEmpty();
        boolean isTargetSafe = risk.getBoard().neighboringEnemyTerritories(targetTerritory).isEmpty();

        if (isTargetSafe && ! isSrcSafe) {
            return Set.of(RiskAction.occupy(1)); // min troops
        } else if (isSrcSafe && ! isTargetSafe) {
            return Set.of(RiskAction.occupy(risk.getBoard().getFortifyableTroops(srcTerritory))); // max troops
        }

        return possibleActions; // all troops -> let mcts decide - room for improvements
    }
}
