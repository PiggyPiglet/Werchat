# Issue #2: PAPI Channel Placeholder Syntax Ambiguity

Status: Open (decision pending)
Scope: `WerchatExpansion` channel selector/key parsing

## Problem Summary

Current keyed syntax uses:

- `%werchat_channel_<selector>%`
- `%werchat_channel_<selector>_<key>%`

Because selector and key are both underscore-delimited, channel names that contain underscores can become ambiguous with key suffixes.

## Concrete Example

If channel name is `trade_name`:

- `%werchat_channel_trade_name%`
  - Could mean selector `trade_name` (alias form), or selector `trade` + key `name`.
- `%werchat_channel_trade_name_worlds%`
  - Intended meaning is usually selector `trade_name` + key `worlds`,
  - but parser may first attempt selector `trade_name` + key `worlds` only if key splitting happens to align.

## Current Parser Behavior

`parseChannelPlaceholderRequest(...)` scans known keys from the end and splits at the trailing `_key` match.

Implication:
- Any selector ending with known key text (`_name`, `_worlds`, etc.) risks accidental reinterpretation.

## Why This Matters

- Makes placeholder behavior non-obvious for server owners.
- Increases support burden (“why does this placeholder resolve wrong/blank?”).
- Makes docs harder to explain cleanly.

## Short-Term Mitigation (Backward Compatible)

Keep current syntax, and add fallback:

1. Parse as keyed (`selector + key`).
2. If channel resolution fails, retry treating whole selector part as alias form (`channel_<selector>` -> key `name`).

Benefit:
- Fixes common failures without breaking existing placeholders.

Limitation:
- Syntax remains conceptually ambiguous.

## Long-Term Syntax Options

### Option A (recommended): explicit key separator

- `%werchat_selected_channel%`
- `%werchat_selected_channel_<key>%`
- `%werchat_channel_<channelName>%`
- `%werchat_channel_<channelName>__<key>%` (`__` reserved as key separator)

Pros:
- Unambiguous with underscore channel names.
- Easy to explain.
- Keeps channel-first mental model.

Cons:
- Introduces a new pattern to learn.

### Option B: hard require focused-channel keyword

- `%werchat_channel_active_<key>%` (or `%werchat_selected_channel_<key>%`)
- `%werchat_channel_<channelName>%` only for name resolution (no keyed form)

Pros:
- Very simple parsing.

Cons:
- Removes direct keyed access for specific named channels unless another syntax is introduced.

### Option C: keep current syntax + document caveat

Pros:
- No migration.

Cons:
- Ongoing ambiguity and support friction.

## Backward Compatibility Strategy (if syntax changes)

- Support old syntax for one release cycle as deprecated.
- Prefer showing only new syntax in README.
- Log warning for old keyed form when detected (optional).

## Decision Needed

Choose final syntax strategy before locking `1.1.9` docs and expansion behavior.
