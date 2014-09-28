/**
 * Copyright 2014 by RyanTheAlmighty, UnoModding and Contributors
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 3.0 Unported License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/3.0/.
 */
package unomodding.minecraft.playtimelimiter;

import net.canarymod.chat.MessageReceiver;
import net.canarymod.chat.TextFormat;
import net.canarymod.commandsys.Command;
import net.canarymod.commandsys.CommandListener;
import unomodding.minecraft.playtimelimiter.exceptions.UnknownPlayerException;

public class PlayTimeCommands implements CommandListener {
    private final PlayTimeLimiter plugin;

    public PlayTimeCommands(PlayTimeLimiter plugin) {
        this.plugin = plugin;
    }

    @Command(aliases = {"playtime"},
			description = "playtime command",
			permissions = {},
			toolTip = "/playtime <argument> (argument) (argument)",
			min = 2,
			max = 4)
    public void onStartCommand(MessageReceiver caller, String[] args) {
        if (args[0].equals("start") && args.length == 1) {
            if (!caller.hasPermission("playtimelimiter.start")) {
                caller.message(TextFormat.RED
                        + "You don't have permission to start the playtime counter!");
            } else {
                if (plugin.start()) {
                } else {
                    caller.message(TextFormat.RED + "Playtime already started!");
                }
            }
        } else if (args[0].equals("add") && args.length == 3) {
            if (!plugin.hasStarted()) {
                caller.message(TextFormat.RED + "Playtime hasn't started yet!");
            }
            if (!caller.hasPermission("playtimelimiter.playtime.add")) {
                caller.message(TextFormat.RED
                        + "You don't have permission to add time to a players playtime!");
            } else {
                try {
                    plugin.addPlayTime(args[1], Integer.parseInt(args[2]));
                    caller.message(TextFormat.GREEN + "Added " + Integer.parseInt(args[2])
                            + " seconds of playtime to " + args[1]);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    caller.message(TextFormat.RED + "Invalid number of seconds given!");
                }
            }
        } else if (args[0].equals("remove") && args.length == 3) {
            if (!plugin.hasStarted()) {
                caller.message(TextFormat.RED + "Playtime hasn't started yet!");
            }
            if (!caller.hasPermission("playtimelimiter.playtime.remove")) {
                caller.message(TextFormat.RED
                        + "You don't have permission to remove time from a players playtime!!");
            } else {
                try {
                    plugin.removePlayTime(args[1], Integer.parseInt(args[2]));
                    caller.message(TextFormat.GREEN + "Removed " + Integer.parseInt(args[2])
                            + " seconds of playtime from " + args[1]);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    caller.message(TextFormat.RED + "Invalid number of seconds given!");
                } catch (UnknownPlayerException e) {
                    e.printStackTrace();
                    caller.message(TextFormat.RED + e.getMessage());
                }
            }
        } else if (args[0].equals("check")) {
            if (!plugin.hasStarted()) {
                caller.message(TextFormat.RED + "Playtime hasn't started yet!");
            }
            if (args.length == 1) {
                if (!caller.hasPermission("playtimelimiter.playtime.check.self")) {
                    caller.message(TextFormat.RED
                            + "You don't have permission to check your playtime!");
                } else {
                    caller.message(TextFormat.GREEN
                            + "You have played for "
                            + plugin.secondsToDaysHoursSecondsString(plugin
                                    .getPlayerPlayTime(caller.getName()))
                            + " and have "
                            + plugin.secondsToDaysHoursSecondsString(plugin
                                    .getTimeAllowedInSeconds(caller.getName())) + " remaining!");
                }
            } else if (args.length == 2) {
                if (!caller.hasPermission("playtimelimiter.playtime.check.others")) {
                    caller.message(TextFormat.RED
                            + "You don't have permission to check other players playtime!");
                } else {
                    caller.message(TextFormat.GREEN
                            + args[1]
                            + " has played for "
                            + plugin.secondsToDaysHoursSecondsString(plugin
                                    .getPlayerPlayTime(args[1]))
                            + " and has "
                            + plugin.secondsToDaysHoursSecondsString(plugin
                                    .getTimeAllowedInSeconds(args[1])) + " remaining!");
                }
            }
        }
    }
}