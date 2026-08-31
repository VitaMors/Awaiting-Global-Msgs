/*
 * Copyright (c) 2026, Xav
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.xav.friendsglobalchatqol;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

/**
 * Highlights public ("global") chat messages sent by players on the local
 * player's friends list, and optionally keeps them pinned in a stack at the
 * top of the chatbox - so a friend's message never scrolls out of view
 * unread - until the player dismisses it with the tick button.
 */
@PluginDescriptor(
	name = "Friends Global Chat QoL",
	description = "Highlights public/global chat messages from friends and pins them at the top of the chatbox until you tick them off",
	tags = {"friends", "chat", "highlight", "public", "global", "pin", "notification", "qol"}
)
public class FriendsGlobalChatQolPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private FriendsGlobalChatQolConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private FriendPinOverlay overlay;

	@Inject
	private MouseManager mouseManager;

	private final List<PinnedMessage> pinnedMessages = new CopyOnWriteArrayList<>();
	private final AtomicLong idGenerator = new AtomicLong();
	private FriendPinMouseListener mouseListener;

	@Provides
	FriendsGlobalChatQolConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FriendsGlobalChatQolConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		mouseListener = new FriendPinMouseListener(this);
		mouseManager.registerMouseListener(mouseListener);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);

		if (mouseListener != null)
		{
			mouseManager.unregisterMouseListener(mouseListener);
			mouseListener = null;
		}

		pinnedMessages.clear();
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		if (chatMessage.getType() != ChatMessageType.PUBLICCHAT)
		{
			return;
		}

		if (!config.highlightMessages() && !config.pinMessages())
		{
			return;
		}

		String rawName = chatMessage.getName();
		if (rawName == null)
		{
			return;
		}

		String senderName = Text.toJagexName(Text.removeTags(rawName));

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer != null && senderName.equalsIgnoreCase(localPlayer.getName()))
		{
			// never highlight/pin our own messages
			return;
		}

		if (senderName.isEmpty() || !client.isFriended(senderName, false))
		{
			return;
		}

		MessageNode messageNode = chatMessage.getMessageNode();

		if (config.highlightMessages() && messageNode != null)
		{
			Color highlight = config.highlightColor();
			messageNode.setName(ColorUtil.wrapWithColorTag(messageNode.getName(), highlight));
			messageNode.setValue(ColorUtil.wrapWithColorTag(messageNode.getValue(), highlight));
			messageNode.setRuneLiteFormatMessage(messageNode.getValue());
		}

		if (config.pinMessages())
		{
			String rawMessage = chatMessage.getMessage();
			String cleanText = rawMessage == null ? "" : Text.removeTags(rawMessage);
			pinnedMessages.add(new PinnedMessage(idGenerator.incrementAndGet(), senderName, cleanText));

			int max = config.maxPinnedMessages();
			while (pinnedMessages.size() > max)
			{
				pinnedMessages.remove(0);
			}
		}
	}

	List<PinnedMessage> getPinnedMessages()
	{
		return pinnedMessages;
	}

	void dismiss(PinnedMessage message)
	{
		pinnedMessages.remove(message);
	}
}
