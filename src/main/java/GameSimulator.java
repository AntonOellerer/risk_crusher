import agents.leeroy.Leeroy;
import at.ac.tuwien.ifs.sge.agent.alphabetaagent.AlphaBetaAgent;
import at.ac.tuwien.ifs.sge.agent.mctsagent.MctsAgent;
import at.ac.tuwien.ifs.sge.agent.randomagent.RandomAgent;
import util.command.MockedMatchCommand;

import java.util.logging.Logger;

public class GameSimulator {

    private static final Logger logger = Logger.getLogger(GameSimulator.class.getName());

    public static void main(String[] args) throws Exception {
        logger.info("Starting simulation..");


        MockedMatchCommand mCmd = new MockedMatchCommand(String.format("GameRun_%02d", 1));
        mCmd.setEvaluatedAgent(Leeroy.class);
        mCmd.setOpponentAgent(AlphaBetaAgent.class);
        mCmd.showMapOutput(false);
        mCmd.setComputationTime(30l); // max seconds per turn

        // start
        mCmd.run();
        System.exit(0);
    }
}
