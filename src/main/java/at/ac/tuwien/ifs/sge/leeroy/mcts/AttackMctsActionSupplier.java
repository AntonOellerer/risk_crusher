package at.ac.tuwien.ifs.sge.leeroy.mcts;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import at.ac.tuwien.ifs.sge.leeroy.agents.AttackActionSupplier;
import at.ac.tuwien.ifs.sge.leeroy.agents.GameUtils;
import at.ac.tuwien.ifs.sge.leeroy.agents.OccupyActionSupplier;
import at.ac.tuwien.ifs.sge.leeroy.agents.ReinforcementActionSupplier;
import at.ac.tuwien.ifs.sge.util.Util;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AttackMctsActionSupplier extends MctsActionSupplier{

    /**
     * magic numbers for optimal action selection
     */
    private static final double OCCUPATION_FACTOR = 20; // number of our occupied territories
    private static final double FRONTLINE_PENALTY_FACTOR = -.25; // number of enemy territories on the frontline
    private static final double FRONTLINE_MARGIN_FACTOR = 0.75; // margin of frontline troops (ours minus theirs)
    private static final double CONTINENT_BONUS_FACTOR = 100; // bonus for occupied continents
    private static final double ENEMY_CONTINENT_PENALTY_FACTOR = -40; // malus for enemy occupied continents
    private static final double UNUSED_TROOPS_PENALTY_FACTOR = -0.5; // malus for inefficient attack/occupy

    private final static int MAX_ATTACK_TROOPS = 3;

    public AttackMctsActionSupplier(BooleanSupplier shouldStopComputation) {
        super(shouldStopComputation);
    }

    /**
     * first expand territories with max defending troops -> attacker looses less troops -> maximize expected value
     * ( i.e. difference of troops lost)
     * @return
     */
    @Override
    Function<ActionNode, ActionNode> getNodeSelectionFunction() {
        return (node) -> {
            RiskBoard board = node.getBoard();

            return Collections.max(node.getSuccessors(),
                    Comparator.comparingInt(nodeToEvaluate ->
                       board.getTerritoryTroops(nodeToEvaluate.getAction().defendingId())
                    ));
            };
    }

    @Override
    Function<ActionNode, Integer> getEvaluationFunction() {
        return (node) -> {
            Risk game = node.getGame();
            RiskBoard board = node.getBoard();
            int activePlayer = game.getCurrentPlayer();

            Set<Integer> occupiedTerritories = board.getTerritoriesOccupiedByPlayer(activePlayer);
            Set<Integer> neighbouringEnemyTerritories = GameUtils.getEnemyNeighbors(occupiedTerritories, board);

            double occupationBonus = occupiedTerritories.size() * OCCUPATION_FACTOR;
            double frontlineCntMalus = neighbouringEnemyTerritories.size() * FRONTLINE_PENALTY_FACTOR;
            double frontlineMarginFactor = GameUtils.getFrontlineMargin(occupiedTerritories, board) * FRONTLINE_MARGIN_FACTOR;
            double continentBonus = GameUtils.getContinentBonusForPlayer(activePlayer, board) * CONTINENT_BONUS_FACTOR;
            double enemyContinentMalus = GameUtils.getTotalContinentMalus(activePlayer, board) * ENEMY_CONTINENT_PENALTY_FACTOR;
            double unusedTroopsMalus = GameUtils.getUnusedTroops(occupiedTerritories, board) * UNUSED_TROOPS_PENALTY_FACTOR;

            return Math.toIntExact(Math.round(occupationBonus + frontlineCntMalus +
                    frontlineMarginFactor + continentBonus + enemyContinentMalus + unusedTroopsMalus));
        };
    }

    @Override
    int simulateGame(ActionNode explorationNode) {
        var currentNode = explorationNode;
        while (! this.shouldStopComputation.getAsBoolean()) {
            if (currentNode.getSuccessors() == null) {
                // expand further
                getSuccessors(currentNode);
            }
            if (currentNode.getSuccessors().isEmpty()) {
                // no further expansion possible
                break;
            }
            if (isCasualtyPhase(currentNode.getGame(), currentNode.getBoard()) || GameUtils.isReinforcementAction(currentNode.getAction())) {
                // for casualty simulation we take a random successor, not the best
                // For reinforcement, I did not yet find a good evaluation
                // function, a possible one would be to try to avoid
                // completely overpowering single enemy territories, so
                // we do not end up w/ strong territories far away from
                // the next enemy.
                currentNode = Util.selectRandom(currentNode.getSuccessors());
            } else {
                //TODO: @Martin If I got this correctly this is also used in
                //the fortification phase, where the number of troops on the
                //board does not change
                currentNode = getNodeSelectionFunction().apply(currentNode);
            }
            //This should prevent NPEs when we try to find the actually
            //executed node in CachedMctsLeeroy
            if (currentNode.getSuccessors() == null) {
                // expand further
                getSuccessors(currentNode);
            }
        }
        return getEvaluationFunction().apply(currentNode);
    }

    /**
     * reuses the static attack action supplier
     * valid attack actions are:
     *  - only with expected win chances > 0.5
     *  - all-out-attacks ( no reason to attack with less than 3 troops if more is possible)
     * @param selectedNode
     * @return
     */
    @Override
    List<ActionNode> getSuccessors(ActionNode selectedNode) {
        if (selectedNode.getSuccessors() != null) {
            return selectedNode.getSuccessors();
        }
        if (selectedNode.isLeafNode()) {
            selectedNode.setSuccessors(new ArrayList<>());
            return new ArrayList<>();
        }

        List<ActionNode> successors;
        RiskBoard board = selectedNode.getBoard();
        if (selectedNode.getGame().isGameOver()) {
            // no more actions
            successors = List.of();
        } else if (board.isOccupyPhase()) {
            // if we simulated the attack action we pass it to the action supplier - otherwise we fetch it from the history (takes more time)
            RiskAction attackAction = selectedNode.getParent().isPresent() ? selectedNode.getParent().get().getAction() : null;
            Set<RiskAction> occupyActions = attackAction != null ?
                    OccupyActionSupplier.createActions(selectedNode.getGame(), board, attackAction) :
                    OccupyActionSupplier.createActions(selectedNode.getGame(), board);

            successors = occupyActions
                    .stream()
                    .map(ra -> new ActionNode(selectedNode.getPlayer(), selectedNode, (Risk) selectedNode.getGame().doAction(ra), ra))
                    .collect(Collectors.toList());
        } else if (isCasualtyPhase(selectedNode.getGame(), board)) {
            successors = selectedNode.getGame().getPossibleActions()
                    .stream()
                    .map(ra -> new ActionNode(selectedNode.getPlayer(), selectedNode, (Risk) selectedNode.getGame().doAction(ra), ra))
                    .collect(Collectors.toList());
        } else if (board.isReinforcementPhase()) {
            successors = ReinforcementActionSupplier.getSuccessors(selectedNode, board)
                    .map(riskAction -> new ActionNode(selectedNode.getPlayer(), selectedNode, (Risk) selectedNode.getGame().doAction(riskAction), riskAction))
                    .collect(Collectors.toList());
        } else {
            successors = AttackActionSupplier
                    .createActions(selectedNode.getGame(), board, MAX_ATTACK_TROOPS)
                    .stream()
                    .map(ra -> new ActionNode(selectedNode.getPlayer(), selectedNode, (Risk) selectedNode.getGame().doAction(ra), ra))
                    .collect(Collectors.toList());
            successors.add(new ActionNode(selectedNode.getPlayer(), selectedNode, (Risk) selectedNode.getGame().doAction(RiskAction.endPhase()), RiskAction.endPhase()));
        }

        selectedNode.setSuccessors(successors);

        return successors;
    }

    boolean isCasualtyPhase(Risk game, RiskBoard board) {
        return board.isAttackPhase() && game.getCurrentPlayer() == -6;
    }

//    /**
//     * attack is a multi-step action int the game engine
//     * @param game
//     * @param action
//     * @return
//     */
//    Risk applyActionToGame(Risk game, RiskAction action) {
//        if (action.isEndPhase() || game.getBoard().isOccupyPhase()) {
//            return (Risk) game.doAction(action);
//        }
//
//        // if attack is done we also need to simulate casualties which were also modelled as phase
//        game = (Risk) game.doAction(action); // attack
//        game = (Risk) game.doAction(Util.selectRandom(game.getPossibleActions())); // casualties are random
//        return game;
//    }
}
