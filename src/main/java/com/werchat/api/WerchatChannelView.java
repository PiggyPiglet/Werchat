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
    int distance,
    boolean isDefault,
    boolean autoJoin,
    boolean quickChatEnabled,
    String quickChatSymbol,
    Set<String> worlds
) {}
