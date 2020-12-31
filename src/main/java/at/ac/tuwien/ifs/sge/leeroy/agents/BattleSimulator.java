package at.ac.tuwien.ifs.sge.leeroy.agents;

import java.util.Arrays;
import java.util.List;

/**
 * based on https://www4.stat.ncsu.edu/~jaosborn/research/RISK.pdf
 */
public class BattleSimulator {

    private final static List<? extends List<? extends Number>> smallBattleProbabilities = Arrays.asList(
            Arrays.asList(0.417, 0.106, 0.027, 0.007, 0.002, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            Arrays.asList(0.754, 0.363, 0.206, 0.091, 0.049, 0.021, 0.011, 0.005, 0.003, 0.001),
            Arrays.asList(0.916, 0.656, 0.47, 0.315, 0.206, 0.134, 0.084, 0.054, 0.033, 0.021),
            Arrays.asList(0.972, 0.785, 0.642, 0.477, 0.359, 0.253, 0.181, 0.123, 0.086, 0.057),
            Arrays.asList(0.990, 0.890, 0.769, 0.638, 0.506, 0.397, 0.297, 0.224, 0.162, 0.118),
            Arrays.asList(0.997, 0.934, 0.857, 0.745, 0.638, 0.521, 0.423, 0.329, 0.258, 0.193),
            Arrays.asList(0.999, 0.967, 0.91, 0.834, 0.736, 0.64, 0.536, 0.446, 0.357, 0.287),
            Arrays.asList(1.0, 0.98, 0.947, 0.888, 0.818, 0.73, 0.643, 0.547, 0.464, 0.38),
            Arrays.asList(1.0, 0.99, 0.967, 0.93, 0.873, 0.808, 0.726, 0.646, 0.558, 0.48),
            Arrays.asList(1.0, 0.994, 0.981, 0.954, 0.916, 0.861, 0.8, 0.724, 0.65, 0.568)
    );

    public static double getWinProbability(int attackTroops, int defendTroops) {
        if (attackTroops < 1 || (defendTroops / attackTroops * 1.0 > 10)) {
            return 0;
        } else if (defendTroops < 1 || (attackTroops / defendTroops * 1.0 > 10)) {
            return 1;
        }

        if (attackTroops < 11 && defendTroops < 11) {
            return (double) smallBattleProbabilities.get(attackTroops-1).get(defendTroops-1);
        }

        // stupid heuristic which needs to be revised maybe
        // based on https://boardgames.stackexchange.com/questions/3514/how-can-i-estimate-my-chances-to-win-a-risk-battle
        final double expectedLossDef = 1.08;
        final double expectedLossAtt = 0.922;
        if ((attackTroops / expectedLossAtt) > (defendTroops / expectedLossDef)) {
            return getWinProbability(10, (int) Math.floor(defendTroops - (attackTroops - 10) / expectedLossAtt * expectedLossDef));
        }

        return getWinProbability((int)Math.floor (attackTroops - (defendTroops - 10) / expectedLossDef * expectedLossAtt), 10);
    }

}
