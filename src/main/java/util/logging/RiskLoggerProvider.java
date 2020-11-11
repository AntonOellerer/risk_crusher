package util.logging;

import java.util.HashMap;
import java.util.Map;

public class RiskLoggerProvider {

    private static RiskLoggerProvider instance;
    private Map<String, RiskLogger> loggerMap;

    private RiskLoggerProvider(){
        this.loggerMap = new HashMap<>();
    }

    public static RiskLoggerProvider getInstance() {
        if (instance == null) {
            instance = new RiskLoggerProvider();
        }
        return instance;
    }

    /**
     * returns a logging wrapper for a specific game
     * @param gameName
     * @return
     */
    public RiskLogger forGame(String gameName) {
        if (! this.loggerMap.containsKey(gameName)) {
            this.loggerMap.put(gameName, new RiskLogger(gameName));
        }
        return this.loggerMap.get(gameName);
    }
}
