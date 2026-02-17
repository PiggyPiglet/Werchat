package com.werchat.integration.papi;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.werchat.WerchatPlugin;

import java.util.logging.Level;

/**
 * Soft bridge to PlaceholderAPI to avoid hard runtime dependency.
 */
public interface PAPIIntegration {

    static PAPIIntegration register(WerchatPlugin plugin) {
        try {
            Class.forName("at.helpch.placeholderapi.PlaceholderAPI");

            WerchatExpansion expansion = new WerchatExpansion(plugin);
            if (expansion.register()) {
                plugin.getLogger().at(Level.INFO).log("PlaceholderAPI integration enabled");
            } else {
                plugin.getLogger().at(Level.WARNING).log("PlaceholderAPI detected, but Werchat expansion could not be registered");
            }

            return new PAPIImplementation();
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).log("PlaceholderAPI integration failed: %s", e.getMessage());
            return null;
        }
    }

    String setPlaceholders(PlayerRef player, String text);

    String setRelationalPlaceholders(PlayerRef one, PlayerRef two, String text);
}
