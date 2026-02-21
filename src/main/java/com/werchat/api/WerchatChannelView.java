package com.werchat.api;

import java.util.Set;

/**
 * Snapshot view of a Werchat channel for external integrations.
 */
public record WerchatChannelView(
    String name,
    String nick,
    String colorHex,
    String effectiveMessageColorHex,
    String description,
    boolean descriptionEnabled,
    String motd,
    boolean motdEnabled,
    int distance,
    boolean isDefault,
    boolean autoJoin,
    boolean quickChatEnabled,
    String quickChatSymbol,
    Set<String> worlds
) {}
