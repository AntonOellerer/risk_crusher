package at.ac.tuwien.ifs.sge.leeroy.util.heuristics;

import at.ac.tuwien.ifs.sge.game.risk.configuration.RiskConfiguration;
import at.ac.tuwien.ifs.sge.game.risk.configuration.RiskContinentConfiguration;
import at.ac.tuwien.ifs.sge.game.risk.configuration.RiskTerritoryConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TerritoryBonusProvider {

    private static TerritoryBonusProvider instance;
    private Map<Integer, Integer> bonusMap = new HashMap<>(); // how much bonus does each continent give
    private Map<Integer, Integer> sizeMap = new HashMap<>(); // how many territories can be found in each country

    private TerritoryBonusProvider() {
        for (RiskContinentConfiguration rcc : RiskConfiguration.RISK_DEFAULT_CONFIG.getContinents()) {
            bonusMap.put(rcc.getContinentId(), rcc.getTroopBonus());
        }
        for (RiskTerritoryConfiguration rtc : RiskConfiguration.RISK_DEFAULT_CONFIG.getTerritories()) {
            if (!sizeMap.containsKey(rtc.getContinentId())) {
                sizeMap.put(rtc.getContinentId(), 0);
            }
            sizeMap.put(rtc.getContinentId(), sizeMap.get(rtc.getContinentId())+1);
        }
    }

    public static TerritoryBonusProvider getInstance() {
        if (instance == null) {
            instance = new TerritoryBonusProvider();
        }
        return instance;
    }

    public Integer getTerritoryBonus(int continentId) {
        return bonusMap.get(continentId);
    }

    public Integer getTerritorySize(int continentId) {
        return sizeMap.get(continentId);
    }

    public Set<Integer> getContinentIds() {
        return sizeMap.keySet();
    }
}
