package util.game;

import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import util.logging.RiskLogger;
import util.logging.RiskLoggerProvider;

import static util.logging.RiskLogger.RiskLoggerType.GLOBAL_TROOP_SIZE_EV;

/***
 * used to obtain global game information within each turn
 */
public class TransparentRisk extends Risk {

    private String gameName;

    /**
     * required for reflection
     * @param yaml
     * @param numberOfPlayers
     */
    public TransparentRisk(String yaml, int numberOfPlayers) {
        super(yaml, numberOfPlayers);
    }

    /***
     * required for reflection
     */
    public TransparentRisk() {
        super();
    }

    public TransparentRisk(Risk risk, String gameName)
    {
        super(risk);
        this.gameName = gameName;
    }

    public Game<RiskAction, RiskBoard> doAction(RiskAction riskAction) {
        int actionNr = getActionRecords().size();
        RiskLogger rlp = RiskLoggerProvider.getInstance().forGame(this.gameName);
        rlp.getRiskLogger(GLOBAL_TROOP_SIZE_EV).info(actionNr+ "");
        return new TransparentRisk((Risk) super.doAction(riskAction), this.gameName);
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }
}
