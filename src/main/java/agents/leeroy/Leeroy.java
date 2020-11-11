package agents.leeroy;

import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import phase.Phase;
import phase.PhaseUtils;

import java.util.concurrent.TimeUnit;

public class Leeroy<G extends Game<A, RiskBoard>, A> extends AbstractGameAgent<G, A> implements GameAgent<G, A> {
    Phase currentPhase = Phase.INITIAL_SELECT;
    private int playerNumber;
    private int numberOfPlayers;

    public Leeroy(Logger log) {
        super(log);
    }

    @Override
    public A computeNextAction(G game, long computationTime, TimeUnit timeUnit) {
        super.setTimers(computationTime, timeUnit);
        Risk risk = (Risk) game;
        setPhase(risk);
        return null;
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

    @Override
    public void setUp(int numberOfPlayers, int playerNumber) {
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
