/**
 * Copyright 2014 by RyanTheAlmighty, UnoModding and Contributors
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 3.0 Unported License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/3.0/.
 */
package unomodding.minecraft.playtimelimiter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import net.canarymod.Canary;
import net.canarymod.chat.TextFormat;
import net.canarymod.commandsys.CommandDependencyException;
import net.canarymod.plugin.Plugin;

import unomodding.minecraft.playtimelimiter.exceptions.UnknownPlayerException;
import unomodding.minecraft.playtimelimiter.threads.PlayTimeCheckerTask;
import unomodding.minecraft.playtimelimiter.threads.PlayTimeSaverTask;
import unomodding.minecraft.playtimelimiter.threads.ShutdownThread;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public final class PlayTimeLimiter extends Plugin {
    private Map<String, Integer> timePlayed = new HashMap<String, Integer>();
    private Map<String, Integer> timeLoggedIn = new HashMap<String, Integer>();
    private Map<String, Boolean> seenWarningMessages = new HashMap<String, Boolean>();

    private boolean shutdownHookAdded = false;
    private Timer savePlayTimeTimer = null;
    private Timer checkPlayTimeTimer = null;
    private boolean started = false;
    private final Gson GSON = new Gson();
	private static PlayTimeLimiter instance;

    public void disable() {
        this.savePlayTime(); // Save the playtime to file on plugin disable
    }

    public boolean enable() {
    	instance = this;
        if (!this.shutdownHookAdded) {
            this.shutdownHookAdded = true;
            try {
                Runtime.getRuntime().addShutdownHook(new ShutdownThread(this));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(!getConfig().containsKey("timeStarted")) {
        	getConfig().setInt("timeStarted", (int) (System.currentTimeMillis() / 1000));
        }
        this.started = true;

        if (!getConfig().containsKey("initialTime")) {
            getConfig().setInt("initialTime", 28800);
            getConfig().save();
        }

        if (!getConfig().containsKey("timePerDay")) {
            getConfig().setInt("timePerDay", 3600);
            getConfig().save();
        }

        if (!getConfig().containsKey("secondsBetweenPlayTimeChecks")) {
            getConfig().setInt("secondsBetweenPlayTimeChecks", 10);
            getConfig().save();
        }

        if (!getConfig().containsKey("secondsBetweenPlayTimeSaving")) {
            getConfig().setInt("secondsBetweenPlayTimeSaving", 600);
            getConfig().save();
        }

        getLogman()
                .info(String.format("Server started at %s which was %s seconds ago!", getConfig()
                        .getInt("timeStarted"), this.secondsToDaysHoursSecondsString((int) ((System
                        .currentTimeMillis() / 1000) - getConfig().getInt("timeStarted")))));

        // Load the playtime from file
        this.loadPlayTime();
        
        // Enable Listener
     	Canary.hooks().registerListener(new PlayerListener(this), this);
     		
     	// Enable Commands
     	try {
            Canary.commands().registerCommands(new PlayTimeCommands(this), this, false);
        } catch (CommandDependencyException e) {
            e.printStackTrace();
        }

        if (savePlayTimeTimer == null) {
            this.savePlayTimeTimer = new Timer();
            this.savePlayTimeTimer.scheduleAtFixedRate(new PlayTimeSaverTask(this), 30000,
                    getConfig().getInt("secondsBetweenPlayTimeSaving") * 1000);
        }

        if (checkPlayTimeTimer == null) {
            this.checkPlayTimeTimer = new Timer();
            this.checkPlayTimeTimer.scheduleAtFixedRate(new PlayTimeCheckerTask(this), 30000,
                    getConfig().getInt("secondsBetweenPlayTimeChecks") * 1000);
        }
        return true;
    }

    public int secondsUntilNextDay() {
        int timeStarted = getConfig().getInt("timeStarted");
        int secondsSince = (int) ((System.currentTimeMillis() / 1000) - timeStarted);

        while (secondsSince >= 86400) {
            secondsSince -= 86400;
        }

        return secondsSince;
    }

    public String secondsToDaysHoursSecondsString(int secondsToConvert) {
        int hours = secondsToConvert / 3600;
        int minutes = (secondsToConvert % 3600) / 60;
        int seconds = secondsToConvert % 60;
        return String.format("%02d hours, %02d minutes & %02d seconds", hours, minutes, seconds);
    }

    public int getTimeAllowedInSeconds() {
        int timeStarted = getConfig().getInt("timeStarted");
        int secondsSince = (int) ((System.currentTimeMillis() / 1000) - timeStarted);
        int secondsAllowed = 0;

        // Add the initial time we give the player at the beginning
        secondsAllowed += getConfig().getInt("initialTime");

        // Then for each day including the first day (24 hours realtime) add the set amount of
        // seconds to the time allowed
        while (secondsSince >= 0) {
            secondsAllowed += getConfig().getInt("timePerDay");
            secondsSince -= 86400;
        }

        return secondsAllowed;
    }

    public int getTimeAllowedInSeconds(String player) {
        int secondsAllowed = this.getTimeAllowedInSeconds();

        // Remove the amount of time the player has played to get their time allowed
        secondsAllowed -= getPlayerPlayTime(player);

        return secondsAllowed;
    }

    public void addPlayTime(String player, int seconds) {
        if (this.timePlayed.containsKey(player)) {
            this.timePlayed.put(player, this.timePlayed.get(player) + seconds);
        } else {
            this.timePlayed.put(player, seconds);
        }
    }

    public void removePlayTime(String player, int seconds) throws UnknownPlayerException {
        if (this.timePlayed.containsKey(player)) {
            this.timePlayed.put(player, this.timePlayed.get(player) - seconds);
        } else {
            throw new UnknownPlayerException(player);
        }
    }

    public int getPlayerPlayTime(String player) {
        int timePlayed = 0;
        if (this.timePlayed.containsKey(player)) {
            timePlayed += this.timePlayed.get(player);
        }
        if (this.timeLoggedIn.containsKey(player)) {
            timePlayed += (int) ((System.currentTimeMillis() / 1000) - this.timeLoggedIn
                    .get(player));
        }
        return timePlayed;
    }

    public void setPlayerLoggedIn(String player) {
        if (!this.timePlayed.containsKey(player)) {
            this.timePlayed.put(player, 0);
            this.savePlayTime();
        }
        this.timeLoggedIn.put(player, (int) (System.currentTimeMillis() / 1000));
    }

    public void setPlayerLoggedOut(String player) {
        if (this.timeLoggedIn.containsKey(player)) {
            int timePlayed = (int) ((System.currentTimeMillis() / 1000) - this.timeLoggedIn
                    .get(player));
            if (this.timePlayed.containsKey(player)) {
                timePlayed += this.timePlayed.get(player);
            }
            if (timePlayed > this.getTimeAllowedInSeconds()) {
                timePlayed = this.getTimeAllowedInSeconds();
            }
            this.timePlayed.put(player, timePlayed);
            this.timeLoggedIn.remove(player);
            getLogman().info(
                    "Player " + player + " played for a total of " + timePlayed + " seconds!");
            this.savePlayTime();
        }
        if (this.seenWarningMessages.containsKey(player + ":10")) {
            this.seenWarningMessages.remove(player + ":10");
        }
        if (this.seenWarningMessages.containsKey(player + ":60")) {
            this.seenWarningMessages.remove(player + ":60");
        }
        if (this.seenWarningMessages.containsKey(player + ":300")) {
            this.seenWarningMessages.remove(player + ":300");
        }
    }

    public boolean hasPlayerSeenMessage(String player, int time) {
        if (this.seenWarningMessages.containsKey(player + ":" + time)) {
            return this.seenWarningMessages.get(player + ":" + time);
        } else {
            return false;
        }
    }

    public void sentPlayerWarningMessage(String player, int time) {
        this.seenWarningMessages.put(player + ":" + time, true);
    }

    public boolean start() {
        if (this.started) {
            return false;
        } else {
            this.started = true;
            String initial = (getConfig().getInt("initialTime") / 60 / 60) + "";
            String perday = (getConfig().getInt("timePerDay") / 60 / 60) + "";
            Canary.getServer().broadcastMessage(
                    TextFormat.GREEN + "Playtime has now started! You have " + initial
                            + " hour/s of playtime to start with and " + perday
                            + " hour/s of playtime added per day!");
            getConfig().setInt("timeStarted", (int) (System.currentTimeMillis() / 1000));
            getConfig().save();
            return true;
        }
    }
    
    public boolean stop() {
    	if (!this.started) {
            return false;
        } else {
            this.started = false;
            return true;
        }
    }

    public boolean hasStarted() {
        return this.started;
    }

    public void loadPlayTime() {
        if (!hasStarted()) {
            return;
        }
        File file = new File(getDataFolder(), "playtime.json");
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        if (!file.exists()) {
            getLogman().warn("playtime.json file missing! Not loading in values");
            return;
        }
        getLogman().info("Loading data from playtime.json");
        FileReader fileReader;
        try {
            fileReader = new FileReader(file);
            java.lang.reflect.Type type = new TypeToken<Map<String, Integer>>() {
            }.getType();
            this.timePlayed = GSON.fromJson(fileReader, type);
            fileReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void savePlayTime() {
        this.savePlayTime(false);
    }

    public void savePlayTime(boolean force) {
        if (!hasStarted()) {
            return;
        }

        if (force) {
            for (String key : this.timeLoggedIn.keySet()) {
                this.setPlayerLoggedOut(key);
            }
        }
        File file = new File(getDataFolder(), "playtime.json");
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        getLogman().info("Saving data to playtime.json");
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            fw = new FileWriter(file);
            bw = new BufferedWriter(fw);
            bw.write(GSON.toJson(this.timePlayed));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (bw != null) {
                bw.close();
            }
            if (fw != null) {
                fw.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	public File getDataFolder() {
		return new File(Canary.getWorkingPath() + "/config/PlayTimeLimiter");
	}
	
	public static PlayTimeLimiter getInstance() {
		return instance;
	}
}