package com.werchat.api;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Public integration surface for Werchat.
 */
public interface WerchatAPI {

    Collection<WerchatChannelView> getChannels();

    Optional<WerchatChannelView> getChannel(String channelInput);

    String getFocusedChannel(UUID playerId);

    boolean setFocusedChannel(UUID playerId, String channelInput);

    boolean joinChannel(UUID playerId, String channelInput, String password);

    boolean leaveChannel(UUID playerId, String channelInput);

    boolean isMember(UUID playerId, String channelInput);

    /**
     * Relay chat through Werchat's normal processing path (quick-chat, mute, cooldown, filter, etc).
     */
    boolean relayChat(UUID senderId, String message);
}
