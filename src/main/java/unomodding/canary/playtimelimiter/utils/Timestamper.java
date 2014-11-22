/**
 * Copyright 2013-2014 by UnoModding, ATLauncher and Contributors
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 3.0 Unported License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/3.0/.
 */
package unomodding.canary.playtimelimiter.utils;

import net.canarymod.ToolBox;

public final class Timestamper {
    public static String now() {
        return ToolBox.formatTimestamp(ToolBox.getUnixTimestamp());
    }
}
