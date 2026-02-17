package com.werchat.integration.papi;

import at.helpch.placeholderapi.PlaceholderAPI;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;

public class PAPIImplementation implements PAPIIntegration {
    @Override
    public @NotNull String setPlaceholders(final PlayerRef player, final String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    @Override
    public @NotNull String setRelationalPlaceholders(final PlayerRef one, final PlayerRef two, final String text) {
        return PlaceholderAPI.setRelationalPlaceholders(one, two, text);
    }
}
