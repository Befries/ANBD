package network.amnesia.anbd.gameinfo;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// List of the games given for a specific search as well as the information of the which one is currently displayed
public class GameInfoList {

    /**
     * regex expression to split the json objects encapsulated in {@code {...}}. This means that the objects cannot
     * contain other sub objects encapsulated in {@code {...}}, Luckily it is not a problem as the kind of json returned
     * by the CheapShark API follow this rule.
     */
    private static final String regex = "(?s)\\{[^\\{\\}]*\\}";
    private static final String urlBase = "https://www.cheapshark.com/api/1.0/deals?";

    public static int DEFAULT_MAX_PAGE_SIZE = 60;
    public static SortingCriteria DEFAULT_SORTING_CRITERIA = SortingCriteria.REVIEWS;


    // search fields (in case we want to display them later)
    private final String searchTitle;
    private final int quantity;
    private final SortingCriteria sortingCriteria;

    // storage of gameInfos
    private final List<GameInfo> gameInfos;
    private int currentGameInfoIndex = 0;


    // uses default values
    public GameInfoList(String searchTitle) {
        this(searchTitle, DEFAULT_MAX_PAGE_SIZE, DEFAULT_SORTING_CRITERIA);
    }

    public GameInfoList(String searchTitle, int quantity, SortingCriteria sortingCriteria) {
        this.searchTitle = searchTitle;
        this.quantity = quantity;
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

    // there's probably a less stupid way to do that with JSONObjects but whatever if it works
    private Stream<String> splitJsonObjects(String jsonString) {
        return Pattern
                .compile(regex, Pattern.MULTILINE)
                .matcher(jsonString)
                .results()
                .map(MatchResult::group);
    }


    private List<GameInfo> loadGameInfos() {
        ObjectMapper jsonConverter = new ObjectMapper();
        LinkedHashMap<String, ArrayList<GameInfo>> selector = new LinkedHashMap<>();

        splitJsonObjects(fetchRemote(getLinkQuery())).map((elem) -> {
            try {
                return jsonConverter.readValue(elem, GameInfo.class);
            } catch (JsonProcessingException e) {
                System.out.println("Invalid Json object");
                return null; // if the object is invalid, remove it
            }
        }).filter(Objects::nonNull).forEach(x -> {
            selector.computeIfAbsent(x.getGameID(), r -> {
                return new ArrayList<GameInfo>();
            }).add(x);
        }); // could do this shit with a collector with some dark magic i suppose

        return selector.values().stream().reduce(new ArrayList<GameInfo>(), (buffer, current) -> {
            // get the one with the max saving, if text rating is null, is not a steam game / is soundtrack
            if (current.get(0).getSteamRatingText() == null) return buffer;
            buffer.add(current.stream()
                    .max((u,v) -> (int) ((Double.parseDouble(u.getSavings()) - Double.parseDouble(v.getSavings()))* 1000))
                    .get());
            return buffer;
        });
    }


    private String getLinkQuery() {
        return urlBase +
                "title=" + searchTitle +
                "&sortBy=" + sortingCriteria.getValue() +
                "&pageSize=" + quantity;
    }


    private String fetchRemote(String url) {
        Document document = null;
        try {
            document = Jsoup.connect(url).ignoreContentType(true).get();
        } catch (IOException e) {
            // TODO: ephemeral message if failure
            throw new RuntimeException(e);
        }
        return document.toString();
    }


    public enum SortingCriteria {

        DEAL_RATING("DealRating"),
        TITLE("Title"),
        SAVINGS("Savings"),
        PRICE("Price"),
        METACRITICS("Metacritic"),
        REVIEWS("Reviews"),
        RELEASE("Release"),
        STORE("Store"),
        RECENT("Recent");

        private final String value;
        SortingCriteria(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}
