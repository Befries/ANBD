package network.amnesia.anbd.gameinfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import network.amnesia.anbd.Constants;
import network.amnesia.anbd.Main;
import network.amnesia.anbd.Utils;
import network.amnesia.anbd.command.Button;
import network.amnesia.anbd.command.ButtonManager;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class GameLookupManager {

    public static final String CHEAP_SHARK_API_LINK = "https://www.cheapshark.com/api/1.0";

    private static HashMap<String, String> storeMap;
    private static final HashMap<Guild, GameLookupManager> GUILDS = new HashMap<>();
    private static final String STEAM_LINK = "https://store.steampowered.com/app/";
    private static final String PREVIOUS_INDEX_HEADER = "previous-";
    private static final String NEXT_INDEX_HEADER = "next-";
    private static final int MAX_LISTS = 20;

    private final Guild guild;
    private final LinkedMap<String, GameInfoList> gameListsBuffer;

    /**
     * Stores the last {@code MAX_LISTS} {@link GameInfoList} demanded using the GameLookupCommand.
     * This means that only the last  {@code MAX_LISTS} responses will still be usable in the chat.
     */
    private GameLookupManager(Guild guild) {
        this.guild = guild;

        // Override the LinkedMap to remove the eldest element if full (removes the callBacks attached)
        // another way to achieve this is to create a wrapper around the LinkedHashMap
        gameListsBuffer = new LinkedMap<String, GameInfoList>() {
            @Override
            public GameInfoList put(String key, GameInfoList value) {
                if (size() >= MAX_LISTS) {
                    removeButtonsCallback(firstKey());
                    remove(0);
                }
                return super.put(key, value);
            }
        };
    }


    public static GameLookupManager forGuild(Guild guild) {
        return GUILDS.computeIfAbsent(guild, GameLookupManager::new);
    }

    public Guild getGuild() {
        return guild;
    }


    public static void initGameLookupManagers() {
        fetchStores();
    }

    private static void fetchStores() {
        String jsonStores;
        try {
            jsonStores = Utils.fetchRemote("https://www.cheapshark.com/api/1.0/stores");
        } catch (IOException e) {
            LogManager.getLogger().info("error arise while fetching stores' infos");
            return;
        }
        JsonNode jsonNode; // we know for fact it's an ArrayNode
        try {
            jsonNode = (ArrayNode) Utils.objectMapper.readTree(jsonStores);
        } catch (JsonProcessingException e) {
            LogManager.getLogger().info("error arise while processing stores' infos");
            return;
        }

        storeMap = new HashMap<>();
        jsonNode.forEach(subNode -> {
            storeMap.put(subNode.get("storeID").asText(), subNode.get("storeName").asText());
        });
        LogManager.getLogger().info("stores names fetched");
    }

    public void replyMessage(SlashCommandInteractionEvent event, GameInfoList gameInfoList) {
        String id = generateId();
        gameListsBuffer.put(id, gameInfoList);
        registerButtonsCallback(id, gameInfoList);

        setSwitchButtons(event.replyEmbeds(getInfoEmbed(gameInfoList)), gameInfoList, id).queue();
    }

    private void refreshMessage(ButtonInteractionEvent event, GameInfoList gameInfoList, String id) {
        setSwitchButtons(event.editMessageEmbeds(getInfoEmbed(gameInfoList)), gameInfoList, id).queue();
    }


    // reset action row each time, don't put the button that's useless
    private <T extends MessageRequest<T>> T setSwitchButtons(T reply, GameInfoList gameInfoList, String id) {
        if (gameInfoList.size() == 1) return reply;
        if (gameInfoList.getCurrentGameInfoIndex() <= 0) {
            return reply.setActionRow(Button.primary(NEXT_INDEX_HEADER + id, "️➡"));
        } else if (gameInfoList.getCurrentGameInfoIndex() >= gameInfoList.size() - 1) {
            return reply.setActionRow(Button.primary(PREVIOUS_INDEX_HEADER + id, "️⬅"));
        } else {
            return reply.setActionRow(
                    Button.primary(PREVIOUS_INDEX_HEADER + id, "️⬅"),
                    Button.primary(NEXT_INDEX_HEADER + id, "️➡")
            );
        }
    }


    private MessageEmbed getInfoEmbed(GameInfoList gameInfoList) {
        GameInfo gameInfo = gameInfoList.getCurrentGameInfo();
        EmbedBuilder builder = new EmbedBuilder();

        int indexNormalized = gameInfoList.getCurrentGameInfoIndex() + 1;
        if (storeMap == null) fetchStores();

        builder.setAuthor(indexNormalized + "/" + gameInfoList.size())
                .setTitle(gameInfo.getTitle(), STEAM_LINK + gameInfo.getSteamAppID())
                .setThumbnail(gameInfo.getThumb())
                .setDescription("cheapest with " + storeMap.get(gameInfo.getStoreID()));

        builder.addField("Normal Price", "$" + gameInfo.getNormalPrice(), true)
                .addField("Sale Price", "$" + gameInfo.getSalePrice(), true)
                .addField("Savings", gameInfo.getSavings() + "%", true)
                .addField("Steam Review", gameInfo.getSteamRatingPercent() + "%", true)
                .addField("Amount of Reviews", gameInfo.getSteamRatingCount(), true)
                .addField("", gameInfo.getSteamRatingText(), true);

        return builder.build();
    }


    // TODO: possible update, change buttons of outdated research to 'outdated buttons' which sends a reply to say they
    //       can't be used anymore >< currently says interaction failure (button doesn't have a callback anymore)
    private void registerButtonsCallback(String id, GameInfoList gameInfoList) {
        ButtonManager buttonManager = Main.getButtonManager();

        buttonManager.registerCallback(PREVIOUS_INDEX_HEADER + id, e -> {
            if (!gameInfoList.previousGame()) e.replyFormat("%s Can't go before", Constants.X_EMOTE).setEphemeral(true).queue();
            else refreshMessage(e, gameInfoList, id);
        });

        buttonManager.registerCallback(NEXT_INDEX_HEADER + id, e -> {
            if (!gameInfoList.nextGame()) e.replyFormat("%s Can't go further", Constants.X_EMOTE).setEphemeral(true).queue();
            else refreshMessage(e, gameInfoList, id);
        });
    }

    private void removeButtonsCallback(String id) {
        ButtonManager buttonManager = Main.getButtonManager();
        buttonManager.removeCallback(PREVIOUS_INDEX_HEADER + id);
        buttonManager.removeCallback(NEXT_INDEX_HEADER + id);
    }

    // let jesus take the wheel, it's a miracle if we get the same id
    private String generateId() {
        return UUID.randomUUID().toString();
    }

}
