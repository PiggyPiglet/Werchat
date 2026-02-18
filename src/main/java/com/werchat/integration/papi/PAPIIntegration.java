package com.werchat.integration.papi;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.werchat.WerchatPlugin;

/**
 * Soft bridge to PlaceholderAPI to avoid hard runtime dependency.
 */
public interface PAPIIntegration {

    static PAPIIntegration register(WerchatPlugin plugin) {
        try {
            Class.forName("at.helpch.placeholderapi.PlaceholderAPI");
            return new PAPIImplementation(plugin);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    String setPlaceholders(PlayerRef player, String text);

    String setRelationalPlaceholders(PlayerRef one, PlayerRef two, String text);

    /**
     * Best-effort registration hook for integrations that need %werchat_*%
     * placeholders available before first chat message formatting.
     */
    default void ensureExpansionRegistered(PlayerRef player) {
        // Optional for implementations.
    }
}
