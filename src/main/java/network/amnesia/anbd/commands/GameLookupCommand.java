package network.amnesia.anbd.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import network.amnesia.anbd.command.Command;
import network.amnesia.anbd.command.CommandCategory;
import network.amnesia.anbd.command.ICommand;
import network.amnesia.anbd.gameinfo.GameInfoList;
import network.amnesia.anbd.gameinfo.GameLookupManager;

@ICommand(name = "gamelookup", category = CommandCategory.INFO, description = "Get a list of widgets describing games matching the search")
public class GameLookupCommand extends Command {


    public Outcome invoke(SlashCommandInteractionEvent event, String title) {
        GameLookupManager gameLookupManager = GameLookupManager.forGuild(event.getGuild());
        GameInfoList gameInfoList = new GameInfoList(title);
        gameLookupManager.replyMessage(event, gameInfoList);
        return Outcome.SUCCESS;
    }


    @Override
    public SlashCommandData getCommandData() {
        return super.getCommandData()
                .addOption(OptionType.STRING, "title", "The title of the game you want to know about", true)
                .addOption(OptionType.INTEGER, "amount", "quantity of games with similar names to fetch", false);
    }
}
