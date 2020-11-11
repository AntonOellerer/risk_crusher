package agents.leeroy;

public class GameUtils {
    public static int getNumberOfStartingTroops(int players) {
        switch (players) {
            case 2:
                return 50;
            case 3:
                return 35;
            case 4:
                return 30;
            case 5:
                return 25;
            case 6:
                return 20;
            default:
                throw new IllegalArgumentException();
        }
    }
}
