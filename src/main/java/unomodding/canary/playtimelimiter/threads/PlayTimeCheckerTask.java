/**
 * Copyright 2014 by UnoModding, RyanTheAlmighty and Contributors
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 3.0 Unported License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/3.0/.
 */
package unomodding.canary.playtimelimiter.threads;

import java.io.File;
import java.util.TimerTask;

import net.canarymod.Canary;
import net.canarymod.api.entity.living.humanoid.Player;
import net.canarymod.chat.Colors;
import net.canarymod.chat.TextFormat;
import unomodding.canary.playtimelimiter.PlayTimeLimiter;
import unomodding.canary.playtimelimiter.utils.FileUtils;
import unomodding.canary.playtimelimiter.utils.Timestamper;

public class PlayTimeCheckerTask implements Runnable {
    private final PlayTimeLimiter plugin;

    public PlayTimeCheckerTask(PlayTimeLimiter instance) {
        this.plugin = instance;
    }

    public void run() {
        for (Player player : Canary.getServer().getPlayerList()) {
            if (plugin.hasPlayTime(player)) {
                int timeLeft = this.plugin.getTimeAllowedInSeconds(player);
                if (timeLeft <= 0) {
                    FileUtils.appendStringToFile(
                            new File(this.plugin.getDataFolder(), "playtime.log"),
                            String.format("[%s] %s was kicked for exceeding play time", Timestamper.now(),
                                    player.getName()));
                    player.kick("You have exceeded the time allowed to play! Come back in "
                            + this.plugin.secondsToDaysHoursSecondsString(this.plugin.secondsUntilNextDay()) + "!");
                } else if (timeLeft <= 10 && !this.plugin.hasPlayerSeenMessage(player, 10)) {
                    player.message(Colors.RED
                            + "WARNING!"
                            + TextFormat.RESET
                            + " You have less than 10 seconds of playtime left! Stop what your doing and prepare to be disconnected!");
                    this.plugin.sentPlayerWarningMessage(player, 10);
                } else if (timeLeft <= 60 && timeLeft > 10 && !this.plugin.hasPlayerSeenMessage(player, 60)) {
                    player.message(Colors.RED
                            + "WARNING!"
                            + TextFormat.RESET
                            + " You have less than 60 seconds of playtime left! Stop what your doing and prepare to be disconnected!");
                    this.plugin.sentPlayerWarningMessage(player, 60);
                } else if (timeLeft <= 300 && timeLeft > 60 && !this.plugin.hasPlayerSeenMessage(player, 300)) {
                    player.message(Colors.RED
                            + "WARNING!"
                            + TextFormat.RESET
                            + " You have less than 5 minutes of playtime left! Stop what your doing and prepare to be disconnected!");
                    this.plugin.sentPlayerWarningMessage(player, 300);
                }
            }
        }
    }
}
