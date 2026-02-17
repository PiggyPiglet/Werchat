package com.werchat.integration.papi;

import org.jetbrains.annotations.NotNull;

public class WerchatExpansionConfig {
    private String activeChannel;

    public WerchatExpansionConfig(@NotNull final String activeChannel) {
        this.activeChannel = activeChannel;
    }

    public String activeChannel() {
        return activeChannel;
    }
}
