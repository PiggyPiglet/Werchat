package com.werchat.integration.papi;

import at.helpch.placeholderapi.PlaceholderAPI;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class PAPIImplementation implements PAPIIntegration {
    @Override
    public String setPlaceholders(PlayerRef player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    @Override
    public String setRelationalPlaceholders(PlayerRef one, PlayerRef two, String text) {
        return PlaceholderAPI.setRelationalPlaceholders(one, two, text);
    }
}
