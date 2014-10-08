/**
 * Copyright 2014 by RyanTheAlmighty, UnoModding and Contributors
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 3.0 Unported License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/3.0/.
 */
package unomodding.canary.playtimelimiter.exceptions;

public class UnknownPlayerException extends Exception {
    private static final long serialVersionUID = -5987543214085051018L;

    public UnknownPlayerException(String uuid) {
        super("Unknown player with UUID of " + uuid);
    }
}
