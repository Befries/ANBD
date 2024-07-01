package network.amnesia.anbd.gameinfo;


import network.amnesia.anbd.Utils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

// List of the games given for a specific search as well as the information of the which one is currently displayed
public class GameInfoList {


    private static final String dealUrlBase = GameLookupManager.CHEAP_SHARK_API_LINK + "/deals?";


    // search fields (in case we want to display them later)
    private final String searchTitle;
    private final SortingCriteria sortingCriteria;

    // storage of gameInfos
    private final List<GameInfo> gameInfos;
    private int currentGameInfoIndex = 0;


    public GameInfoList(String searchTitle, SortingCriteria sortingCriteria) throws IOException {
        this.searchTitle = searchTitle;
        this.sortingCriteria = sortingCriteria;
        gameInfos = loadGameInfos();
    }

    // switch gameInfoIndex, return false if we can't go further
    public boolean nextGame() {
        if (currentGameInfoIndex != gameInfos.size() - 1) {
            currentGameInfoIndex++;
            return true;
        }
        return false;
    }

    public boolean previousGame() {
        if (currentGameInfoIndex != 0) {
            currentGameInfoIndex--;
            return true;
        }
        return false;
    }

    public GameInfo getCurrentGameInfo() {
        return gameInfos.get(currentGameInfoIndex);
    }

    public int getCurrentGameInfoIndex() {
        return currentGameInfoIndex;
    }

    public int size() {
        return gameInfos.size();
    }

    public boolean hasContent() {
        return gameInfos != null;
    }

    // if no matches, return null
    private List<GameInfo> loadGameInfos() throws IOException {

        List<String> jsonObjects = Utils.splitJsonObjects(Utils.fetchRemote(getLinkQuery()));
        if (jsonObjects.isEmpty()) return null;

        return jsonObjects.stream()
                .map(GameInfo::jsonToGameInfo)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        GameInfo::getTitle,
                        gameInfo ->  gameInfo,
                        (current, next) -> current.getSavingsDouble() >= next.getSavingsDouble() ? current : next,
                        LinkedHashMap::new))
                .values().stream()
                .filter(GameInfo::isValid)
                .toList();
    }


    private String getLinkQuery() {
        return dealUrlBase +
                "title=" + searchTitle +
                "&sortBy=" + sortingCriteria.toString();
    }

}
