package com.werchat.integration.papi;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.werchat.WerchatPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PAPIIntegration {
    @Nullable
    static PAPIIntegration register(@NotNull final WerchatPlugin main) {
        try {
            Class.forName("at.helpch.placeholderapi.PlaceholderAPI");
            new WerchatExpansion(main).register();
            return new PAPIImplementation();
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @NotNull
    String setPlaceholders(PlayerRef player, String text);

    @NotNull
    String setRelationalPlaceholders(PlayerRef one, PlayerRef two, String text);
}
