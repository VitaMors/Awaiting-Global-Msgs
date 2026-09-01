# Awaiting Global Msgs

A [RuneLite](https://runelite.net/) plugin that makes sure you never miss a message from a friend in public ("global") chat.

## What it does

- **Highlight** - any public chat message sent by a player on your friends list is recolored to a lighter shade of blue than RuneLite's normal public-chat text color, as if it had been marked with a highlighter pen. This happens no matter which chat tab you're looking at (All, Public, etc.).
- **Pin** - the same message is also duplicated into a small stack drawn over the top of the chatbox's message area. As long as a pinned message hasn't been dismissed, it stays put at the top instead of scrolling away with the rest of chat, so a handful of unread friend messages sit up top while the newest public chat still scrolls normally underneath. Each pinned row has a circular green tick button on the right edge - click it to dismiss that one message and shrink the stack.

Both features can be toggled independently from the plugin's config panel, and you can pick your own highlight color and cap how many messages can stack up at once (oldest unacknowledged message drops off past that cap).

## How the pin stack works

RuneLite plugins aren't allowed to rewrite the game's own chat history/scrollback, so this doesn't try to freeze the native chatbox. Instead it draws a second, separate overlay panel anchored to the top of the chatbox's message area (`WidgetInfo.CHATBOX_MESSAGE_LINES`) and layered above it (`OverlayLayer.ABOVE_WIDGETS`). That's the panel that "stacks up" and stays until you tick it away; the original message also keeps existing in your normal chat history underneath, just highlighted.

## Config options

| Setting | Default | Description |
|---|---|---|
| Highlight friends' messages | On | Recolors a friend's public chat text/name. |
| Highlight color | Light sky blue (`#87CEFA`) | The recolor shade - pick anything from the color picker. |
| Pin messages until acknowledged | On | Enables the stacking panel + tick button. |
| Max pinned messages | 15 | Stack size cap; oldest unacknowledged entry is dropped past this. |

## Project layout

```
build.gradle
settings.gradle
runelite-plugin.properties
LICENSE
src/main/java/com/xav/friendsglobalchatqol/
    FriendsGlobalChatQolPlugin.java   - event handling, highlighting, pin list
    FriendsGlobalChatQolConfig.java   - config panel definition
    FriendPinOverlay.java             - draws the pinned stack + tick buttons
    FriendPinMouseListener.java       - detects clicks on tick buttons
    PinnedMessage.java                - small data holder for one pinned row
```

This follows the same shape as RuneLite's official [example-plugin](https://github.com/runelite/example-plugin) template, so it can be developed/sideloaded and later submitted to the [plugin-hub](https://github.com/runelite/plugin-hub) the normal way.

## Building & testing it locally

1. Open this folder in IntelliJ IDEA (recommended by RuneLite for plugin development) or run from the command line.
2. From the command line: `./gradlew run --args="--developer-mode --debug"` (or `gradlew.bat run ...` on Windows) will pull down the RuneLite client dependency, compile the plugin, and launch a RuneLite client with it side-loaded so you can test in-game immediately.
3. In IntelliJ: import the Gradle project, then run the `run` task (or create a run configuration pointing at `FriendsGlobalChatQolPlugin`/the test classpath) the same way the example-plugin template describes.

Once you're happy with it, submitting it to the plugin hub is the standard process: fork `runelite/plugin-hub`, add a `plugins/friends-global-chat-qol` file pointing at this repository + commit hash, and open a PR.

**Note on this build:** this project was written and structured here without network access to Maven Central / repo.runelite.net, so it hasn't been compiled against the live `net.runelite:client` jar in this environment - only checked with `javac` against no classpath to catch structural mistakes. The Java code follows the exact same patterns RuneLite's own core plugins use (`ChatNotificationsPlugin` for message recoloring via `MessageNode`, `WidgetOverlay`/the XP tracker overlay for anchoring to a widget's bounds, and `MouseAdapter` for click handling), verified against the current RuneLite source. Still, the very first `./gradlew run` on your machine is the real compile check - if it reports an error, send it over and it'll get fixed.

## Ideas for later

- A "clear all" button to dismiss every pinned message at once.
- Also catching friends chat / clan chat, if you want the same treatment there.
- An option to auto-drop a pinned message after N minutes instead of only via the tick button.
