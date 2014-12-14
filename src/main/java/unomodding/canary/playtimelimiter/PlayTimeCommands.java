/**
 * Copyright 2014 by UnoModding, RyanTheAlmighty and Contributors
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 3.0 Unported License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/3.0/.
 */
package unomodding.canary.playtimelimiter;

import java.util.List;

import net.canarymod.Canary;
import net.canarymod.api.OfflinePlayer;
import net.canarymod.chat.Colors;
import net.canarymod.chat.MessageReceiver;
import net.canarymod.chat.ReceiverType;
import net.canarymod.chat.TextFormat;
import net.canarymod.commandsys.Command;
import net.canarymod.commandsys.CommandListener;
import net.canarymod.commandsys.TabComplete;
import net.canarymod.commandsys.TabCompleteHelper;
import unomodding.canary.playtimelimiter.exceptions.UnknownPlayerException;

public class PlayTimeCommands implements CommandListener {
    private final PlayTimeLimiter plugin;

    public PlayTimeCommands(PlayTimeLimiter plugin) {
        this.plugin = plugin;
    }

    @Command(aliases = { "playtime", "pt" },
             description = "playtime command",
             permissions = {},
             toolTip = "/playtime <start|stop|add|remove|check> [parameters...]",
             version = 2)
    public void baseCommand(MessageReceiver caller, String[] args) {
        printUsage(caller);
    }

    @Command(aliases = { "start" },
             parent = "playtime",
             description = "start subcommand",
             permissions = { "playtimelimiter.playtime.start" },
             toolTip = "/playtime start",
             version = 2)
    public void startCommand(MessageReceiver caller, String[] args) {
        if (!plugin.start()) {
            caller.message(Colors.RED + "Playtime already started!");
        } else {
            caller.message(Colors.RED + "Playtime started!");
        }
    }

    @Command(aliases = { "stop" },
             parent = "playtime",
             description = "stop subcommand",
             permissions = { "playtimelimiter.playtime.stop" },
             toolTip = "/playtime stop",
             version = 2)
    public void stopCommand(MessageReceiver caller, String[] args) {
        if (!plugin.stop()) {
            caller.message(Colors.RED + "Playtime already stopped!");
        } else {
            caller.message(Colors.RED + "Playtime stopped!");
        }
    }

    @Command(aliases = { "check" },
             parent = "playtime",
             description = "stop subcommand",
             permissions = {},
             toolTip = "/playtime check [player]",
             version = 2)
    public void checkCommand(MessageReceiver caller, String[] args) {
        if (!plugin.hasStarted()) {
            caller.message(Colors.RED + "Playtime hasn't started yet!");
        } else {
            if (args.length == 0) {
                if (!caller.hasPermission("playtimelimiter.playtime.check.self")) {
                    caller.message(Colors.RED + "You don't have permission to check your playtime!");
                } else {
                    if(caller.getReceiverType() == ReceiverType.PLAYER) {
                        OfflinePlayer player = Canary.getServer().getOfflinePlayer(caller.getName());
                        plugin.loadPlayTime(player);
                        caller.message(Colors.GREEN + "You have played for "
                                + plugin.secondsToDaysHoursSecondsString(plugin.getPlayerPlayTime(player)) + " and have "
                                + plugin.secondsToDaysHoursSecondsString(plugin.getTimeAllowedInSeconds(player))
                                + " remaining!");
                    } else {
                        caller.message(Colors.RED + "Only Players have playtime!");
                    }
                }
            } else if (args.length == 1) {
                if (!caller.hasPermission("playtimelimiter.playtime.check.others")) {
                    caller.message(Colors.RED + "You don't have permission to check other players playtime!");
                } else {
                    OfflinePlayer player = Canary.getServer().getOfflinePlayer(args[0]);
                    caller.message(Colors.GREEN + player.getName() + " has played for "
                            + plugin.secondsToDaysHoursSecondsString(plugin.getPlayerPlayTime(player)) + " and has "
                            + plugin.secondsToDaysHoursSecondsString(plugin.getTimeAllowedInSeconds(player))
                            + " remaining!");
                }
            }
        }
    }

    @Command(aliases = { "add" },
             parent = "playtime",
             description = "add subcommand",
             permissions = { "playtimelimiter.playtime.add" },
             toolTip = "/playtime add <player> <seconds>",
             version = 2)
    public void addCommand(MessageReceiver caller, String[] args) {
        if (!plugin.hasStarted()) {
            caller.message(Colors.RED + "Playtime hasn't started yet!");
        } else {
            try {
                OfflinePlayer player = Canary.getServer().getOfflinePlayer(args[0]);
                plugin.loadPlayTime(player);
                plugin.addPlayTime(player, Integer.parseInt(args[1]));
                caller.message(Colors.GREEN + "Added " + Integer.parseInt(args[1]) + " seconds of playtime from "
                        + args[0]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                caller.message(Colors.RED + "Invalid number of seconds given!");
            } catch (UnknownPlayerException e) {
                e.printStackTrace();
                caller.message(Colors.RED + e.getMessage());
            }
        }
    }

    @Command(aliases = { "remove" },
             parent = "playtime",
             description = "remove subcommand",
             permissions = { "playtimelimiter.playtime.remove" },
             toolTip = "/playtime remove <player> <seconds>",
             version = 2)
    public void removeCommand(MessageReceiver caller, String[] args) {
        if (!plugin.hasStarted()) {
            caller.message(Colors.RED + "Playtime hasn't started yet!");
        } else {
            try {
                OfflinePlayer player = Canary.getServer().getOfflinePlayer(args[0]);
                plugin.loadPlayTime(player);
                plugin.removePlayTime(player, Integer.parseInt(args[1]));
                caller.message(Colors.GREEN + "Removed " + Integer.parseInt(args[1]) + " seconds of playtime from "
                        + args[0]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                caller.message(Colors.RED + "Invalid number of seconds given!");
            }
        }
    }

    @TabComplete(commands = { "playtime", "pt" })
    public List<String> playtimeTabComplete(MessageReceiver caller, String[] parameters) {
        if (parameters.length == 1) {
            return TabCompleteHelper.matchTo(parameters, new String[] { "start", "stop", "add", "remove", "check" });
        } else if (parameters.length == 2
                && (parameters[1].equals("add") || parameters[1].equals("remove") || parameters[1].equals("check"))) {
            return TabCompleteHelper.matchTo(parameters, Canary.getServer().getKnownPlayerNames());
        } else {
            return null;
        }
    }

    public void printUsage(MessageReceiver caller) {
        caller.message(Colors.YELLOW + "/playtime usage:");
        if (caller.hasPermission("playtimelimiter.playtime.start")) {
            caller.message(Colors.CYAN + "/playtime start" + TextFormat.RESET + " - Start the playtime counter.");
        }
        if (caller.hasPermission("playtimelimiter.playtime.stop")) {
            caller.message(Colors.CYAN + "/playtime stop" + TextFormat.RESET + " - Stop the playtime counter.");
        }
        if (caller.hasPermission("playtimelimiter.playtime.add")) {
            caller.message(Colors.CYAN + "/playtime add <user> <time>" + TextFormat.RESET
                    + " - Add time in seconds to the user's playtime.");
        }
        if (caller.hasPermission("playtimelimiter.playtime.check.others")) {
            caller.message(Colors.CYAN + "/playtime check [user]" + TextFormat.RESET
                    + " - Check the time played and time left for a given user, or if blank, for yourself.");
        } else if (caller.hasPermission("playtimelimiter.playtime.check.self")) {
            caller.message(Colors.CYAN + "/playtime check" + TextFormat.RESET
                    + " - Check the time played and time left for yourself.");
        }
        if (caller.hasPermission("playtimelimiter.playtime.remove")) {
            caller.message(Colors.CYAN + "/playtime remove <user> <time>" + TextFormat.RESET
                    + " - Remove time in seconds from the user's playtime.");
        }
    }
}