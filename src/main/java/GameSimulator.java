import agents.leeroy.Leeroy;
import at.ac.tuwien.ifs.sge.agent.randomagent.RandomAgent;
import util.command.MockedMatchCommand;

import java.util.logging.Logger;

public class GameSimulator {

    private static final Logger logger = Logger.getLogger(GameSimulator.class.getName());

    public static void main(String[] args) throws Exception {
        logger.info("Starting simulation..");

        // note: no multithreading on my pc :-(
        for (int i = 1; i<= 10; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    MockedMatchCommand mCmd = new MockedMatchCommand(String.format("GameRun_%02d", finalI));
                    mCmd.setEvaluatedAgent(Leeroy.class);
                    mCmd.setOpponentAgent(RandomAgent.class);
                    mCmd.showMapOutput(false);

                    // start
                    mCmd.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).run();
        }
    }
}
