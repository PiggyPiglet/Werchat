package com.werchat.config;

import com.google.gson.*;
import com.werchat.WerchatPlugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;

public class WerchatConfig {

    private final WerchatPlugin plugin;
    private final Path configFile;

    // General settings
    private String defaultChannelName = "Global";
    private boolean autoJoinDefault = true;
    private boolean showJoinLeaveMessages = true;
    private boolean allowPrivateMessages = true;
    private boolean enforceChannelPermissions = false;
    private boolean channelDescriptionsEnabled = true;
    private boolean channelMotdSystemEnabled = true;

    // Moderation messages
    private String banMessage = "You have been banned from {channel}";
    private String muteMessage = "You have been muted in {channel}";

    // Word Filter
    private boolean wordFilterEnabled = false;
    private Set<String> filteredWords = new HashSet<>();
    private String filterMode = "censor"; // "censor" = replace with ***, "block" = block entire message
    private String filterReplacement = "***";
    private boolean filterNotifyPlayer = true;
    private String filterWarningMessage = "Your message contained inappropriate language.";

    // Chat Cooldown (disabled by default)
    private boolean cooldownEnabled = false;
    private int cooldownSeconds = 3;
    private String cooldownMessage = "Please wait {seconds}s before sending another message.";
    private String cooldownBypassPermission = "werchat.cooldown.bypass";

    // Mention Alerts
    private boolean mentionsEnabled = true;
    private String mentionColor = "#FFFF55"; // Yellow

    // URL handling
    private boolean clickableUrlsEnabled = true;

    // Chat Cancellation
    private boolean ignoreChatCancellations = false;

    public WerchatConfig(WerchatPlugin plugin) {
        this.plugin = plugin;
        this.configFile = plugin.getDataDirectory().resolve("config.json");
        initDefaults();
    }

    private void initDefaults() {
        // Default filtered words
        filteredWords.addAll(Arrays.asList(
            "fuck", "shit", "bitch", "cunt", "dick", "pussy",
            "nigger", "nigga", "faggot", "retard", "whore", "slut"
        ));
    }

    public void load() {
        try {
            if (Files.exists(configFile)) {
                String json = Files.readString(configFile);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();

                // General
                if (root.has("defaultChannelName")) defaultChannelName = root.get("defaultChannelName").getAsString();
                if (root.has("autoJoinDefault")) autoJoinDefault = root.get("autoJoinDefault").getAsBoolean();
                if (root.has("showJoinLeaveMessages")) showJoinLeaveMessages = root.get("showJoinLeaveMessages").getAsBoolean();
                if (root.has("allowPrivateMessages")) allowPrivateMessages = root.get("allowPrivateMessages").getAsBoolean();
                if (root.has("channelPermissions")) {
                    JsonObject cp = root.getAsJsonObject("channelPermissions");
                    if (cp.has("enforce")) {
                        enforceChannelPermissions = cp.get("enforce").getAsBoolean();
                    }
                }
                if (root.has("channelDescriptions")) {
                    JsonObject cd = root.getAsJsonObject("channelDescriptions");
                    if (cd.has("enabled")) {
                        channelDescriptionsEnabled = cd.get("enabled").getAsBoolean();
                    }
                }
                if (root.has("channelMotd")) {
                    JsonObject cm = root.getAsJsonObject("channelMotd");
                    if (cm.has("enabled")) {
                        channelMotdSystemEnabled = cm.get("enabled").getAsBoolean();
                    }
                }

                // Moderation messages
                if (root.has("banMessage")) banMessage = root.get("banMessage").getAsString();
                if (root.has("muteMessage")) muteMessage = root.get("muteMessage").getAsString();

                // Word Filter
                if (root.has("wordFilter")) {
                    JsonObject wf = root.getAsJsonObject("wordFilter");
                    if (wf.has("enabled")) wordFilterEnabled = wf.get("enabled").getAsBoolean();
                    if (wf.has("mode")) filterMode = wf.get("mode").getAsString();
                    if (wf.has("replacement")) filterReplacement = wf.get("replacement").getAsString();
                    if (wf.has("notifyPlayer")) filterNotifyPlayer = wf.get("notifyPlayer").getAsBoolean();
                    if (wf.has("warningMessage")) filterWarningMessage = wf.get("warningMessage").getAsString();
                    if (wf.has("words")) {
                        filteredWords.clear();
                        for (JsonElement el : wf.getAsJsonArray("words")) {
                            filteredWords.add(el.getAsString().toLowerCase());
                        }
                    }
                }

                // Chat Cooldown
                if (root.has("cooldown")) {
                    JsonObject cd = root.getAsJsonObject("cooldown");
                    if (cd.has("enabled")) cooldownEnabled = cd.get("enabled").getAsBoolean();
                    if (cd.has("seconds")) cooldownSeconds = cd.get("seconds").getAsInt();
                    if (cd.has("message")) cooldownMessage = cd.get("message").getAsString();
                    if (cd.has("bypassPermission")) cooldownBypassPermission = cd.get("bypassPermission").getAsString();
                }

                // Mentions
                if (root.has("mentions")) {
                    JsonObject m = root.getAsJsonObject("mentions");
                    if (m.has("enabled")) mentionsEnabled = m.get("enabled").getAsBoolean();
                    if (m.has("color")) mentionColor = m.get("color").getAsString();
                }

                // URL handling
                if (root.has("clickableUrls")) {
                    JsonObject clickableUrls = root.getAsJsonObject("clickableUrls");
                    if (clickableUrls.has("enabled")) clickableUrlsEnabled = clickableUrls.get("enabled").getAsBoolean();
                }

                // Chat Cancellation
                if (root.has("ignoreChatCancellations")) ignoreChatCancellations = root.get("ignoreChatCancellations").getAsBoolean();

                plugin.getLogger().at(Level.INFO).log("Configuration loaded from config.json");
                save(); // Re-save to add any new config fields from updates
            } else {
                save(); // Create default config
                plugin.getLogger().at(Level.INFO).log("Created default config.json");
            }
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).log("Failed to load config: %s", e.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(configFile.getParent());

            JsonObject root = new JsonObject();

            // General
            root.addProperty("defaultChannelName", defaultChannelName);
            root.addProperty("autoJoinDefault", autoJoinDefault);
            root.addProperty("showJoinLeaveMessages", showJoinLeaveMessages);
            root.addProperty("allowPrivateMessages", allowPrivateMessages);
            JsonObject channelPermissions = new JsonObject();
            channelPermissions.addProperty("enforce", enforceChannelPermissions);
            root.add("channelPermissions", channelPermissions);
            JsonObject channelDescriptions = new JsonObject();
            channelDescriptions.addProperty("enabled", channelDescriptionsEnabled);
            root.add("channelDescriptions", channelDescriptions);
            JsonObject channelMotd = new JsonObject();
            channelMotd.addProperty("enabled", channelMotdSystemEnabled);
            root.add("channelMotd", channelMotd);

            // Moderation messages
            root.addProperty("banMessage", banMessage);
            root.addProperty("muteMessage", muteMessage);

            // Word Filter
            JsonObject wf = new JsonObject();
            wf.addProperty("enabled", wordFilterEnabled);
            wf.addProperty("mode", filterMode);
            wf.addProperty("replacement", filterReplacement);
            wf.addProperty("notifyPlayer", filterNotifyPlayer);
            wf.addProperty("warningMessage", filterWarningMessage);
            JsonArray wordsArr = new JsonArray();
            for (String word : filteredWords) wordsArr.add(word);
            wf.add("words", wordsArr);
            root.add("wordFilter", wf);

            // Chat Cooldown
            JsonObject cd = new JsonObject();
            cd.addProperty("enabled", cooldownEnabled);
            cd.addProperty("seconds", cooldownSeconds);
            cd.addProperty("message", cooldownMessage);
            cd.addProperty("bypassPermission", cooldownBypassPermission);
            root.add("cooldown", cd);

            // Mentions
            JsonObject m = new JsonObject();
            m.addProperty("enabled", mentionsEnabled);
            m.addProperty("color", mentionColor);
            root.add("mentions", m);

            // URL handling
            JsonObject clickableUrls = new JsonObject();
            clickableUrls.addProperty("enabled", clickableUrlsEnabled);
            root.add("clickableUrls", clickableUrls);

            // Chat Cancellation
            root.addProperty("ignoreChatCancellations", ignoreChatCancellations);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(configFile, gson.toJson(root));

        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).log("Failed to save config: %s", e.getMessage());
        }
    }

    // === Getters ===

    public String getDefaultChannelName() { return defaultChannelName; }
    public boolean isAutoJoinDefault() { return autoJoinDefault; }
    public boolean isShowJoinLeaveMessages() { return showJoinLeaveMessages; }
    public boolean isAllowPrivateMessages() { return allowPrivateMessages; }
    public boolean isEnforceChannelPermissions() { return enforceChannelPermissions; }
    public boolean isChannelDescriptionsEnabled() { return channelDescriptionsEnabled; }
    public boolean isChannelMotdSystemEnabled() { return channelMotdSystemEnabled; }

    public String getBanMessage() { return banMessage; }
    public String getMuteMessage() { return muteMessage; }

    // Word Filter
    public boolean isWordFilterEnabled() { return wordFilterEnabled; }
    public Set<String> getFilteredWords() { return filteredWords; }
    public String getFilterMode() { return filterMode; } // "censor" or "block"
    public String getFilterReplacement() { return filterReplacement; }
    public boolean isFilterNotifyPlayer() { return filterNotifyPlayer; }
    public String getFilterWarningMessage() { return filterWarningMessage; }

    // Cooldown
    public boolean isCooldownEnabled() { return cooldownEnabled; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public String getCooldownMessage() { return cooldownMessage; }
    public String getCooldownBypassPermission() { return cooldownBypassPermission; }

    // Mentions
    public boolean isMentionsEnabled() { return mentionsEnabled; }
    public String getMentionColor() { return mentionColor; }

    // URL handling
    public boolean isClickableUrlsEnabled() { return clickableUrlsEnabled; }

    // Chat Cancellation
    public boolean isIgnoreChatCancellations() { return ignoreChatCancellations; }

}
