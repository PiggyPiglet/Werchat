# Werchat - Channel Chat System for Hytale (v1.1.9)

A fully featured chat channel plugin for Hytale servers. Organize communication with customizable channels, quick-chat routing, private messaging, moderation tools, nickname/message styling, and PlaceholderAPI support.

Discord support: https://discord.gg/SNPjyfkYPc

## Updated Info (vs older listing)

- Version references updated to **1.1.9**.
- Added **PlaceholderAPI integration** details (including built-in `%werchat_*%` expansion).
- Added **Plugin API** notes for integrations.
- Added new config keys like `allowPrivateMessages`, `defaultChannelName`, and `ignoreChatCancellations`.
- Added cooldown bypass permission `werchat.cooldown.bypass`.
- Clarified current channel format placeholder behavior (`{nick}`, `{sender}`, `{msg}`, etc.).

## Features

- Multiple Chat Channels: Create and manage unlimited channels.
- Multi-World Support: Restrict channels to one or more worlds.
- Channel Customization: Per-channel colors, nicknames, password, distance, quick-chat symbol.
- Quick Chat Routing: Send to channels using symbol prefixes (example: `!hello`, `~hello`).
- Private Messaging: `/msg` and `/r` with ignore support.
- Player Nicknames & Message Colors: Per-player nick/message color with optional gradients.
- Mentions: `@username` highlighting.
- Moderation: Per-channel ban, mute, and moderator tools.
- Word Filter + Cooldown: Optional moderation safeguards.
- PlaceholderAPI Integration: External placeholders in chat formats + built-in Werchat placeholders.
- Plugin API: Other plugins can query channels, manage membership/focus, and submit chat through Werchat.
- Persistent Storage: JSON-backed channel/member/player data.

## New in v1.1.9

- PlaceholderAPI integration for format literals and built-in Werchat expansion.
- Channel format pipeline now supports configurable token placement using channel `format`.
- Public plugin API expanded with typed results and integration hooks.
- API channel targeting now supports explicit `EXACT` and `FUZZY` lookup behavior.
- Private messaging can be globally disabled via config (`allowPrivateMessages`).

## Default Channels

| Channel | Nick | Color | Range | Auto-Join | Quick Chat |
|---|---|---|---|---|---|
| Global | Global | `#ffffff` | Unlimited | Yes | `!` |
| Local | Local | `#808080` | 100 blocks | Yes | (none) |
| Trade | Trade | `#ffd700` | Unlimited | Yes | `~` |
| Support | Support | `#00ff00` | Unlimited | Yes | (none) |

## Commands

### Player Commands

| Command | Aliases | Description |
|---|---|---|
| `/ch list` | `/ch l` | List all channels |
| `/ch join <channel> [password]` | `/ch j` | Join a channel |
| `/ch leave <channel>` | - | Leave a channel |
| `/ch <channel>` | - | Switch active channel |
| `/ch who <channel>` | `/ch w` | View channel members |
| `/ch info <channel>` | - | View channel details |
| `/ch playernick <name> [#color] [#gradient]` | `/ch pnick`, `/ch nickname` | Set your nickname |
| `/ch msgcolor <#color> [#gradient]` | `/ch chatcolor` | Set your message color |
| `/ch help` | `/ch ?` | Show command help |
| `/msg <player> <message>` | `/whisper`, `/w`, `/tell`, `/pm` | Private message |
| `/r <message>` | `/reply` | Reply to last PM |
| `/ignore <player>` | - | Toggle ignore |
| `/ignorelist` | - | Show ignored players |

### Admin/Moderator Commands

| Command | Permission |
|---|---|
| `/ch create <name> [nick]` | `werchat.create` |
| `/ch remove <channel>` | `werchat.remove` |
| `/ch color <channel> <#tag> [#text]` | `werchat.color` |
| `/ch nick <channel> <nick>` | `werchat.nick` |
| `/ch password <channel> [password]` | `werchat.password` |
| `/ch rename <channel> <newname>` | `werchat.rename` |
| `/ch mod <channel> <player>` | `werchat.mod` |
| `/ch unmod <channel> <player>` | `werchat.mod` |
| `/ch distance <channel> <blocks>` | `werchat.distance` |
| `/ch world <channel> add\|remove <world>` | `werchat.world` |
| `/ch ban <channel> <player>` | `werchat.ban` |
| `/ch unban <channel> <player>` | `werchat.ban` |
| `/ch mute <channel> <player>` | `werchat.mute` |
| `/ch unmute <channel> <player>` | `werchat.mute` |
| `/ch playernick <player> <name> [#color] [#gradient]` | `werchat.playernick.others` |
| `/ch msgcolor <player> <#color> [#gradient]` | `werchat.msgcolor.others` |

Note: Channel moderators can use management commands on their own channels without global admin nodes.

## Permissions

Wildcard:

- `*` - all permissions
- `werchat.*` - all Werchat permissions

Player permissions:

- `werchat.list`
- `werchat.join`
- `werchat.leave`
- `werchat.switch`
- `werchat.who`
- `werchat.info`
- `werchat.msg`
- `werchat.ignore`
- `werchat.playernick`
- `werchat.nickcolor`
- `werchat.msgcolor`
- `werchat.quickchat`
- `werchat.cooldown.bypass`

Management permissions:

- `werchat.create`
- `werchat.remove`
- `werchat.color`
- `werchat.nick`
- `werchat.password`
- `werchat.rename`
- `werchat.mod`
- `werchat.distance`
- `werchat.world`
- `werchat.ban`
- `werchat.mute`
- `werchat.playernick.others`
- `werchat.msgcolor.others`

## Configuration (`config.json`)

```json
{
  "defaultChannelName": "Global",
  "autoJoinDefault": true,
  "showJoinLeaveMessages": true,
  "allowPrivateMessages": true,
  "banMessage": "You have been banned from {channel}",
  "muteMessage": "You have been muted in {channel}",
  "wordFilter": {
    "enabled": false,
    "mode": "censor",
    "replacement": "***",
    "notifyPlayer": true,
    "warningMessage": "Your message contained inappropriate language.",
    "words": ["..."]
  },
  "cooldown": {
    "enabled": false,
    "seconds": 3,
    "message": "Please wait {seconds}s before sending another message.",
    "bypassPermission": "werchat.cooldown.bypass"
  },
  "mentions": {
    "enabled": true,
    "color": "#FFFF55"
  },
  "ignoreChatCancellations": false
}
```

## Channel Format Placeholders

Channel `format` supports:

- `{name}` - full channel name
- `{nick}` - channel nickname/abbreviation
- `{color}` - channel color hex
- `{sender}` - sender display name
- `{msg}` - chat message
- `{prefix}` - permission prefix
- `{suffix}` - permission suffix

Default format:

```text
{nick} {sender}: {msg}
```

If PlaceholderAPI is installed, format literals can also contain `%...%` placeholders.

## PlaceholderAPI (Built-in Expansion)

Werchat registers a built-in expansion with identifier `werchat`.

Examples:

- `%werchat_channel%` - focused channel name
- `%werchat_channels_total%` - total channel count
- `%werchat_display_name%` - player display name
- `%werchat_channel_active_name%` - active/focused channel name
- `%werchat_channel_trade_member_count%` - member count of `Trade`

## Data Files

Werchat stores data in:

`mods/com.werchat_Werchat/`

- `config.json` - global settings
- `channels.json` - channel settings
- `channel-members.json` - members/mods/bans/mutes
- `nicknames.json` - nickname and message color data

## Installation

1. Download `Werchat-1.1.9.jar`.
2. Place it in your server `Mods` folder.
3. Start/restart the server.
4. Configure permissions and `config.json` as needed.

## Developer Notes

Werchat exposes `WerchatAPI` for integrations:

- Query channels and focused channel
- Join/leave/focus channels
- Submit player chat through Werchat pipeline
- Register integration hooks (pre/post action)

## License

GPLv3 (see repository `LICENSE`).

## Support

- Discord: https://discord.gg/SNPjyfkYPc
- Issues: https://github.com/HyperSystemsDev/Werchat/issues
