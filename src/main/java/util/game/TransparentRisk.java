package util.game;

import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;

/***
 * used to obtain global game information within each turn
 */
public class TransparentRisk extends Risk {

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

    public TransparentRisk(Risk risk) {
        super(risk);
    }

    public Game<RiskAction, RiskBoard> doAction(RiskAction riskAction) {
        System.out.println(getActionRecords().size());
        return new TransparentRisk((Risk) super.doAction(riskAction));
    }
}
