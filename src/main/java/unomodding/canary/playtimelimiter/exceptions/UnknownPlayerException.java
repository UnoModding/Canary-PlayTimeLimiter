/**
 * Copyright 2014 by UnoModding, RyanTheAlmighty and Contributors
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 3.0 Unported License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/3.0/.
 */
package unomodding.canary.playtimelimiter.exceptions;

import java.util.UUID;

public class UnknownPlayerException extends Exception {
    private static final long serialVersionUID = -5987543214085051018L;

    public UnknownPlayerException(UUID uuid) {
        super("Unknown player with UUID of " + uuid);
    }
}
