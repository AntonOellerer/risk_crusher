import at.ac.tuwien.ifs.sge.agent.alphabetaagent.AlphaBetaAgent;
import at.ac.tuwien.ifs.sge.agent.mctsagent.MctsAgent;
import at.ac.tuwien.ifs.sge.agent.randomagent.RandomAgent;
import at.ac.tuwien.ifs.sge.leeroy.agents.Leeroy;
import at.ac.tuwien.ifs.sge.leeroy.agents.LeeroyMctsAttack;
import at.ac.tuwien.ifs.sge.leeroy.util.command.MockedMatchCommand;

import java.util.logging.Logger;

public class GameSimulator {

    private static final Logger logger = Logger.getLogger(GameSimulator.class.getName());

    public static void main(String[] args) throws Exception {
        logger.info("Starting simulation..");

        for (int i = 1; i< 11; i++) {
            System.out.println(String.format("Performing run %d", i));
            MockedMatchCommand mCmd = new MockedMatchCommand(String.format("GameRun_%02d", i));
            mCmd.setEvaluatedAgent(LeeroyMctsAttack.class);
            mCmd.setOpponentAgent(AlphaBetaAgent.class);
            mCmd.showMapOutput(false);
            mCmd.setComputationTime(15l); // max seconds per turn

            // start
            mCmd.run();
        }
        System.exit(0);
    }
}
