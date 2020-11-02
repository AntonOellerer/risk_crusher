package util.factory;

import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.engine.factory.GameFactory;
import at.ac.tuwien.ifs.sge.game.Game;
import util.game.TransparentRisk;

import java.lang.reflect.Constructor;

public class NamedGameFactory<G extends Game<?, ?>>  extends GameFactory {

    private String gameName;

    public NamedGameFactory(Constructor gameConstructor, int minimumNumberOfPlayers, int maximumNumberOfPlayers, Logger log, String gameName) {
        super(gameConstructor, minimumNumberOfPlayers, maximumNumberOfPlayers, log);
        this.gameName = gameName;
    }

    public G newInstance(Object... args){
        G game = (G) super.newInstance(args);
        if (game instanceof TransparentRisk) {
            ((TransparentRisk)game).setGameName(this.gameName);
        }
        return game;
    }


}
