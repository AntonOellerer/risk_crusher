package util.game;

import java.util.List;

public class TerritoryNames {
    private static List<String> territoryNames = List.of("ALASKA", "ALBERTA", "CENTRAL_AMERICA", "EASTERN_UNITED_STATES", "GREENLAND", "NORTHWEST_TERRITORY", "ONTARIO", "QUEBEC", "WESTERN_UNITED_STATES", "ARGENTINA", "BRAZIL", "PERU", "VENEZUELA", "GREAT_BRITAIN", "ICELAND", "NORTHERN_EUROPE", "SCANDINAVIA", "SOUTHERN_EUROPE", "UKRAINE", "WESTERN_EUROPE", "CENTRAL_AFRICA", "EAST_AFRICA", "EGYPT", "MADAGASCAR", "NORTH_AFRICA", "SOUTH_AFRICA", "AFGHANISTAN", "CHINA", "INDIA", "IRKUTSK", "JAPAN", "KAMCHATKA", "MIDDLE_EAST", "MONGOLIA", "SIAM", "SIBERIA", "URAL", "YAKUTSK", "EASTERN_AUSTRALIA", "INDONESIA", "NEW_GUINEA", "WESTERN_AUSTRALIA");

    public static String of(int territoryIndex) {
        return territoryNames.get(territoryIndex);
    }
}
