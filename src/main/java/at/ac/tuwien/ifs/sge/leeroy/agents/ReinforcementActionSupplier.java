package at.ac.tuwien.ifs.sge.leeroy.agents;

import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import at.ac.tuwien.ifs.sge.leeroy.mcts.ActionNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class contains the methods for generating reinforcement actions (at the start of the players turn)
 * For our agent, we just want to reinforce territories which are neighbour to an enemy territory.
 * If the heap is large enough, we can use the improved heuristic to find beneficial
 * reinforcement actions, if not we fall back to selecting actions at random.
 */
public class ReinforcementActionSupplier {
    private static final int BRANCHING_FACTOR = 3;
    private static final int NEEDED_HEAP_GB = 8;
    private static final boolean CAN_USE_IMPROVED_HEURISTIC = Runtime.getRuntime().maxMemory() > NEEDED_HEAP_GB * Math.pow(10, 9);
    private static final Random randomizer = new Random();


    /**
     * Get the reinforcement actions
     *
     * @param selectedNode The node to expand
     * @param riskBoard    The board status at the selected node.
     *                     Since calculating it anew is very expensive, we try to
     *                     save a bit of computation time here by caching it per turn for the whole algorithm
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
            Stream<RiskAction> reinforcementActions;
            if (CAN_USE_IMPROVED_HEURISTIC) {
                var allPossibleActions = new HashSet<>(game.getPossibleActions());
                var allTerritories = new HashMap<>(riskBoard.getTerritories());
                HashSet<RiskAction> reinforcementActionsSet = new HashSet<>();
                while (reinforcementActionsSet.size() < BRANCHING_FACTOR && reinforcementActionsSet.size() < allPossibleActions.size()) {
                    var action = HeuristicReinforce.reinforce(game.getCurrentPlayer(), riskBoard, allPossibleActions, allTerritories);
                    reinforcementActionsSet.add(action);
                    allPossibleActions.remove(action);
                    allTerritories.remove(action.reinforcedId());
                }
                reinforcementActions = reinforcementActionsSet.stream();
            } else {
                var reinforcementActionsList = validActions
                        .stream()
                        .filter(GameUtils::isReinforcementAction)
                        .filter(riskAction -> riskBoard.neighboringEnemyTerritories(riskAction.reinforcedId()).size() > 0)
                        .collect(Collectors.toList());
                var reinforcementActionsSet = new LinkedList<RiskAction>();
                while (reinforcementActionsSet.size() < BRANCHING_FACTOR && reinforcementActionsSet.size() != reinforcementActionsList.size()) {
                    reinforcementActionsSet.add(reinforcementActionsList.get(randomizer.nextInt(reinforcementActionsList.size())));
                }
                reinforcementActions = reinforcementActionsSet.stream();
            }
            var atStartOfTurn = !GameUtils.isReinforcementAction(game.getPreviousAction());
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
