package com.werchat.api;

/**
 * Controls how channel text input is resolved.
 */
public enum WerchatChannelLookupMode {
    /**
     * Requires exact match by channel name or nick.
     */
    EXACT,

    /**
     * Allows command-style fuzzy matching (prefixes, nick, etc).
     */
    FUZZY
}
