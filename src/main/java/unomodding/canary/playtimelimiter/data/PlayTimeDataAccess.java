/**
 * Copyright 2014 by UnoModding and Contributors
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 3.0 Unported License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/3.0/.
 */
package unomodding.canary.playtimelimiter.data;

import net.canarymod.database.Column;
import net.canarymod.database.Column.DataType;
import net.canarymod.database.DataAccess;

public class PlayTimeDataAccess extends DataAccess {
    public PlayTimeDataAccess() {
        super("playtime");
    }

    @Column(columnName = "player_uuid", dataType = DataType.STRING)
    public String uuid;

    @Column(columnName = "player_playtime", dataType = DataType.INTEGER)
    public int playtime;

    @Override
    public DataAccess getInstance() {
        return new PlayTimeDataAccess();
    }
}