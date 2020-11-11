package util.command;

import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.engine.cli.SgeCommand;
import at.ac.tuwien.ifs.sge.engine.factory.AgentFactory;
import at.ac.tuwien.ifs.sge.engine.factory.GameFactory;
import at.ac.tuwien.ifs.sge.game.Game;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.List;

public class MockedSgeCommand extends SgeCommand {

    private Logger log;

    public MockedSgeCommand() throws IllegalAccessException, NoSuchFieldException {
        super();
        this.call(); // init logger + workers
        // workaround for suboptimal class design
        this.log = (Logger) FieldUtils.readField(this.getClass().getSuperclass().getDeclaredField("log"), this, true);
    }

    public Logger getLogger() {
        return this.log;
    }

    public void setGameFactory(GameFactory<Game<Object, Object>> gameFactory) throws IllegalAccessException, NoSuchFieldException {
        // workaround for suboptimal class design
        FieldUtils.writeField(this.getClass().getSuperclass().getDeclaredField("gameFactory"),  this, gameFactory, true);
    }

    public void setAgentFactories(List<AgentFactory> agentFactories) throws IllegalAccessException, NoSuchFieldException {
        // workaround for suboptimal class design
        FieldUtils.writeField(this.getClass().getSuperclass().getDeclaredField("agentFactories"),  this, agentFactories, true);
    }


}
