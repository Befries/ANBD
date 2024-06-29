package network.amnesia.anbd.gameinfo;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class GameLookupManager {

    private static final HashMap<Guild, GameLookupManager> GUILDS = new HashMap<>();
    private static final int MAX_LISTS = 10;

    private final Guild guild;
    private final LinkedHashMap<Message, GameInfoList> gameListsBuffer;

    /**
     * Stores the last {@code MAX_LISTS} {@link GameInfoList} demanded using the GameLookupCommand.
     * This means that only the last  {@code MAX_LISTS} responses will still be usable in the chat.
     */
    private GameLookupManager(Guild guild) {
        this.guild = guild;
        gameListsBuffer = new LinkedHashMap<Message, GameInfoList>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > MAX_LISTS;
            }
        };
    }


    public static GameLookupManager forGuild(Guild guild) {
        return GUILDS.computeIfAbsent(guild, GameLookupManager::new);
    }

    public Guild getGuild() {
        return guild;
    }


    public void registerGameInfoList(Message linkedMessage, GameInfoList gameInfoList) {
        gameListsBuffer.put(linkedMessage, gameInfoList);
    }

}
