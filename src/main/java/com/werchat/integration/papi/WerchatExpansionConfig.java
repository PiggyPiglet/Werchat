package com.werchat.integration.papi;

public class WerchatExpansionConfig {
    private String activeChannel;

    public WerchatExpansionConfig(String activeChannel) {
        this.activeChannel = activeChannel;
    }

    public String activeChannel() {
        return activeChannel;
    }
}
