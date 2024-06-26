package network.amnesia.anbd.music;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.AndroidWithThumbnail;
import dev.lavalink.youtube.clients.MusicWithThumbnail;
import dev.lavalink.youtube.clients.WebWithThumbnail;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import network.amnesia.anbd.Main;
import network.amnesia.anbd.configs.ConfigManager;

import java.util.HashMap;

public class MusicManager {

    private static final HashMap<Guild, MusicManager> GUILDS = new HashMap<>();
    private final Guild guild;

    /**
     * Audio player for the guild.
     */
    private final AudioPlayer player;
    /**
     * Track scheduler for the player.
     */
    private final TrackScheduler scheduler;

    private MusicManager(Guild guild) {
        this.guild = guild;

        player = getAudioPlayerManager().createPlayer();
        scheduler = new TrackScheduler(this, player);
        player.addListener(scheduler);

        guild.getAudioManager().setSendingHandler(getSendHandler());
    }

    public static MusicManager forGuild(Guild guild) {
        return GUILDS.computeIfAbsent(guild, MusicManager::new);
    }

    public static void registerSources() {
        AudioSourceManagers.registerLocalSource(Main.getAudioPlayerManager());

        Main.getAudioPlayerManager().registerSourceManager(new YoutubeAudioSourceManager(true, new MusicWithThumbnail(), new WebWithThumbnail(), new AndroidWithThumbnail()));
        Main.getAudioPlayerManager().registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        Main.getAudioPlayerManager().registerSourceManager(new BandcampAudioSourceManager());
        Main.getAudioPlayerManager().registerSourceManager(new VimeoAudioSourceManager());
        Main.getAudioPlayerManager().registerSourceManager(new TwitchStreamAudioSourceManager());
        Main.getAudioPlayerManager().registerSourceManager(new BeamAudioSourceManager());
        Main.getAudioPlayerManager().registerSourceManager(new GetyarnAudioSourceManager());
        Main.getAudioPlayerManager().registerSourceManager(new HttpAudioSourceManager(MediaContainerRegistry.DEFAULT_REGISTRY));
    }

    public static AudioPlayerManager getAudioPlayerManager() {
        return Main.getAudioPlayerManager();
    }

    public Guild getGuild() {
        return guild;
    }

    /**
     * @return Wrapper around AudioPlayer to use it as an AudioSendHandler.
     */
    public AudioPlayerSendHandler getSendHandler() {
        return new AudioPlayerSendHandler(player);
    }

    public boolean isPlaying() {
        return getAudioPlayer().getPlayingTrack() != null;
    }

    public AudioPlayer getAudioPlayer() {
        return player;
    }

    public TrackScheduler getTrackScheduler() {
        return scheduler;
    }

    public AudioManager getAudioManager() {
        return getGuild().getAudioManager();
    }

    public boolean connectVoice() {
        return connectVoice(null);
    }

    public boolean connectVoice(VoiceChannel voiceChannel) {
        if (getAudioManager().isConnected()) return false;

        if (voiceChannel != null) {
            getAudioManager().openAudioConnection(voiceChannel);
            return true;
        }

        if (ConfigManager
                .getGuildConfig(
                        getGuild()
                )
                .getMusicVoiceChannel() != null) {
            getAudioManager().openAudioConnection(ConfigManager.getGuildConfig(getGuild()).getMusicVoiceChannel());
            return true;
        }

        for (VoiceChannel vc : getGuild().getVoiceChannels()) {
            getAudioManager().openAudioConnection(vc);
            return true;
        }

        return false;
    }

    public void disconnect() {
        getAudioManager().closeAudioConnection();
    }
}
