package util.command;

import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.engine.cli.MatchCommand;
import at.ac.tuwien.ifs.sge.engine.cli.SgeCommand;
import at.ac.tuwien.ifs.sge.engine.factory.AgentFactory;
import at.ac.tuwien.ifs.sge.engine.factory.GameFactory;
import at.ac.tuwien.ifs.sge.engine.loader.GameLoader;
import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import org.apache.commons.lang3.reflect.FieldUtils;
import util.factory.NamedGameFactory;
import util.game.TransparentRisk;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class MockedMatchCommand <T extends GameAgent> extends MatchCommand {

    private MockedSgeCommand sgeCmd;

    private Class gameClass = TransparentRisk.class;

    private Class<T> evaluatedAgentClass;

    private Class<T> opponentAgentClass;

    private String gameName;

    public MockedMatchCommand(String gameName) throws IllegalAccessException, NoSuchFieldException {
        super();
        sgeCmd = new MockedSgeCommand();
        this.gameName = gameName;
        FieldUtils.writeField(this.getClass().getSuperclass().getDeclaredField("sge"),this, sgeCmd, true);
    }

    protected void loadCommon() {
        try {
            sgeCmd.setGameFactory(this.initRiskGameFactory());
            sgeCmd.setAgentFactories(this.initAgentFactories());
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                InvocationTargetException | NoSuchFieldException e) {
            e.printStackTrace();
        }

        this.loadDebug();
        this.loadLogLevel();
        this.loadArguments();
        // this.loadFiles(); // we don't load config from files
        this.loadBoard();
    }

    public SgeCommand getSge() {
        return  this.sgeCmd;
    }

    public void setEvaluatedAgent(Class<T> agentClass) {
        this.evaluatedAgentClass = agentClass;
    }

    public void setOpponentAgent(Class<T> agentClass) {
        this.opponentAgentClass = agentClass;
    }

    public void showMapOutput(boolean showMapOutput) {
        this.sgeCmd.getLogger().setLogLevel(showMapOutput ? 0 : 1);
    }

    public void setComputationTime(long computationTimeSeconds) throws NoSuchFieldException, IllegalAccessException {
        FieldUtils.writeField(this.getClass().getSuperclass().getDeclaredField("computationTime"),this, computationTimeSeconds, true);
    }

    private GameFactory<Game<Object, Object>> initRiskGameFactory() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<Game<Object, Object>> gameConstructor = gameClass.getConstructor(String.class, Integer.TYPE);
        Constructor<Game<Object, Object>> gameConstructorWithoutPlayerNumber = gameClass.getConstructor();
        Game<?, ?> testGame = gameConstructorWithoutPlayerNumber.newInstance();
        return new NamedGameFactory<>(gameConstructor, testGame.getMinimumNumberOfPlayers(), testGame.getMaximumNumberOfPlayers(), this.sgeCmd.getLogger(), this.gameName);
    }

    private List<AgentFactory> initAgentFactories() throws NoSuchMethodException {
        List<AgentFactory> agentFactories = new ArrayList<>();

        agentFactories.add(new AgentFactory(
                "evaluatedAgent_"+evaluatedAgentClass.getSimpleName(),
                (Constructor<GameAgent<Game<Object, Object>, Object>>) evaluatedAgentClass.getConstructor(Logger.class),
                this.sgeCmd.getLogger()));

        agentFactories.add(new AgentFactory(
                "opponentAgent_"+evaluatedAgentClass.getSimpleName(),
                (Constructor<GameAgent<Game<Object, Object>, Object>>) opponentAgentClass.getConstructor(Logger.class),
                this.sgeCmd.getLogger()));

        return agentFactories;
    }


}
