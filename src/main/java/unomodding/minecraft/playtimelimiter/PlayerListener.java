/**
 * Copyright 2014 by RyanTheAlmighty, UnoModding and Contributors
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 3.0 Unported License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/3.0/.
 */
package unomodding.minecraft.playtimelimiter;

import java.io.File;

import net.canarymod.chat.TextFormat;
import net.canarymod.hook.HookHandler;
import net.canarymod.hook.player.ConnectionHook;
import net.canarymod.hook.player.DisconnectionHook;
import net.canarymod.plugin.PluginListener;
import unomodding.minecraft.playtimelimiter.utils.FileUtils;
import unomodding.minecraft.playtimelimiter.utils.Timestamper;

public class PlayerListener implements PluginListener {
    private final PlayTimeLimiter plugin;

    public PlayerListener(PlayTimeLimiter instance) {
        this.plugin = instance;
    }

    @HookHandler
    public void onPlayerJoin(ConnectionHook hook) {
        FileUtils.appendStringToFile(new File(this.plugin.getDataFolder(), "playtime.log"),
                String.format("[%s] %s logged in", Timestamper.now(), hook.getPlayer().getName()));
        if (this.plugin.getTimeAllowedInSeconds(hook.getPlayer().getName()) <= 0) {
            FileUtils.appendStringToFile(new File(this.plugin.getDataFolder(), "playtime.log"),
                    String.format("[%s] %s was kicked for exceeding play time", Timestamper.now(),
                            hook.getPlayer().getName()));
            hook.getPlayer().kick(
                    "You have exceeded the time allowed to play! Come back in "
                            + this.plugin.secondsToDaysHoursSecondsString(this.plugin
                                    .secondsUntilNextDay()) + "!");
        } else {
            this.plugin.setPlayerLoggedIn(hook.getPlayer().getName());
        }
        hook.getPlayer().message(
                "You have "
                        + TextFormat.GREEN
                        + plugin.secondsToDaysHoursSecondsString(plugin
                                .getTimeAllowedInSeconds(hook.getPlayer().getName()))
                        + TextFormat.RESET + " of playtime left!");
    }

    @HookHandler
    public void onPlayerQuit(DisconnectionHook hook) {
        FileUtils
                .appendStringToFile(new File(this.plugin.getDataFolder(), "playtime.log"), String
                        .format("[%s] %s logged out", Timestamper.now(), hook.getPlayer()
                                .getName()));
        this.plugin.setPlayerLoggedOut(hook.getPlayer().getName());
    }
}
