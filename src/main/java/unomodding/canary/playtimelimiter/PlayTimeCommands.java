/**
 * Copyright 2014 by RyanTheAlmighty, UnoModding and Contributors
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 3.0 Unported License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/3.0/.
 */
package unomodding.canary.playtimelimiter;

import java.util.List;

import net.canarymod.Canary;
import net.canarymod.chat.MessageReceiver;
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

    @Command(aliases = {"playtime", "pt"},
			description = "playtime command",
			permissions = {},
			toolTip = "/playtime <start|stop|add|remove|check> [parameters...]",
			version = 2)
    public void playtimeCommand(MessageReceiver caller, String[] args) {
        if(args.length != 0) {
        	if(args[0].equals("start") && args.length == 1) {
                if(!caller.hasPermission("playtimelimiter.playtime.start")) {
                    caller.message(TextFormat.RED
                            + "You don't have permission to start the playtime counter!");
                } else {
                    if(!plugin.start()) {
                    	caller.message(TextFormat.RED + "Playtime already started!");
                    } else {
                    	caller.message(TextFormat.RED + "Playtime started!");
                    }
                }
                return;
            } else if(args[0].equals("stop")) {
            	if(!caller.hasPermission("playtimelimiter.playtime.stop")) {
                    caller.message(TextFormat.RED
                            + "You don't have permission to stop the playtime counter!");
                } else {
                    if(!plugin.stop()) {
                    	caller.message(TextFormat.RED + "Playtime already stopped!");
                    } else {
                    	caller.message(TextFormat.RED + "Playtime stopped!");
                    }
                }
                return;
            } else if(args[0].equals("remove") && args.length == 3) {
                if(!plugin.hasStarted()) {
                    caller.message(TextFormat.RED + "Playtime hasn't started yet!");
                    return;
                }
                if (!caller.hasPermission("playtimelimiter.playtime.remove")) {
                    caller.message(TextFormat.RED
                            + "You don't have permission to add remove from a players playtime!");
                } else {
                    try {
                        plugin.addPlayTime(Canary.getServer().getPlayer(args[1]).getUUIDString(), Integer.parseInt(args[2]));
                        caller.message(TextFormat.GREEN + "Removed " + Integer.parseInt(args[2])
                                + " seconds of playtime to " + args[1]);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        caller.message(TextFormat.RED + "Invalid number of seconds given!");
                    }
                }
                return;
            } else if(args[0].equals("add") && args.length == 3) {
                if(!plugin.hasStarted()) {
                    caller.message(TextFormat.RED + "Playtime hasn't started yet!");
                    return;
                }
                if(!caller.hasPermission("playtimelimiter.playtime.add")) {
                    caller.message(TextFormat.RED
                            + "You don't have permission to add time to a players playtime!");
                } else {
                    try {
                        plugin.removePlayTime(Canary.getServer().getPlayer(args[1]).getUUIDString(), Integer.parseInt(args[2]));
                        caller.message(TextFormat.GREEN + "Added " + Integer.parseInt(args[2])
                                + " seconds of playtime from " + args[1]);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        caller.message(TextFormat.RED + "Invalid number of seconds given!");
                    } catch (UnknownPlayerException e) {
                        e.printStackTrace();
                        caller.message(TextFormat.RED + e.getMessage());
                    }
                }
                return;
            } else if(args[0].equals("check")) {
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
                                        .getPlayerPlayTime(Canary.getServer().getPlayer(caller.getName()).getUUIDString()))
                                + " and have "
                                + plugin.secondsToDaysHoursSecondsString(plugin
                                        .getTimeAllowedInSeconds(Canary.getServer().getPlayer(caller.getName()).getUUIDString() + " remaining!")));
                    }
                    return;
                } else if (args.length == 2) {
                    if (!caller.hasPermission("playtimelimiter.playtime.check.others")) {
                        caller.message(TextFormat.RED
                                + "You don't have permission to check other players playtime!");
                    } else {
                        caller.message(TextFormat.GREEN
                                + Canary.getServer().getPlayer(args[1]).getName()
                                + " has played for "
                                + plugin.secondsToDaysHoursSecondsString(plugin
                                        .getPlayerPlayTime(Canary.getServer().getPlayer(args[1]).getUUIDString()))
                                + " and has "
                                + plugin.secondsToDaysHoursSecondsString(plugin
                                        .getTimeAllowedInSeconds(Canary.getServer().getPlayer(args[1]).getUUIDString())) + " remaining!");
                    }
                    return;
                }
            }
        }
        printUsage(caller);
    }
    
    public void printUsage(MessageReceiver sender) {
    	sender.message(TextFormat.YELLOW + "/playtime usage:");
    	if(sender.hasPermission("playtimelimiter.playtime.start")){
    		sender.message(TextFormat.CYAN + "/playtime start" + TextFormat.RESET
                    + " - Start the playtime counter.");
        }
    	if(sender.hasPermission("playtimelimiter.playtime.stop")){
    		sender.message(TextFormat.CYAN + "/playtime stop" + TextFormat.RESET
                    + " - Stop the playtime counter.");
        } 
    	if(sender.hasPermission("playtimelimiter.playtime.add")) {
        	sender.message(TextFormat.CYAN + "/playtime add <user> <time>" + TextFormat.RESET
                    + " - Add time in seconds to the user's playtime.");
        }
        if(sender.hasPermission("playtimelimiter.playtime.check.others")) {
        	sender.message(TextFormat.CYAN
                    + "/playtime check [user]"
                    + TextFormat.RESET
                    + " - Check the time played and time left for a given user, or if blank, for yourself.");
        } else if(sender.hasPermission("playtimelimiter.playtime.check.self")) {
        	sender.message(TextFormat.CYAN + "/playtime check" + TextFormat.RESET
                    + " - Check the time played and time left for yourself.");
        }
        if(sender.hasPermission("playtimelimiter.playtime.remove")) {
        	sender.message(TextFormat.CYAN + "/playtime remove <user> <time>" + TextFormat.RESET
                    + " - Remove time in seconds from the user's playtime.");
        }
    }
    
    @TabComplete(commands = {"playtime", "pt"})
    public List<String> playtimeTabComplete(MessageReceiver caller, String[] parameters) {
    	if(parameters.length == 1) {
    		return TabCompleteHelper.matchTo(parameters, new String[]{"start", "stop", "add", "remove", "check"});
    	} else if(parameters.length == 2 && (parameters[1].equals("add") || parameters[1].equals("remove") || parameters[1].equals("check"))) {
    		return TabCompleteHelper.matchTo(parameters, Canary.getServer().getKnownPlayerNames());
    	} else {
    		return null;
    	}
    }
}