package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import at.ac.tuwien.ifs.sge.leeroy.mcts.ActionNode;

import java.util.stream.Stream;

public class ReinforcementActionSupplier {

    /**
     * Get the reinforcement actions
     * For our agent, we just want to reinforce territories which
     * are neighbour to an enemy territory.
     *
     * @param selectedNode The node to expand
     * @param riskBoard    The board status at the selected node.
     *                     Since calculating it anew is very expensive, we try to
     *                     save a bit of computation time here
     * @return The actions we could/want to do from here on.
     */
    public static Stream<RiskAction> getSuccessors(ActionNode selectedNode, RiskBoard riskBoard) {
        var game = selectedNode.getGame();
        var validActions = game.getPossibleActions();
        var tradeInActions = validActions.stream()
                .filter(RiskAction::isCardIds);
        // We can not use tradeInActions.count(), since this closes the stream)
        var onlyTradeInActions = validActions.stream().allMatch(RiskAction::isCardIds);
        if (onlyTradeInActions) {
            //We have to trade in
            return tradeInActions;
        } else {
            var atStartOfTurn = !GameUtils.isReinforcementAction(game.getPreviousAction());
            var reinforcementActions = validActions
                    .stream()
                    .filter(GameUtils::isReinforcementAction)
                    .filter(riskAction -> riskBoard.neighboringEnemyTerritories(riskAction.reinforcedId()).size() > 0);
            if (atStartOfTurn) {
                //We are at the start of our turn, we should consider using cards.
                return Stream.concat(tradeInActions, reinforcementActions);
            } else {
                //We already reinforced, if we traded in cards now we would lose the opportunity
                //to fully reinforce an already reinforced territory
                return reinforcementActions;
            }
        }
    }
}
