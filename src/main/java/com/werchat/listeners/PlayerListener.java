package com.werchat.listeners;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.werchat.WerchatPlugin;
import com.werchat.channels.Channel;
import com.werchat.channels.ChannelManager;
import com.werchat.config.WerchatConfig;
import com.werchat.storage.PlayerDataManager;

import java.util.UUID;

/**
 * Handles player join/leave for channel management
 */
public class PlayerListener {

    private final ChannelManager channelManager;
    private final PlayerDataManager playerDataManager;
    private final WerchatConfig config;

    public PlayerListener(WerchatPlugin plugin) {
        this.channelManager = plugin.getChannelManager();
        this.playerDataManager = plugin.getPlayerDataManager();
        this.config = plugin.getConfig();
    }

    public void onPlayerConnect(PlayerConnectEvent event) {
        PlayerRef player = event.getPlayerRef();
        UUID playerId = player.getUuid();
        String playerName = player.getUsername();

        // Track online player
        playerDataManager.trackPlayer(playerId, player);

        // Auto-join channels flagged with autoJoin, optionally skipping default when disabled in config
        for (Channel channel : channelManager.getAllChannels()) {
            boolean shouldAutoJoin = channel.isAutoJoin();
            if (channel.isDefault() && !config.isAutoJoinDefault()) {
                shouldAutoJoin = false;
            }
            if (shouldAutoJoin && !channel.isBanned(playerId)) {
                channel.addMember(playerId);
            }
        }

        Channel defaultChannel = channelManager.getDefaultChannel();
        if (defaultChannel != null && config.isAutoJoinDefault() && !defaultChannel.isBanned(playerId)) {
            defaultChannel.addMember(playerId);
        }

        // Set focused channel if none set
        if (playerDataManager.getFocusedChannel(playerId) == null) {
            if (defaultChannel != null) {
                playerDataManager.setFocusedChannel(playerId, defaultChannel.getName());
            }
        }

        if (config.isShowJoinLeaveMessages()) {
            broadcastMembershipEvent(playerName + " joined the server", playerId);
        }
    }

    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef player = event.getPlayerRef();
        UUID playerId = player.getUuid();
        String playerName = player.getUsername();

        if (config.isShowJoinLeaveMessages()) {
            broadcastMembershipEvent(playerName + " left the server", playerId);
        }

        // Don't remove from channels - persist membership across sessions
        playerDataManager.untrackPlayer(playerId);
        playerDataManager.clearTransientData(playerId);
    }

    private void broadcastMembershipEvent(String text, UUID playerId) {
        for (Channel channel : channelManager.getPlayerChannels(playerId)) {
            if (channel.isVerbose()) {
                broadcastToChannel(channel, text);
            }
        }
    }

    private void broadcastToChannel(Channel channel, String message) {
        Message formatted = Message.join(
            Message.raw("[" + channel.getNick() + "] ").color(channel.getColorHex()),
            Message.raw(message).color("#FFFF55")
        );
        for (UUID memberId : channel.getMembers()) {
            PlayerRef member = playerDataManager.getOnlinePlayer(memberId);
            if (member != null) {
                member.sendMessage(formatted);
            }
        }
    }
}
