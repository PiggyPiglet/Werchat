package com.werchat.integration.papi;

import at.helpch.placeholderapi.PlaceholderAPI;
import at.helpch.placeholderapi.expansion.Configurable;
import at.helpch.placeholderapi.expansion.PlaceholderExpansion;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.werchat.WerchatPlugin;
import com.werchat.channels.Channel;
import com.werchat.channels.ChannelManager;
import com.werchat.storage.PlayerDataManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class WerchatExpansion extends PlaceholderExpansion implements Configurable<WerchatExpansionConfig> {
    private final WerchatPlugin main;

    public WerchatExpansion(@NotNull final WerchatPlugin main) {
        this.main = main;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "werchat";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Wer";
    }

    @Override
    public @NotNull String getVersion() {
        return main.getManifest().getVersion().toString();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        final String activeChannel = getConfig().activeChannel();

        main.getChannelManager().getAllChannels().stream()
                .map(Channel::getName)
                .filter(activeChannel::equalsIgnoreCase)
                .findAny()
                .ifPresent(channel -> main.getLogger().atWarning().log("[Werchat PAPI Expansion] There is a channel with the same name as your configured active channel identifier for the Werchat PAPI Expansion (configurable in mods/HelpChat_PlaceholderAPI/config.yml). You will not be able to access targeted placeholder data about the channel of the same name, the name \"" + activeChannel + "\" will only target the player's current active channel."));

        return super.canRegister();
    }

    @Override
    public @Nullable String onPlaceholderRequest(final PlayerRef playerRef, @NotNull String params) {
        final ChannelManager channelManager = main.getChannelManager();
        final PlayerDataManager playerDataManager = main.getPlayerDataManager();
        final UUID uuid = playerRef.getUuid();

        final int channelIndex = params.indexOf("channel_");
        if (channelIndex != -1) {
            params = params.substring(7);
            int underscoreIndex = params.indexOf('_');

            if (underscoreIndex == -1) {
                return null;
            }

            underscoreIndex = params.indexOf('_');
            final String channelName = params.substring(0, underscoreIndex + 1);
            params = params.substring(underscoreIndex);

            final Channel channel;

            if (channelName.equalsIgnoreCase(getConfig().activeChannel())) {
                channel = channelManager.getChannel(playerDataManager.getFocusedChannel(uuid));
            } else {
                channel = channelManager.getChannel(channelName);
            }

            return switch (params) {
                case "name" -> channel.getName();
                case "nick" -> channel.getNick();
                case "colorhex" -> channel.getColorHex();
                case "format" -> channel.getFormat();
                case "password" -> channel.getPassword(); // potentially dangerous?
                case "color" -> String.valueOf(channel.getColor());
                case "effective_msg_colorhex" -> channel.getEffectiveMessageColorHex();
                case "join_permission" -> channel.getJoinPermission();
                case "msg_color_hex" -> channel.getMessageColorHex();
                case "msg_color" -> String.valueOf(channel.getMessageColor());
                case "quickchatsymbol" -> channel.getQuickChatSymbol();
                case "see_permission" -> channel.getSeePermission();
                case "speak_permission" -> channel.getSpeakPermission();
                case "worlds_display" -> channel.getWorldsDisplay();
                case "worlds_count" -> String.valueOf(channel.getWorlds().size());
                case "worlds" -> String.join(", ", channel.getWorlds());
                case "distance" -> String.valueOf(channel.getDistance());
                case "member_count" -> String.valueOf(channel.getMemberCount());
                case "moderator_count" -> String.valueOf(channel.getModerators().size());
                case "muted_count" -> String.valueOf(channel.getMuted().size());
                case "owner" -> String.valueOf(channel.getOwner());
                case "owner_name" -> Optional.ofNullable(Universe.get().getPlayer(channel.getOwner()))
                        .map(PlayerRef::getUsername)
                        .orElse("null");
                case "is_autojoin" -> PlaceholderAPI.booleanValue(channel.isAutoJoin());
                case "is_muted" -> PlaceholderAPI.booleanValue(channel.isMuted(uuid));
                case "is_banned" -> PlaceholderAPI.booleanValue(channel.isBanned(uuid));
                case "is_member" -> PlaceholderAPI.booleanValue(channel.isMember(uuid));
                case "is_moderator" -> PlaceholderAPI.booleanValue(channel.isModerator(uuid));
                case "is_verbose" -> PlaceholderAPI.booleanValue(channel.isVerbose());
                case "is_default" -> PlaceholderAPI.booleanValue(channel.isDefault());
                case "is_focusable" -> PlaceholderAPI.booleanValue(channel.isFocusable());
                case "is_global" -> PlaceholderAPI.booleanValue(channel.isGlobal());
                case "is_local" -> PlaceholderAPI.booleanValue(channel.isLocal());
                case "has_password" -> PlaceholderAPI.booleanValue(channel.hasPassword());
                case "has_msg_color" -> PlaceholderAPI.booleanValue(channel.hasMessageColor());
                case "has_worlds" -> PlaceholderAPI.booleanValue(channel.hasWorlds());
                case "has_quickchatsymbol" -> PlaceholderAPI.booleanValue(channel.hasQuickChatSymbol());
                default -> null;
            };
        }

        return switch(params) {
            case "channels_total" -> String.valueOf(channelManager.getAllChannels().size());
            case "channels" -> channelManager.getAllChannels().stream()
                    .map(Channel::getName)
                    .collect(Collectors.joining(", "));
            case "default_channel" -> channelManager.getDefaultChannel().getName();
            case "channel" -> playerDataManager.getFocusedChannel(uuid);
            case "ignored_players_total" -> String.valueOf(playerDataManager.getIgnoredPlayers(uuid).size());
            case "ignored_players" -> playerDataManager.getIgnoredPlayers(uuid).stream()
                    .map(Universe.get()::getPlayer)
                    .filter(Objects::nonNull)
                    .map(PlayerRef::getUsername)
                    .collect(Collectors.joining(", "));
            case "known_name" -> playerDataManager.getKnownName(uuid);
            case "display_colour" -> playerDataManager.getDisplayColor(uuid);
            case "msg_color" -> playerDataManager.getMsgColor(uuid);
            case "msg_gradient_end" -> playerDataManager.getMsgGradientEnd(uuid);
            case "nick_color" -> playerDataManager.getNickColor(uuid);
            case "nick_gradient_end" -> playerDataManager.getNickGradientEnd(uuid);
            case "nick" -> playerDataManager.getNickname(uuid);
            case "display_name" -> playerDataManager.getDisplayName(uuid);
            default -> null;
        };
    }

    @Override
    public @NotNull Class<WerchatExpansionConfig> provideConfigType() {
        return WerchatExpansionConfig.class;
    }

    @Override
    public @NotNull WerchatExpansionConfig provideDefault() {
        return new WerchatExpansionConfig("active");
    }
}
