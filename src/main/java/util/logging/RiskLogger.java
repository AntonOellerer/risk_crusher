package util.logging;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * used to centralize loggers for a single game simulation
 */
public class RiskLogger {

    public enum RiskLoggerType {
        TROOP_SIZE_EV,
        OCCUPIED_TERRITORY_COUNT
    }

    private Map<RiskLoggerType, Logger> loggerMap = new HashMap<>();
    private String gameName;

    public RiskLogger(String gameName) {
        this.gameName = gameName;
    }

    public Logger getRiskLogger(RiskLoggerType riskLoggerType) {
        if (! loggerMap.containsKey(riskLoggerType)) {
            Logger newLogger = Logger.getLogger(gameName + "_" + riskLoggerType.name());
            try {
                FileHandler fh = new FileHandler(newLogger.getName() + ".log");
                newLogger.addHandler(fh);
                fh.setFormatter(new CleanFormatter());
                newLogger.setUseParentHandlers(false); // no console logging
            } catch (IOException e) {
                Logger.getAnonymousLogger().severe(e.toString());
            }
            loggerMap.put(riskLoggerType, newLogger);
        }
        return loggerMap.get(riskLoggerType);
    }

}
