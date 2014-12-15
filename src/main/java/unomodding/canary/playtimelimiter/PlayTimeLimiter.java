/**
 * Copyright 2014 by UnoModding, RyanTheAlmighty and Contributors
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 3.0 Unported License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/3.0/.
 */
package unomodding.canary.playtimelimiter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import net.canarymod.Canary;
import net.canarymod.api.OfflinePlayer;
import net.canarymod.api.entity.living.humanoid.Player;
import net.canarymod.chat.Colors;
import net.canarymod.commandsys.CommandDependencyException;
import net.canarymod.database.Database;
import net.canarymod.database.exceptions.DatabaseReadException;
import net.canarymod.database.exceptions.DatabaseWriteException;
import net.canarymod.plugin.Plugin;

import org.mcstats.Metrics;

import unomodding.canary.playtimelimiter.data.PlayTimeDataAccess;
import unomodding.canary.playtimelimiter.exceptions.UnknownPlayerException;
import unomodding.canary.playtimelimiter.threads.PlayTimeCheckerTask;
import unomodding.canary.playtimelimiter.threads.PlayTimeSaverTask;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public final class PlayTimeLimiter extends Plugin {
    private Map<String, Integer> timePlayed = new HashMap<String, Integer>();
    private Map<String, Integer> timeLoggedIn = new HashMap<String, Integer>();
    private Map<String, Boolean> seenWarningMessages = new HashMap<String, Boolean>();

    private Timer savePlayTimeTimer = null;
    private Timer checkPlayTimeTimer = null;
    private boolean started = false;
    private final Gson GSON = new Gson();

    @Override
    public void disable() {
        this.savePlayTime(); // Save the playtime to file on plugin disable
    }

    @Override
    public boolean enable() {
        if (!getConfig().containsKey("timeStarted")) {
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

        if (!getConfig().containsKey("timeTravels")) {
            getConfig().setBoolean("timeTravels", true);
            getConfig().save();
        }

        getLogman().info(
                String.format("Server started at %s which was %s seconds ago!", getConfig().getInt("timeStarted"), this
                        .secondsToDaysHoursSecondsString((int) ((System.currentTimeMillis() / 1000) - getConfig()
                                .getInt("timeStarted")))));

        // Enable Listener
        Canary.hooks().registerListener(new PlayTimeListener(this), this);

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

        // Metrics
        try {
            Metrics metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {
            getLogman().info("Failed to send data to Metrics", e);
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

        // Then for each day including the first day (24 hours realtime) add the
        // set amount of
        // seconds to the time allowed
        while (secondsSince >= 0) {
            if (getConfig().getBoolean("timeTravels")) {
                secondsAllowed += getConfig().getInt("timePerDay");
            } else {
                secondsAllowed = getConfig().getInt("timePerDay");
            }
            secondsSince -= 86400;
        }

        return secondsAllowed;
    }

    public int getTimeAllowedInSeconds(OfflinePlayer player) {
        return getTimeAllowedInSeconds(player.getUUIDString());
    }

    public int getTimeAllowedInSeconds(Player player) {
        return getTimeAllowedInSeconds(player.getUUIDString());
    }

    private int getTimeAllowedInSeconds(String uuid) {
        int secondsAllowed = this.getTimeAllowedInSeconds();

        // Remove the amount of time the player has played to get their time
        // allowed
        secondsAllowed -= getPlayerPlayTime(uuid);

        return secondsAllowed;
    }

    public void addPlayTime(OfflinePlayer player, int seconds) throws UnknownPlayerException {
        if (this.timePlayed.containsKey(player.getUUIDString())) {
            this.timePlayed.put(player.getUUIDString(), this.timePlayed.get(player.getUUIDString()) - seconds);
        } else {
            throw new UnknownPlayerException(player.getUUID());
        }
    }

    public void removePlayTime(OfflinePlayer player, int seconds) {
        if (this.timePlayed.containsKey(player.getUUIDString())) {
            this.timePlayed.put(player.getUUIDString(), this.timePlayed.get(player.getUUIDString()) + seconds);
        } else {
            this.timePlayed.put(player.getUUIDString(), seconds);
        }
    }

    public int getPlayerPlayTime(OfflinePlayer player) {
        return getPlayerPlayTime(player.getUUIDString());
    }

    private int getPlayerPlayTime(String uuid) {
        int timePlayed = 0;
        if (this.timePlayed.containsKey(uuid)) {
            timePlayed += this.timePlayed.get(uuid);
        }
        if (this.timeLoggedIn.containsKey(uuid)) {
            timePlayed += (int) ((System.currentTimeMillis() / 1000) - this.timeLoggedIn.get(uuid));
        }
        return timePlayed;
    }

    public void setPlayerLoggedIn(OfflinePlayer player) {
        setPlayerLoggedIn(player.getUUIDString());
    }

    public void setPlayerLoggedIn(Player player) {
        setPlayerLoggedIn(player.getUUIDString());
    }

    private void setPlayerLoggedIn(String uuid) {
        if (!this.timePlayed.containsKey(uuid)) {
            this.timePlayed.put(uuid, 0);
            this.savePlayTime();
        }
        this.timeLoggedIn.put(uuid, (int) (System.currentTimeMillis() / 1000));
    }

    public void setPlayerLoggedOut(OfflinePlayer player) {
        setPlayerLoggedOut(player.getUUIDString());
    }

    public void setPlayerLoggedOut(Player player) {
        setPlayerLoggedOut(player.getUUIDString());
    }

    private void setPlayerLoggedOut(String uuid) {
        if (this.timeLoggedIn.containsKey(uuid)) {
            int timePlayed = (int) ((System.currentTimeMillis() / 1000) - this.timeLoggedIn.get(uuid));
            if (this.timePlayed.containsKey(uuid)) {
                timePlayed += this.timePlayed.get(uuid);
            }
            if (timePlayed > this.getTimeAllowedInSeconds()) {
                timePlayed = this.getTimeAllowedInSeconds();
            }
            this.timePlayed.put(uuid, timePlayed);
            this.timeLoggedIn.remove(uuid);
            getLogman().info(
                    "Player " + Canary.getServer().getPlayerFromUUID(uuid).getName() + " played for a total of "
                            + timePlayed + " seconds!");
            this.savePlayTime();
        }
        if (this.seenWarningMessages.containsKey(uuid + ":10")) {
            this.seenWarningMessages.remove(uuid + ":10");
        }
        if (this.seenWarningMessages.containsKey(uuid + ":60")) {
            this.seenWarningMessages.remove(uuid + ":60");
        }
        if (this.seenWarningMessages.containsKey(uuid + ":300")) {
            this.seenWarningMessages.remove(uuid + ":300");
        }
    }

    public boolean hasPlayerSeenMessage(OfflinePlayer player, int time) {
        return hasPlayerSeenMessage(player.getUUIDString(), time);
    }

    public boolean hasPlayerSeenMessage(Player player, int time) {
        return hasPlayerSeenMessage(player.getUUIDString(), time);
    }

    private boolean hasPlayerSeenMessage(String uuid, int time) {
        if (this.seenWarningMessages.containsKey(uuid + ":" + time)) {
            return this.seenWarningMessages.get(uuid + ":" + time);
        } else {
            return false;
        }
    }

    public void sentPlayerWarningMessage(OfflinePlayer player, int time) {
        sentPlayerWarningMessage(player.getUUIDString(), time);
    }

    public void sentPlayerWarningMessage(Player player, int time) {
        sentPlayerWarningMessage(player.getUUIDString(), time);
    }

    private void sentPlayerWarningMessage(String uuid, int time) {
        this.seenWarningMessages.put(uuid + ":" + time, true);
    }

    public boolean start() {
        if (this.started) {
            return false;
        } else {
            this.started = true;
            String initial = (getConfig().getInt("initialTime") / 60 / 60) + "";
            String perday = (getConfig().getInt("timePerDay") / 60 / 60) + "";
            Canary.getServer().broadcastMessage(
                    Colors.GREEN + "Playtime has now started! You have " + initial
                            + " hour/s of playtime to start with and " + perday + " hour/s of playtime added per day!");
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

    public void loadPlayTime(OfflinePlayer player) {
        loadPlayTime(player.getUUIDString());
    }

    public void loadPlayTime(Player player) {
        loadPlayTime(player.getUUIDString());
    }

    private void loadPlayTime(String uuid) {
        if (!hasStarted()) {
            return;
        }

        // Old data file, code transfers old data over
        File file = new File(getDataFolder(), "playtime.json");
        if (!file.exists()) {
            getLogman().debug("old data file doesn't exist, no data will be transfered over.");
        } else {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
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
            file.renameTo(new File(getDataFolder(), "oldplaytim.json"));
        }

        PlayTimeDataAccess dataAccess = new PlayTimeDataAccess();
        try {
            HashMap<String, Object> filter = new HashMap<String, Object>();
            filter.put("player_uuid", uuid);

            Database.get().load(dataAccess, filter);
        } catch (DatabaseReadException e) {
            getLogman().warn("Failed to read from database", e);
        }
        if (dataAccess.hasData()) {
            timePlayed.put(dataAccess.uuid, dataAccess.playtime);
        } else {
            timePlayed.put(uuid, 0);
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
                this.setPlayerLoggedOut(Canary.getServer().getOfflinePlayer(key));
            }
        }

        for (String key : this.timePlayed.keySet()) {
            PlayTimeDataAccess dataAccess = new PlayTimeDataAccess();
            dataAccess.uuid = key;
            dataAccess.playtime = this.timePlayed.get(key);

            HashMap<String, Object> filter = new HashMap<String, Object>();
            filter.put("player_uuid", key);

            try {
                Database.get().update(dataAccess, filter);
            } catch (DatabaseWriteException e) {
                getLogman().warn("Failed to write to database", e);
            }
        }
    }

    public File getDataFolder() {
        return new File(Canary.getWorkingPath() + "/config/PlayTimeLimiter");
    }
}