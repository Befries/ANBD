package network.amnesia.anbd.gameinfo;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import network.amnesia.anbd.Constants;
import network.amnesia.anbd.Main;
import network.amnesia.anbd.command.Button;
import network.amnesia.anbd.command.ButtonManager;
import org.apache.commons.collections4.map.LinkedMap;

import java.util.HashMap;
import java.util.UUID;

public class GameLookupManager {

    private static final HashMap<Guild, GameLookupManager> GUILDS = new HashMap<>();
    private static final String STEAM_LINK = "https://store.steampowered.com/app/";
    private static final String PREVIOUS_INDEX_HEADER = "previous-";
    private static final String NEXT_INDEX_HEADER = "next-";
    private static final int MAX_LISTS = 10;

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

    // TODO: remove buttons if end of list
    public void replyMessage(SlashCommandInteractionEvent event, GameInfoList gameInfoList) {
        String id = generateId();
        gameListsBuffer.put(id, gameInfoList);

        registerButtonsCallback(id, gameInfoList);
        event.replyEmbeds(getInfoEmbed(gameInfoList)).setActionRow(
                Button.primary(PREVIOUS_INDEX_HEADER + id, "️⬅"),
                Button.primary(NEXT_INDEX_HEADER + id, "️➡")
        ).queue();
    }


    private void refreshMessage(String id, GameInfoList gameInfoList, ButtonInteractionEvent event) {
        event.editMessageEmbeds(getInfoEmbed(gameInfoList)).setActionRow(
                Button.primary(PREVIOUS_INDEX_HEADER + id, "️⬅"),
                Button.primary(NEXT_INDEX_HEADER + id, "️➡")
        ).queue();
    }


    private MessageEmbed getInfoEmbed(GameInfoList gameInfoList) {
        GameInfo gameInfo = gameInfoList.getCurrentGameInfo();
        EmbedBuilder builder = new EmbedBuilder();

        int indexNormalized = gameInfoList.getCurrentGameInfoIndex() + 1;

        builder.setAuthor(indexNormalized + "/" + gameInfoList.size())
                .setTitle(gameInfo.getTitle(), STEAM_LINK + gameInfo.getSteamAppID())
                .setThumbnail(gameInfo.getThumb());

        // TODO: add the cheapest stores (storeId to link with the store name, load stores on bot startup)
        builder.addField("Normal Price", "$" + gameInfo.getNormalPrice(), true)
                .addField("Sale Price", "$" + gameInfo.getSalePrice(), true)
                .addField("Savings", gameInfo.getSavings() + "%", true)
                .addField("Steam Review", gameInfo.getSteamRatingPercent() + "%", true)
                .addField("Amount of Reviews", gameInfo.getSteamRatingCount(), true)
                .addField("", gameInfo.getSteamRatingText(), true);

        return builder.build();
    }


    // TODO: add price lookup button to get a more precise knowledge of the element (potential feature)
    private void registerButtonsCallback(String id, GameInfoList gameInfoList) {
        ButtonManager buttonManager = Main.getButtonManager();

        buttonManager.registerCallback(PREVIOUS_INDEX_HEADER + id, e -> {
            if (!gameInfoList.previousGame()) e.replyFormat("%s Can't go before", Constants.X_EMOTE).setEphemeral(true).queue();
            else refreshMessage(id, gameInfoList, e);
        });

        buttonManager.registerCallback(NEXT_INDEX_HEADER + id, e -> {
            if (!gameInfoList.nextGame()) e.replyFormat("%s Can't go further", Constants.X_EMOTE).setEphemeral(true).queue();
            else refreshMessage(id, gameInfoList, e);
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
