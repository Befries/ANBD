package network.amnesia.anbd.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import network.amnesia.anbd.command.Command;
import network.amnesia.anbd.command.CommandCategory;
import network.amnesia.anbd.command.ICommand;
import network.amnesia.anbd.gameinfo.GameLookupManager;

@ICommand(name = "gamelookup", category = CommandCategory.INFO, description = "Get a list of widgets describing games matching the search")
public class GameLookupCommand extends Command {

    // Create the GameInfoList and register it to the GameLookupManager
    public Outcome invoke(SlashCommandInteractionEvent event, String gameTitle) {
        GameLookupManager gameLookupManager = GameLookupManager.forGuild(event.getGuild());

        return Outcome.SUCCESS;
    }





    @Override
    public SlashCommandData getCommandData() {
        return super.getCommandData()
                .addOption(OptionType.STRING, "game title", "The title of the game you want to know about", true)
                .addOption(OptionType.INTEGER, "amount", "quantity of games with similar names to fetch", false);
    }
}
