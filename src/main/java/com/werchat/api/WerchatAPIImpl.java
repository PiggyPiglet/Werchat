package com.werchat.api;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.werchat.WerchatPlugin;
import com.werchat.channels.Channel;
import com.werchat.channels.ChannelManager;
import com.werchat.listeners.ChatListener;
import com.werchat.storage.PlayerDataManager;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default Werchat API implementation.
 */
public class WerchatAPIImpl implements WerchatAPI {

    private final ChannelManager channelManager;
    private final PlayerDataManager playerDataManager;
    private final ChatListener chatListener;

    public WerchatAPIImpl(WerchatPlugin plugin) {
        this.channelManager = plugin.getChannelManager();
        this.playerDataManager = plugin.getPlayerDataManager();
        this.chatListener = plugin.getChatListener();
    }

    @Override
    public Collection<WerchatChannelView> getChannels() {
        return channelManager.getAllChannels().stream()
            .map(this::toView)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<WerchatChannelView> getChannel(String channelInput) {
        Channel channel = channelManager.findChannel(channelInput);
        return Optional.ofNullable(channel).map(this::toView);
    }

    @Override
    public String getFocusedChannel(UUID playerId) {
        return playerDataManager.getFocusedChannel(playerId);
    }

    @Override
    public boolean setFocusedChannel(UUID playerId, String channelInput) {
        Channel channel = channelManager.findChannel(channelInput);
        if (channel == null || !channel.isMember(playerId)) {
            return false;
        }

        playerDataManager.setFocusedChannel(playerId, channel.getName());
        return true;
    }

    @Override
    public boolean joinChannel(UUID playerId, String channelInput, String password) {
        Channel channel = channelManager.findChannel(channelInput);
        if (channel == null || channel.isBanned(playerId) || channel.isMember(playerId)) {
            return false;
        }

        if (channel.hasPassword() && !channel.checkPassword(password)) {
            return false;
        }

        channel.addMember(playerId);
        return true;
    }

    @Override
    public boolean leaveChannel(UUID playerId, String channelInput) {
        Channel channel = channelManager.findChannel(channelInput);
        if (channel == null || !channel.isMember(playerId)) {
            return false;
        }

        channel.removeMember(playerId);

        String focusedChannel = playerDataManager.getFocusedChannel(playerId);
        if (focusedChannel != null && focusedChannel.equalsIgnoreCase(channel.getName())) {
            Channel fallback = channelManager.getDefaultChannel();
            if (fallback != null) {
                playerDataManager.setFocusedChannel(playerId, fallback.getName());
            }
        }

        return true;
    }

    @Override
    public boolean isMember(UUID playerId, String channelInput) {
        Channel channel = channelManager.findChannel(channelInput);
        return channel != null && channel.isMember(playerId);
    }

    @Override
    public boolean relayChat(UUID senderId, String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        PlayerRef sender = playerDataManager.getOnlinePlayer(senderId);
        if (sender == null) {
            return false;
        }

        chatListener.handleChatInput(sender, message);
        return true;
    }

    private WerchatChannelView toView(Channel channel) {
        Set<String> worlds = Set.copyOf(channel.getWorlds());
        return new WerchatChannelView(
            channel.getName(),
            channel.getNick(),
            channel.getColorHex(),
            channel.getEffectiveMessageColorHex(),
            channel.getDistance(),
            channel.isDefault(),
            channel.isAutoJoin(),
            channel.isQuickChatEnabled(),
            channel.getQuickChatSymbol(),
            worlds
        );
    }
}
