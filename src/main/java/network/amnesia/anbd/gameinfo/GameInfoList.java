package network.amnesia.anbd.gameinfo;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
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

    public static int DEFAULT_PAGE_SIZE = 10;
    public static SortingCriteria DEFAULT_SORTING_CRITERIA = SortingCriteria.REVIEWS;


    // search fields (in case we want to display them later)
    private final String searchTitle;
    private final int quantity;
    private final SortingCriteria sortingCriteria;

    // storage of gameInfos
    private final List<GameInfo> gameInfos;

    // TODO: selector to know which game info is displayed
    //       and all the query needs of the GameInfoManager


    // there's probably a less stupid way to do that with JSONObjects but whatever if it works
    private Stream<String> splitJsonObjects(String jsonString) {
        return Pattern
                .compile(regex, Pattern.MULTILINE)
                .matcher(jsonString)
                .results()
                .map(MatchResult::group);
    }

    // uses default values
    public GameInfoList(String searchTitle) {
        this(searchTitle, DEFAULT_PAGE_SIZE, DEFAULT_SORTING_CRITERIA);
    }

    public GameInfoList(String searchTitle, int quantity, SortingCriteria sortingCriteria) {
        this.searchTitle = searchTitle;
        this.quantity = quantity;
        this.sortingCriteria = sortingCriteria;
        gameInfos = loadGameInfos();
    }


    private List<GameInfo> loadGameInfos() {
        ObjectMapper jsonConverter = new ObjectMapper();
        return splitJsonObjects(fetchRemote(getLinkQuery())).map((elem) -> {
            try {
                return jsonConverter.readValue(elem, GameInfo.class);
            } catch (JsonProcessingException e) {
                System.out.println("Invalid Json object");
                return null; // if the object is invalid, remove it
            }
        }).filter(Objects::nonNull).toList();
    }


    private String getLinkQuery() {
        return urlBase +
                "title=" + searchTitle +
                "&sortBy=" + sortingCriteria.getValue() +
                "&pageSize=" + quantity;
    }


    private String fetchRemote(String link) {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        RequestBody body = RequestBody.create(MediaType.parse("text/plain"), "");
        Request request = new Request.Builder()
                .url(link)
                .method("GET", body)
                .build();

        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            // TODO: setup ephemeral message if failure
            // temporary solution
            System.out.println("problem while requesting data to the CheapShark API");
        }

        // debug:
        System.out.println(response.message());
        return response.message();
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
