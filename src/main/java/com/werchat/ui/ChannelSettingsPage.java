package com.werchat.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.werchat.WerchatPlugin;
import com.werchat.channels.Channel;
import com.werchat.channels.ChannelManager;
import com.werchat.storage.PlayerDataManager;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Main Werchat settings UI page shown from /ch and /ch settings.
 * Includes moderator-only per-channel management tabs.
 */
public class ChannelSettingsPage extends InteractiveCustomUIPage<ChannelSettingsPage.PageEventData> {

    private static final String TAB_MAIN = "main";
    private static final String TAB_CHANNELS = "channels";
    private static final String TAB_MOD_PREFIX = "mod:";
    private static final Set<Integer> DISTANCE_PRESETS = Set.of(0, 25, 50, 100, 150, 250, 500);
    private static final int MAX_NICKNAME_LENGTH = 20;

    private final WerchatPlugin plugin;
    private final ChannelManager channelManager;
    private final PlayerDataManager playerDataManager;

    private String activeTab = TAB_MAIN;
    private String statusMessage = "Ready.";

    public ChannelSettingsPage(WerchatPlugin plugin, PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.plugin = plugin;
        this.channelManager = plugin.getChannelManager();
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder cmd,
        @Nonnull UIEventBuilder events,
        @Nonnull Store<EntityStore> store
    ) {
        cmd.append("Werchat/ChannelSettings.ui");
        bindStaticEvents(events);

        UUID viewerId = playerRef.getUuid();
        List<Channel> moderatorChannels = getModeratorChannels(viewerId);
        validateActiveTab(moderatorChannels);

        applyTabState(cmd);
        renderModeratorTabs(cmd, events, moderatorChannels);
        renderMainPanel(cmd, viewerId);
        renderChannels(cmd, events, viewerId);
        renderModeratorPanel(cmd, viewerId);
        cmd.set("#FooterMessage.Text", statusMessage);
    }

    @Override
    public void handleDataEvent(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull PageEventData data
    ) {
        super.handleDataEvent(ref, store, data);
        if (data.button == null || data.button.isBlank()) {
            return;
        }

        switch (data.button) {
            case "Close" -> close();
            case "Refresh" -> {
                statusMessage = "Refreshed.";
                rebuild();
            }
            case "Tab" -> {
                if (data.tab != null && !data.tab.isBlank()) {
                    activeTab = data.tab.toLowerCase(Locale.ROOT);
                    statusMessage = "Viewing " + activeTab + ".";
                }
                rebuild();
            }
            case "ModTab" -> {
                if (data.channel == null || data.channel.isBlank()) {
                    statusMessage = "Unknown moderator tab.";
                    rebuild();
                    return;
                }
                activeTab = TAB_MOD_PREFIX + data.channel.toLowerCase(Locale.ROOT);
                statusMessage = "Managing " + data.channel + ".";
                rebuild();
            }
            case "Focus" -> {
                handleFocusAction(data.channel);
                rebuild();
            }
            case "Join" -> {
                handleJoinAction(data.channel, data.password);
                rebuild();
            }
            case "Leave" -> {
                handleLeaveAction(data.channel);
                rebuild();
            }
            case "ModAction" -> {
                handleModeratorAction(data.action, data.target, data.value, data.password);
                rebuild();
            }
            case "ProfileAction" -> {
                handleProfileAction(data.action, data.value, data.extra);
                rebuild();
            }
            default -> {
                statusMessage = "Unknown UI action.";
                rebuild();
            }
        }
    }

    private void bindStaticEvents(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
            EventData.of("Button", "Close"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#RefreshButton",
            EventData.of("Button", "Refresh"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabMainButton",
            EventData.of("Button", "Tab").append("Tab", TAB_MAIN), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabChannelsButton",
            EventData.of("Button", "Tab").append("Tab", TAB_CHANNELS), false);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#MainSetFocusButton",
            EventData.of("Button", "ProfileAction").append("Action", "set_focus")
                .append("@Value", "#MainFocusDropdown.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#MainQuickJoinButton",
            EventData.of("Button", "ProfileAction").append("Action", "quick_join")
                .append("@Value", "#MainFocusDropdown.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#MainQuickLeaveButton",
            EventData.of("Button", "ProfileAction").append("Action", "quick_leave")
                .append("@Value", "#MainFocusDropdown.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#MainQuickChannelsButton",
            EventData.of("Button", "Tab").append("Tab", TAB_CHANNELS), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#MainSetNickButton",
            EventData.of("Button", "ProfileAction").append("Action", "set_nickname")
                .append("@Value", "#MainNickInput.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#MainClearNickButton",
            EventData.of("Button", "ProfileAction").append("Action", "clear_nickname"), false);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#MainApplyNickColorButton",
            EventData.of("Button", "ProfileAction").append("Action", "set_nick_solid")
                .append("@Value", "#MainNickStartColorPicker.Color"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#MainApplyNickGradientButton",
            EventData.of("Button", "ProfileAction").append("Action", "set_nick_gradient")
                .append("@Value", "#MainNickStartColorPicker.Color")
                .append("@Extra", "#MainNickEndColorPicker.Color"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#MainClearNickColorButton",
            EventData.of("Button", "ProfileAction").append("Action", "clear_nick_color"), false);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#MainApplyMsgColorButton",
            EventData.of("Button", "ProfileAction").append("Action", "set_msg_solid")
                .append("@Value", "#MainMsgStartColorPicker.Color"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#MainApplyMsgGradientButton",
            EventData.of("Button", "ProfileAction").append("Action", "set_msg_gradient")
                .append("@Value", "#MainMsgStartColorPicker.Color")
                .append("@Extra", "#MainMsgEndColorPicker.Color"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#MainClearMsgColorButton",
            EventData.of("Button", "ProfileAction").append("Action", "clear_msg_color"), false);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#ModBanButton",
            EventData.of("Button", "ModAction").append("Action", "ban")
                .append("@Target", "#ModTargetInput.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ModUnbanButton",
            EventData.of("Button", "ModAction").append("Action", "unban")
                .append("@Target", "#ModTargetInput.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ModMuteButton",
            EventData.of("Button", "ModAction").append("Action", "mute")
                .append("@Target", "#ModTargetInput.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ModUnmuteButton",
            EventData.of("Button", "ModAction").append("Action", "unmute")
                .append("@Target", "#ModTargetInput.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ModAddModeratorButton",
            EventData.of("Button", "ModAction").append("Action", "addmod")
                .append("@Target", "#ModTargetInput.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ModRemoveModeratorButton",
            EventData.of("Button", "ModAction").append("Action", "removemod")
                .append("@Target", "#ModTargetInput.Value"), false);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#ModSetNickButton",
            EventData.of("Button", "ModAction").append("Action", "set_nick")
                .append("@Value", "#ModNickInput.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ModSetDistanceButton",
            EventData.of("Button", "ModAction").append("Action", "set_distance")
                .append("@Value", "#ModDistanceDropdown.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ModSetCustomDistanceButton",
            EventData.of("Button", "ModAction").append("Action", "set_distance")
                .append("@Value", "#ModDistanceInput.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ModSetPasswordButton",
            EventData.of("Button", "ModAction").append("Action", "set_password")
                .append("@Password", "#ModPasswordInput.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ModClearPasswordButton",
            EventData.of("Button", "ModAction").append("Action", "clear_password"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ModSetTagColorButton",
            EventData.of("Button", "ModAction").append("Action", "set_tag_color")
                .append("@Value", "#ModTagColorPicker.Color"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ModSetTextColorButton",
            EventData.of("Button", "ModAction").append("Action", "set_text_color")
                .append("@Value", "#ModTextColorPicker.Color"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ModClearTextColorButton",
            EventData.of("Button", "ModAction").append("Action", "clear_text_color"), false);
    }

    private void applyTabState(UICommandBuilder cmd) {
        boolean main = TAB_MAIN.equals(activeTab);
        boolean channels = TAB_CHANNELS.equals(activeTab);
        boolean moderator = activeTab.startsWith(TAB_MOD_PREFIX);

        cmd.set("#MainPanel.Visible", main);
        cmd.set("#ChannelsPanel.Visible", channels);
        cmd.set("#ModeratorPanel.Visible", moderator);
        cmd.set("#ModeratorTabsSection.Visible", channels);

        cmd.set("#TabMainButton.Text", main ? "[Main]" : "Main");
        cmd.set("#TabChannelsButton.Text", channels ? "[Channels]" : "Channels");
    }

    private void renderModeratorTabs(UICommandBuilder cmd, UIEventBuilder events, List<Channel> moderatorChannels) {
        cmd.clear("#ModeratorTabs");
        for (int i = 0; i < moderatorChannels.size(); i++) {
            Channel channel = moderatorChannels.get(i);
            cmd.append("#ModeratorTabs", "Werchat/ModeratorTabButton.ui");

            String selector = "#ModeratorTabs[" + i + "] #ModeratorTabButton";
            String shortName = getModeratorTabShortName(channel);
            String label = activeTab.equals(TAB_MOD_PREFIX + channel.getName().toLowerCase(Locale.ROOT))
                ? "[M:" + shortName + "]"
                : "M:" + shortName;
            cmd.set(selector + ".Text", label);
            cmd.set(selector + ".TooltipText", "Manage " + channel.getName());

            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector,
                EventData.of("Button", "ModTab").append("Channel", channel.getName()),
                false
            );
        }

        cmd.set("#ModeratorTabsLabel.Text", "Moderator Tabs (" + moderatorChannels.size() + ")");
        cmd.set("#ModeratorTabs.Visible", !moderatorChannels.isEmpty());
        cmd.set("#ModeratorTabsEmptyLabel.Visible", moderatorChannels.isEmpty());
    }

    private String getModeratorTabShortName(Channel channel) {
        String value = channel.getNick();
        if (value == null || value.isBlank()) {
            value = channel.getName();
        }

        String cleaned = value.replace("[", "").replace("]", "").trim();
        if (cleaned.isEmpty()) {
            cleaned = channel.getName();
        }
        if (cleaned.length() > 7) {
            return cleaned.substring(0, 6) + "~";
        }
        return cleaned;
    }

    private void renderMainPanel(UICommandBuilder cmd, UUID viewerId) {
        String focused = playerDataManager.getFocusedChannel(viewerId);
        String displayName = playerDataManager.getDisplayName(viewerId);
        String nickname = playerDataManager.getNickname(viewerId);
        String nickColor = playerDataManager.getNickColor(viewerId);
        String nickGradient = playerDataManager.getNickGradientEnd(viewerId);
        String msgColor = playerDataManager.getMsgColor(viewerId);
        String msgGradient = playerDataManager.getMsgGradientEnd(viewerId);

        List<Channel> focusableChannels = getFocusableChannels(viewerId);
        List<DropdownEntryInfo> focusEntries = new ArrayList<>();
        List<String> focusValues = new ArrayList<>();
        for (Channel channel : focusableChannels) {
            String label = channel.getName();
            if (channel.getNick() != null && !channel.getNick().isBlank()
                && !channel.getNick().equalsIgnoreCase(channel.getName())) {
                label = channel.getNick() + " (" + channel.getName() + ")";
            }
            focusEntries.add(new DropdownEntryInfo(LocalizableString.fromString(label), channel.getName()));
            focusValues.add(channel.getName());
        }
        if (focusEntries.isEmpty()) {
            focusEntries.add(new DropdownEntryInfo(LocalizableString.fromString("No joined channels"), ""));
            focusValues.add("");
        }

        cmd.set("#MainFocusDropdown.Entries", focusEntries);

        String selectedFocus = focused;
        if (selectedFocus == null || selectedFocus.isBlank()) {
            selectedFocus = focusValues.get(0);
        }
        if (!containsDropdownValue(focusValues, selectedFocus)) {
            selectedFocus = focusValues.get(0);
        }
        cmd.set("#MainFocusDropdown.Value", selectedFocus == null ? "" : selectedFocus);

        int joinedCount = 0;
        List<String> joinedChannelNames = new ArrayList<>();
        for (Channel channel : channelManager.getAllChannels()) {
            if (channel.isMember(viewerId)) {
                joinedCount++;
                joinedChannelNames.add(channel.getName());
            }
        }
        joinedChannelNames.sort(String.CASE_INSENSITIVE_ORDER);

        renderDisplayNamePreview(cmd, displayName, nickColor, nickGradient);
        cmd.set("#MainJoinedCountValue.Text", String.valueOf(joinedCount));
        cmd.set("#MainJoinedChannelsValue.Text", summarizeJoinedChannels(joinedChannelNames));
        cmd.set("#MainRecentChannelsValue.Text", buildChannelStrip(joinedChannelNames, focused));
        cmd.set("#MainFocusedChannelValue.Text", focused == null || focused.isBlank() ? "None" : focused);

        cmd.set("#MainNicknameValue.Text", (nickname == null || nickname.isBlank()) ? "Not set" : nickname);
        cmd.set("#MainNickColorValue.Text", formatColorValue(nickColor, nickGradient));
        cmd.set("#MainMsgColorValue.Text", formatColorValue(msgColor, msgGradient));
        cmd.set("#MainMsgPreviewValue.Text", buildMessagePreviewText(msgColor, msgGradient));
        cmd.set("#MainMsgPreviewValue.Style.TextColor", resolveMessagePreviewColor(msgColor, focused));

        cmd.set("#MainNickInput.Value", nickname == null ? "" : nickname);
        cmd.set("#MainNickStartColorPicker.Color", defaultColor(nickColor));
        cmd.set("#MainNickEndColorPicker.Color", defaultColor(nickGradient != null ? nickGradient : nickColor));
        cmd.set("#MainMsgStartColorPicker.Color", defaultColor(msgColor));
        cmd.set("#MainMsgEndColorPicker.Color", defaultColor(msgGradient != null ? msgGradient : msgColor));

        boolean canSwitch = hasPermission(viewerId, "werchat.switch");
        boolean canNick = hasPermission(viewerId, "werchat.playernick");
        boolean canNickColor = hasPermission(viewerId, "werchat.nickcolor");
        boolean canMsgColor = hasPermission(viewerId, "werchat.msgcolor");
        Channel selectedChannel = (selectedFocus == null || selectedFocus.isBlank())
            ? null
            : channelManager.findChannel(selectedFocus);

        boolean selectedMember = selectedChannel != null && selectedChannel.isMember(viewerId);
        boolean quickJoinAllowed = selectedChannel != null
            && !selectedMember
            && !selectedChannel.isBanned(viewerId)
            && !selectedChannel.hasPassword()
            && (!plugin.getConfig().isEnforceChannelPermissions()
                || (hasPermission(viewerId, selectedChannel.getJoinPermission())
                && hasPermission(viewerId, selectedChannel.getReadPermission())));
        boolean quickLeaveAllowed = selectedChannel != null && selectedMember && !selectedChannel.isDefault();

        cmd.set("#MainSetFocusButton.Disabled", !canSwitch || selectedFocus == null || selectedFocus.isBlank());
        cmd.set("#MainQuickJoinButton.Disabled", !quickJoinAllowed);
        cmd.set("#MainQuickLeaveButton.Disabled", !quickLeaveAllowed);
        cmd.set("#MainQuickChannelsButton.Disabled", false);
        cmd.set("#MainSetNickButton.Disabled", !canNick);
        cmd.set("#MainClearNickButton.Disabled", !canNick);
        cmd.set("#MainApplyNickColorButton.Disabled", !canNickColor);
        cmd.set("#MainApplyNickGradientButton.Disabled", !canNickColor);
        cmd.set("#MainClearNickColorButton.Disabled", !canNickColor);
        cmd.set("#MainApplyMsgColorButton.Disabled", !canMsgColor);
        cmd.set("#MainApplyMsgGradientButton.Disabled", !canMsgColor);
        cmd.set("#MainClearMsgColorButton.Disabled", !canMsgColor);
    }

    private List<Channel> getFocusableChannels(UUID viewerId) {
        List<Channel> channels = new ArrayList<>();
        for (Channel channel : channelManager.getAllChannels()) {
            boolean member = channel.isMember(viewerId);
            if (channel.isBanned(viewerId)) {
                continue;
            }

            if (plugin.getConfig().isEnforceChannelPermissions()) {
                if (!hasPermission(viewerId, channel.getReadPermission())) {
                    continue;
                }
                if (member || (!channel.hasPassword() && hasPermission(viewerId, channel.getJoinPermission()))) {
                    channels.add(channel);
                }
            } else {
                if (member || !channel.hasPassword()) {
                    channels.add(channel);
                }
            }
        }
        channels.sort(Comparator.comparing(channel -> channel.getName().toLowerCase(Locale.ROOT)));
        return channels;
    }

    private boolean containsDropdownValue(List<String> entries, String value) {
        if (value == null) {
            return false;
        }
        for (String entry : entries) {
            if (entry != null && value.equalsIgnoreCase(entry)) {
                return true;
            }
        }
        return false;
    }

    private String defaultColor(String value) {
        String normalized = normalizeHexColor(value);
        return normalized == null ? "#FFFFFF" : normalized;
    }

    private String formatColorValue(String start, String end) {
        String startHex = normalizeHexColor(start);
        String endHex = normalizeHexColor(end);
        if (startHex == null) {
            return "Default";
        }
        if (endHex != null) {
            return startHex + " -> " + endHex;
        }
        return startHex;
    }

    private String resolveMessagePreviewColor(String msgColor, String focusedChannelName) {
        String explicit = normalizeHexColor(msgColor);
        if (explicit != null) {
            return explicit;
        }

        if (focusedChannelName != null && !focusedChannelName.isBlank()) {
            Channel focusedChannel = channelManager.getChannel(focusedChannelName);
            if (focusedChannel != null) {
                String channelColor = normalizeHexColor(focusedChannel.getEffectiveMessageColorHex());
                if (channelColor != null) {
                    return channelColor;
                }
            }
        }
        return "#FFFFFF";
    }

    private String buildMessagePreviewText(String msgColor, String msgGradient) {
        String startHex = normalizeHexColor(msgColor);
        String endHex = normalizeHexColor(msgGradient);
        if (startHex != null && endHex != null) {
            return "Sample message (" + startHex + " -> " + endHex + ")";
        }
        if (startHex != null) {
            return "Sample message";
        }
        return "Sample message (channel default)";
    }

    private String summarizeJoinedChannels(List<String> channels) {
        if (channels == null || channels.isEmpty()) {
            return "None";
        }
        String joined = String.join(", ", channels);
        if (joined.length() <= 64) {
            return joined;
        }
        return joined.substring(0, 61) + "...";
    }

    private String buildChannelStrip(List<String> channels, String focused) {
        if (channels == null || channels.isEmpty()) {
            return "No joined channels yet.";
        }

        List<String> ordered = new ArrayList<>(channels);
        ordered.sort((left, right) -> {
            boolean leftFocused = focused != null && left.equalsIgnoreCase(focused);
            boolean rightFocused = focused != null && right.equalsIgnoreCase(focused);
            if (leftFocused && !rightFocused) {
                return -1;
            }
            if (!leftFocused && rightFocused) {
                return 1;
            }
            return left.compareToIgnoreCase(right);
        });

        int limit = Math.min(4, ordered.size());
        List<String> preview = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            String name = ordered.get(i);
            if (focused != null && name.equalsIgnoreCase(focused)) {
                preview.add("[" + name + "]");
            } else {
                preview.add(name);
            }
        }

        String strip = String.join(" | ", preview);
        if (ordered.size() > limit) {
            strip = strip + " | +" + (ordered.size() - limit);
        }
        return strip;
    }

    private void renderDisplayNamePreview(UICommandBuilder cmd, String displayName, String nickColor, String nickGradient) {
        String name = (displayName == null || displayName.isBlank()) ? "-" : displayName;
        String startColor = defaultColor(nickColor);
        String endColor = normalizeHexColor(nickGradient);

        if (endColor == null || endColor.equalsIgnoreCase(startColor) || name.length() < 2) {
            cmd.set("#MainDisplayNameStartValue.Text", name);
            cmd.set("#MainDisplayNameStartValue.Style.TextColor", startColor);
            cmd.set("#MainDisplayNameEndValue.Text", "");
            return;
        }

        int split = Math.max(1, name.length() / 2);
        cmd.set("#MainDisplayNameStartValue.Text", name.substring(0, split));
        cmd.set("#MainDisplayNameStartValue.Style.TextColor", startColor);
        cmd.set("#MainDisplayNameEndValue.Text", name.substring(split));
        cmd.set("#MainDisplayNameEndValue.Style.TextColor", endColor);
    }

    private void renderChannels(UICommandBuilder cmd, UIEventBuilder events, UUID viewerId) {
        cmd.clear("#ChannelRows");
        String focused = playerDataManager.getFocusedChannel(viewerId);

        int index = 0;
        for (Channel channel : channelManager.getAllChannels()) {
            if (plugin.getConfig().isEnforceChannelPermissions()
                && !hasPermission(viewerId, channel.getReadPermission())
                && !channel.isMember(viewerId)) {
                continue;
            }

            cmd.append("#ChannelRows", "Werchat/ChannelRow.ui");
            String base = "#ChannelRows[" + index + "]";

            cmd.set(base + " #ChannelTag.Text", "[" + channel.getNick() + "]");
            cmd.set(base + " #ChannelTag.Style.TextColor", channel.getColorHex());
            cmd.set(base + " #ChannelName.Text", channel.getName());
            cmd.set(base + " #ChannelName.Style.TextColor", channel.getColorHex());

            String meta = buildChannelMeta(channel, viewerId, focused);
            cmd.set(base + " #ChannelMeta.Text", meta);

            boolean member = channel.isMember(viewerId);
            boolean focusedChannel = focused != null && focused.equalsIgnoreCase(channel.getName());
            boolean canJoin = !member
                && !channel.isBanned(viewerId)
                && (!plugin.getConfig().isEnforceChannelPermissions() || hasPermission(viewerId, channel.getJoinPermission()));
            boolean canLeave = member && !channel.isDefault();
            boolean canFocus = member && !focusedChannel;

            cmd.set(base + " #JoinButton.Visible", canJoin);
            cmd.set(base + " #LeaveButton.Visible", canLeave);
            cmd.set(base + " #FocusButton.Visible", canFocus);
            cmd.set(base + " #FocusedChip.Visible", focusedChannel);

            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                base + " #JoinButton",
                EventData.of("Button", "Join")
                    .append("Channel", channel.getName())
                    .append("@Password", "#JoinPasswordInput.Value"),
                false
            );
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                base + " #LeaveButton",
                EventData.of("Button", "Leave").append("Channel", channel.getName()),
                false
            );
            events.addEventBinding(
                CustomUIEventBindingType.Activating,
                base + " #FocusButton",
                EventData.of("Button", "Focus").append("Channel", channel.getName()),
                false
            );
            index++;
        }

        cmd.set("#ChannelsEmptyLabel.Visible", index == 0);
    }

    private void renderModeratorPanel(UICommandBuilder cmd, UUID viewerId) {
        if (!activeTab.startsWith(TAB_MOD_PREFIX)) {
            return;
        }

        String channelName = activeTab.substring(TAB_MOD_PREFIX.length());
        Channel channel = channelManager.findChannel(channelName);
        if (channel == null || !isChannelModerator(viewerId, channel)) {
            statusMessage = "Moderator tab unavailable.";
            activeTab = TAB_CHANNELS;
            return;
        }

        cmd.set("#ModeratorChannelNameValue.Text", channel.getName());
        cmd.set("#ModeratorMemberCountValue.Text", "Members: " + channel.getMemberCount());
        cmd.set("#ModeratorMutedCountValue.Text", "Muted: " + channel.getMuted().size());
        cmd.set("#ModeratorBannedCountValue.Text", "Banned: " + channel.getBanned().size());
        cmd.set("#ModeratorModeratorsCountValue.Text", "Moderators: " + channel.getModerators().size());
        cmd.set("#ModeratorDistanceValue.Text", channel.isGlobal() ? "Distance: Global" : "Distance: " + channel.getDistance() + " blocks");
        cmd.set("#ModeratorPasswordValue.Text", "Password: " + (channel.hasPassword() ? "Enabled" : "None"));
        cmd.set("#ModeratorTagColorValue.Text", "Tag Color: " + channel.getColorHex());
        cmd.set("#ModeratorTextColorValue.Text", "Text Color: " + (channel.hasMessageColor() ? channel.getMessageColorHex() : "Default"));

        cmd.set("#ModNickInput.Value", channel.getNick());
        cmd.set("#ModDistanceDropdown.Value", toDistancePresetValue(channel.getDistance()));
        cmd.set("#ModDistanceInput.Value", String.valueOf(Math.max(0, channel.getDistance())));
        cmd.set("#ModTagColorPicker.Color", channel.getColorHex());
        cmd.set("#ModTextColorPicker.Color", channel.hasMessageColor() ? channel.getMessageColorHex() : channel.getColorHex());

    }

    private String buildChannelMeta(Channel channel, UUID viewerId, String focused) {
        StringBuilder meta = new StringBuilder();
        meta.append("Members: ").append(channel.getMemberCount());
        if (channel.hasPassword()) {
            meta.append(" | Password");
        }
        if (channel.isWorldRestricted()) {
            meta.append(" | Worlds: ").append(channel.getWorldsDisplay());
        }
        if (channel.isGlobal()) {
            meta.append(" | Global");
        } else {
            meta.append(" | ").append(channel.getDistance()).append(" blocks");
        }
        if (focused != null && focused.equalsIgnoreCase(channel.getName())) {
            meta.append(" | Selected");
        }
        if (channel.isModerator(viewerId)) {
            meta.append(" | Mod");
        }
        return meta.toString();
    }

    private void handleFocusAction(String channelName) {
        Channel channel = channelManager.findChannel(channelName);
        UUID viewerId = playerRef.getUuid();
        if (channel == null) {
            statusMessage = "Channel not found.";
            return;
        }
        if (plugin.getConfig().isEnforceChannelPermissions() && !hasPermission(viewerId, channel.getReadPermission())) {
            statusMessage = "No read permission for " + channel.getName() + ".";
            return;
        }
        if (!channel.isMember(viewerId)) {
            if (!attemptJoin(viewerId, channel, null)) {
                return;
            }
        }

        playerDataManager.setFocusedChannel(viewerId, channel.getName());
        statusMessage = "Now chatting in " + channel.getName() + ".";
    }

    private void handleJoinAction(String channelName, String password) {
        Channel channel = channelManager.findChannel(channelName);
        UUID viewerId = playerRef.getUuid();
        if (channel == null) {
            statusMessage = "Channel not found.";
            return;
        }
        if (channel.isMember(viewerId)) {
            statusMessage = "Already in " + channel.getName() + ".";
            return;
        }

        if (attemptJoin(viewerId, channel, password)) {
            playerDataManager.setFocusedChannel(viewerId, channel.getName());
            statusMessage = "Joined and focused " + channel.getName() + ".";
        }
    }

    private boolean attemptJoin(UUID viewerId, Channel channel, String password) {
        if (channel.isBanned(viewerId)) {
            statusMessage = "You are banned from " + channel.getName() + ".";
            return false;
        }
        if (plugin.getConfig().isEnforceChannelPermissions()) {
            if (!hasPermission(viewerId, channel.getJoinPermission())) {
                statusMessage = "No join permission for " + channel.getName() + ".";
                return false;
            }
            if (!hasPermission(viewerId, channel.getReadPermission())) {
                statusMessage = "No read permission for " + channel.getName() + ".";
                return false;
            }
        }
        if (channel.hasPassword() && !channel.checkPassword(password)) {
            statusMessage = "Wrong password for " + channel.getName() + ".";
            return false;
        }

        channel.addMember(viewerId);
        return true;
    }

    private void handleLeaveAction(String channelName) {
        Channel channel = channelManager.findChannel(channelName);
        UUID viewerId = playerRef.getUuid();
        if (channel == null) {
            statusMessage = "Channel not found.";
            return;
        }
        if (channel.isDefault()) {
            statusMessage = "Default channel cannot be left.";
            return;
        }
        if (!channel.isMember(viewerId)) {
            statusMessage = "You are not in " + channel.getName() + ".";
            return;
        }

        channel.removeMember(viewerId);

        String focused = playerDataManager.getFocusedChannel(viewerId);
        if (focused != null && focused.equalsIgnoreCase(channel.getName())) {
            Channel fallback = channelManager.getDefaultChannel();
            if (fallback != null && !fallback.isBanned(viewerId)) {
                fallback.addMember(viewerId);
                playerDataManager.setFocusedChannel(viewerId, fallback.getName());
                statusMessage = "Left " + channel.getName() + ". Switched to " + fallback.getName() + ".";
            } else {
                statusMessage = "Left " + channel.getName() + ".";
            }
        } else {
            statusMessage = "Left " + channel.getName() + ".";
        }
    }

    private void handleProfileAction(String action, String value, String extra) {
        UUID viewerId = playerRef.getUuid();
        if (action == null || action.isBlank()) {
            statusMessage = "Unknown profile action.";
            return;
        }

        switch (action) {
            case "set_focus" -> {
                if (!hasPermission(viewerId, "werchat.switch")) {
                    statusMessage = "You don't have permission to switch channels.";
                    return;
                }
                if (value == null || value.isBlank()) {
                    statusMessage = "Select a channel first.";
                    return;
                }
                Channel channel = channelManager.findChannel(value);
                if (channel == null || !channel.isMember(viewerId)) {
                    statusMessage = "You are not in that channel.";
                    return;
                }
                handleFocusAction(channel.getName());
            }
            case "quick_join" -> {
                if (value == null || value.isBlank()) {
                    statusMessage = "Select a channel first.";
                    return;
                }
                Channel channel = channelManager.findChannel(value);
                if (channel == null) {
                    statusMessage = "Channel not found.";
                    return;
                }
                if (channel.isMember(viewerId)) {
                    statusMessage = "Already in " + channel.getName() + ".";
                    return;
                }
                if (channel.hasPassword()) {
                    statusMessage = "Use /ch join " + channel.getName() + " <password> for password channels.";
                    return;
                }
                handleJoinAction(channel.getName(), null);
            }
            case "quick_leave" -> {
                if (value == null || value.isBlank()) {
                    statusMessage = "Select a channel first.";
                    return;
                }
                Channel channel = channelManager.findChannel(value);
                if (channel == null) {
                    statusMessage = "Channel not found.";
                    return;
                }
                handleLeaveAction(channel.getName());
            }
            case "set_nickname" -> {
                if (!hasPermission(viewerId, "werchat.playernick")) {
                    statusMessage = "You don't have permission to set nicknames.";
                    return;
                }
                String nickname = value == null ? "" : value.trim();
                if (nickname.isEmpty()) {
                    statusMessage = "Nickname cannot be empty.";
                    return;
                }
                if (nickname.length() > MAX_NICKNAME_LENGTH) {
                    statusMessage = "Nickname too long (max " + MAX_NICKNAME_LENGTH + ").";
                    return;
                }
                for (PlayerRef online : playerDataManager.getOnlinePlayers()) {
                    if (online.getUuid().equals(viewerId)) {
                        continue;
                    }
                    if (online.getUsername().equalsIgnoreCase(nickname)) {
                        statusMessage = "You cannot use another player's username.";
                        return;
                    }
                }
                playerDataManager.setNickname(viewerId, nickname);
                statusMessage = "Nickname updated.";
            }
            case "clear_nickname" -> {
                if (!hasPermission(viewerId, "werchat.playernick")) {
                    statusMessage = "You don't have permission to clear nicknames.";
                    return;
                }
                playerDataManager.clearNickname(viewerId);
                statusMessage = "Nickname and nickname colors cleared.";
            }
            case "set_nick_solid" -> {
                if (!hasPermission(viewerId, "werchat.nickcolor")) {
                    statusMessage = "You don't have permission to set nickname colors.";
                    return;
                }
                String start = normalizeHexColor(value);
                if (start == null) {
                    statusMessage = "Invalid nickname color.";
                    return;
                }
                playerDataManager.setNickColor(viewerId, start);
                playerDataManager.setNickGradientEnd(viewerId, null);
                statusMessage = "Nickname color updated.";
            }
            case "set_nick_gradient" -> {
                if (!hasPermission(viewerId, "werchat.nickcolor")) {
                    statusMessage = "You don't have permission to set nickname colors.";
                    return;
                }
                String start = normalizeHexColor(value);
                String end = normalizeHexColor(extra);
                if (start == null || end == null) {
                    statusMessage = "Invalid nickname gradient colors.";
                    return;
                }
                playerDataManager.setNickColor(viewerId, start);
                playerDataManager.setNickGradientEnd(viewerId, end);
                statusMessage = "Nickname gradient updated.";
            }
            case "clear_nick_color" -> {
                if (!hasPermission(viewerId, "werchat.nickcolor")) {
                    statusMessage = "You don't have permission to clear nickname colors.";
                    return;
                }
                playerDataManager.setNickColor(viewerId, null);
                playerDataManager.setNickGradientEnd(viewerId, null);
                statusMessage = "Nickname colors cleared.";
            }
            case "set_msg_solid" -> {
                if (!hasPermission(viewerId, "werchat.msgcolor")) {
                    statusMessage = "You don't have permission to set message colors.";
                    return;
                }
                String start = normalizeHexColor(value);
                if (start == null) {
                    statusMessage = "Invalid message color.";
                    return;
                }
                playerDataManager.setMsgColor(viewerId, start);
                playerDataManager.setMsgGradientEnd(viewerId, null);
                statusMessage = "Message color updated.";
            }
            case "set_msg_gradient" -> {
                if (!hasPermission(viewerId, "werchat.msgcolor")) {
                    statusMessage = "You don't have permission to set message colors.";
                    return;
                }
                String start = normalizeHexColor(value);
                String end = normalizeHexColor(extra);
                if (start == null || end == null) {
                    statusMessage = "Invalid message gradient colors.";
                    return;
                }
                playerDataManager.setMsgColor(viewerId, start);
                playerDataManager.setMsgGradientEnd(viewerId, end);
                statusMessage = "Message gradient updated.";
            }
            case "clear_msg_color" -> {
                if (!hasPermission(viewerId, "werchat.msgcolor")) {
                    statusMessage = "You don't have permission to clear message colors.";
                    return;
                }
                playerDataManager.clearMsgColor(viewerId);
                statusMessage = "Message colors cleared.";
            }
            default -> statusMessage = "Unsupported profile action.";
        }
    }

    private void handleModeratorAction(String action, String targetName, String value, String password) {
        UUID viewerId = playerRef.getUuid();
        Channel channel = getActiveModeratorChannel(viewerId);
        if (channel == null) {
            statusMessage = "No moderator channel selected.";
            return;
        }
        if (!isChannelModerator(viewerId, channel)) {
            statusMessage = "You are not a moderator of " + channel.getName() + ".";
            return;
        }

        if (action == null || action.isBlank()) {
            statusMessage = "Unknown moderator action.";
            return;
        }

        switch (action) {
            case "ban" -> {
                UUID target = resolveTargetUuid(targetName, channel);
                if (target == null) {
                    statusMessage = "Target not found.";
                    return;
                }
                channel.ban(target);
                statusMessage = "Banned " + playerDataManager.getKnownName(target) + " from " + channel.getName() + ".";
            }
            case "unban" -> {
                UUID target = resolveTargetUuid(targetName, channel);
                if (target == null) {
                    statusMessage = "Target not found.";
                    return;
                }
                channel.unban(target);
                statusMessage = "Unbanned " + playerDataManager.getKnownName(target) + " in " + channel.getName() + ".";
            }
            case "mute" -> {
                UUID target = resolveTargetUuid(targetName, channel);
                if (target == null) {
                    statusMessage = "Target not found.";
                    return;
                }
                channel.mute(target);
                statusMessage = "Muted " + playerDataManager.getKnownName(target) + " in " + channel.getName() + ".";
            }
            case "unmute" -> {
                UUID target = resolveTargetUuid(targetName, channel);
                if (target == null) {
                    statusMessage = "Target not found.";
                    return;
                }
                channel.unmute(target);
                statusMessage = "Unmuted " + playerDataManager.getKnownName(target) + " in " + channel.getName() + ".";
            }
            case "addmod" -> {
                UUID target = resolveTargetUuid(targetName, channel);
                if (target == null) {
                    statusMessage = "Target not found.";
                    return;
                }
                channel.addModerator(target);
                statusMessage = "Added moderator " + playerDataManager.getKnownName(target) + " in " + channel.getName() + ".";
            }
            case "removemod" -> {
                UUID target = resolveTargetUuid(targetName, channel);
                if (target == null) {
                    statusMessage = "Target not found.";
                    return;
                }
                if (channel.getOwner() != null && channel.getOwner().equals(target)) {
                    statusMessage = "Channel owner cannot be removed as moderator.";
                    return;
                }
                channel.removeModerator(target);
                statusMessage = "Removed moderator " + playerDataManager.getKnownName(target) + " in " + channel.getName() + ".";
            }
            case "set_nick" -> {
                if (value == null || value.isBlank()) {
                    statusMessage = "Channel nick cannot be empty.";
                    return;
                }
                channel.setNick(value.trim());
                statusMessage = "Channel nick updated.";
            }
            case "set_distance" -> {
                if (value == null || value.isBlank()) {
                    statusMessage = "Distance is required.";
                    return;
                }
                if ("custom".equalsIgnoreCase(value.trim())) {
                    statusMessage = "Select a preset distance or use custom distance input.";
                    return;
                }
                try {
                    int parsed = Integer.parseInt(value.trim());
                    if (parsed < 0) {
                        statusMessage = "Distance must be 0 or greater.";
                        return;
                    }
                    channel.setDistance(parsed);
                    statusMessage = parsed == 0 ? "Distance set to global." : "Distance set to " + parsed + " blocks.";
                } catch (NumberFormatException e) {
                    statusMessage = "Invalid distance.";
                }
            }
            case "set_password" -> {
                if (password == null || password.isBlank()) {
                    statusMessage = "Password is empty.";
                    return;
                }
                channel.setPassword(password);
                statusMessage = "Password updated.";
            }
            case "clear_password" -> {
                channel.setPassword(null);
                statusMessage = "Password cleared.";
            }
            case "set_tag_color" -> {
                Color color = parseHexColor(value);
                if (color == null) {
                    statusMessage = "Invalid tag color. Use #RRGGBB.";
                    return;
                }
                channel.setColor(color);
                statusMessage = "Tag color updated.";
            }
            case "set_text_color" -> {
                if (value == null || value.isBlank()) {
                    channel.setMessageColor(null);
                    statusMessage = "Text color reset to default.";
                    return;
                }
                Color color = parseHexColor(value);
                if (color == null) {
                    statusMessage = "Invalid text color. Use #RRGGBB.";
                    return;
                }
                channel.setMessageColor(color);
                statusMessage = "Text color updated.";
            }
            case "clear_text_color" -> {
                channel.setMessageColor(null);
                statusMessage = "Text color reset to default.";
            }
            default -> statusMessage = "Unsupported moderator action.";
        }
    }

    private Channel getActiveModeratorChannel(UUID viewerId) {
        if (!activeTab.startsWith(TAB_MOD_PREFIX)) {
            return null;
        }
        String channelName = activeTab.substring(TAB_MOD_PREFIX.length());
        Channel channel = channelManager.findChannel(channelName);
        if (channel == null || !isChannelModerator(viewerId, channel)) {
            return null;
        }
        return channel;
    }

    private void validateActiveTab(List<Channel> moderatorChannels) {
        if (!activeTab.startsWith(TAB_MOD_PREFIX)) {
            return;
        }
        String channelName = activeTab.substring(TAB_MOD_PREFIX.length());
        for (Channel channel : moderatorChannels) {
            if (channel.getName().equalsIgnoreCase(channelName)) {
                return;
            }
        }
        activeTab = TAB_CHANNELS;
    }

    private List<Channel> getModeratorChannels(UUID viewerId) {
        List<Channel> channels = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Channel channel : channelManager.getAllChannels()) {
            String key = channel.getName().toLowerCase(Locale.ROOT);
            if (isChannelModerator(viewerId, channel) && seen.add(key)) {
                channels.add(channel);
            }
        }
        channels.sort(Comparator.comparing(channel -> channel.getName().toLowerCase(Locale.ROOT)));
        return channels;
    }

    private boolean isChannelModerator(UUID viewerId, Channel channel) {
        return channel.isModerator(viewerId)
            || (channel.getOwner() != null && channel.getOwner().equals(viewerId))
            || hasPermission(viewerId, "werchat.mod")
            || hasPermission(viewerId, "werchat.*")
            || hasPermission(viewerId, "*");
    }

    private boolean hasPermission(UUID playerId, String permission) {
        PermissionsModule perms = PermissionsModule.get();
        return perms.hasPermission(playerId, permission)
            || perms.hasPermission(playerId, "werchat.*")
            || perms.hasPermission(playerId, "*");
    }

    private UUID resolveTargetUuid(String name, Channel channel) {
        if (name == null || name.isBlank()) {
            return null;
        }

        PlayerRef online = playerDataManager.findPlayerByName(name.trim());
        if (online != null) {
            return online.getUuid();
        }

        String normalized = name.trim().toLowerCase(Locale.ROOT);
        Set<UUID> candidates = new HashSet<>();
        candidates.addAll(channel.getMembers());
        candidates.addAll(channel.getModerators());
        candidates.addAll(channel.getBanned());
        candidates.addAll(channel.getMuted());
        if (channel.getOwner() != null) {
            candidates.add(channel.getOwner());
        }

        for (UUID candidate : candidates) {
            String known = playerDataManager.getKnownName(candidate);
            if (known != null && known.toLowerCase(Locale.ROOT).equals(normalized)) {
                return candidate;
            }
        }
        return null;
    }

    private Color parseHexColor(String value) {
        if (value == null) {
            return null;
        }
        String hex = value.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.matches("(?i)^[0-9a-f]{8}$")) {
            // ColorPicker returns 8-digit RGBA (RRGGBBAA). Ignore trailing alpha.
            hex = hex.substring(0, 6);
        }
        if (!hex.matches("(?i)^[0-9a-f]{6}$")) {
            return null;
        }
        try {
            return new Color(
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16)
            );
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeHexColor(String value) {
        Color parsed = parseHexColor(value);
        if (parsed == null) {
            return null;
        }
        return String.format("#%02X%02X%02X", parsed.getRed(), parsed.getGreen(), parsed.getBlue());
    }

    private String toDistancePresetValue(int distance) {
        if (DISTANCE_PRESETS.contains(distance)) {
            return String.valueOf(distance);
        }
        return "custom";
    }

    public static class PageEventData {
        public static final BuilderCodec<PageEventData> CODEC = BuilderCodec.builder(PageEventData.class, PageEventData::new)
            .addField(new KeyedCodec<>("Button", Codec.STRING), (data, value) -> data.button = value, data -> data.button)
            .addField(new KeyedCodec<>("Tab", Codec.STRING), (data, value) -> data.tab = value, data -> data.tab)
            .addField(new KeyedCodec<>("Channel", Codec.STRING), (data, value) -> data.channel = value, data -> data.channel)
            .addField(new KeyedCodec<>("Action", Codec.STRING), (data, value) -> data.action = value, data -> data.action)
            .addField(new KeyedCodec<>("@Target", Codec.STRING), (data, value) -> data.target = value, data -> data.target)
            .addField(new KeyedCodec<>("@Value", Codec.STRING), (data, value) -> data.value = value, data -> data.value)
            .addField(new KeyedCodec<>("@Extra", Codec.STRING), (data, value) -> data.extra = value, data -> data.extra)
            .addField(new KeyedCodec<>("@Password", Codec.STRING), (data, value) -> data.password = value, data -> data.password)
            .addField(new KeyedCodec<>("Target", Codec.STRING), (data, value) -> data.target = value, data -> data.target)
            .addField(new KeyedCodec<>("Value", Codec.STRING), (data, value) -> data.value = value, data -> data.value)
            .addField(new KeyedCodec<>("Extra", Codec.STRING), (data, value) -> data.extra = value, data -> data.extra)
            .addField(new KeyedCodec<>("Password", Codec.STRING), (data, value) -> data.password = value, data -> data.password)
            .build();

        public String button;
        public String tab;
        public String channel;
        public String action;
        public String target;
        public String value;
        public String extra;
        public String password;
    }
}
