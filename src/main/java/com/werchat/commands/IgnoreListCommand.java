package com.werchat.commands;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.werchat.WerchatPlugin;
import com.werchat.storage.PlayerDataManager;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

/**
 * /ignorelist command - show ignored players
 */
public class IgnoreListCommand extends CommandBase {

    private final PlayerDataManager playerDataManager;

    public IgnoreListCommand(WerchatPlugin plugin) {
        super("ignorelist", "List ignored players", false);
        this.playerDataManager = plugin.getPlayerDataManager();

        this.setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            return;
        }

        UUID playerId = ctx.sender().getUuid();
        PermissionsModule perms = PermissionsModule.get();
        if (!perms.hasPermission(playerId, "werchat.ignore")
                && !perms.hasPermission(playerId, "werchat.*")
                && !perms.hasPermission(playerId, "*")) {
            ctx.sendMessage(Message.raw("You don't have permission to view ignored players").color("#FF5555"));
            return;
        }
        Set<UUID> ignoredPlayers = playerDataManager.getIgnoredPlayers(playerId);

        if (ignoredPlayers.isEmpty()) {
            ctx.sendMessage(Message.raw("You are not ignoring anyone.").color("#AAAAAA"));
            ctx.sendMessage(Message.raw("Use /ignore <player> to ignore someone.").color("#AAAAAA"));
            return;
        }

        ctx.sendMessage(Message.raw("=== Ignored Players ===").color("#FFAA00"));

        for (UUID ignoredId : ignoredPlayers) {
            PlayerRef ignored = playerDataManager.getOnlinePlayer(ignoredId);
            String name;
            String status;

            if (ignored != null) {
                name = ignored.getUsername();
                status = " (online)";
            } else {
                // Offline - show partial UUID
                name = ignoredId.toString().substring(0, 8) + "...";
                status = " (offline)";
            }

            ctx.sendMessage(Message.join(
                Message.raw("  - ").color("#AAAAAA"),
                Message.raw(name).color("#FFFFFF"),
                Message.raw(status).color("#555555")
            ));
        }

        ctx.sendMessage(Message.raw("Use /ignore <player> to unignore.").color("#AAAAAA"));
    }
}
