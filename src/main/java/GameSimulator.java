import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.agent.randomagent.RandomAgent;
import at.ac.tuwien.ifs.sge.engine.cli.MatchCommand;
import util.command.MockedMatchCommand;

import java.util.logging.Logger;

public class GameSimulator {

    private static Logger logger = Logger.getLogger(GameSimulator.class.getName());

    public static void main(String[] args) throws Exception {
        logger.info("Starting simulation..");

        MockedMatchCommand mCmd = new MockedMatchCommand();
        mCmd.setEvaluatedAgent(RandomAgent.class);
        mCmd.setOpponentAgent(RandomAgent.class);

        // start
        mCmd.run();

        /*Match<Game<Object, Object>, GameAgent<Game<Object, Object>, Object>, Object> match  = new Match<>(
                this.sge.gameFactory.newInstance(new Object[]{this.board, this.numberOfPlayers}),
                agentList,
                this.computationTime,
                this.timeUnit,
                this.sge.debug,
                this.sge.log,
                this.sge.pool);*/
    }
}
