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

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(FriendsGlobalChatQolConfig.GROUP)
public interface FriendsGlobalChatQolConfig extends Config
{
	String GROUP = "friendsglobalchatqol";

	@ConfigItem(
		keyName = "highlightMessages",
		name = "Highlight friends' messages",
		description = "Recolors public/global chat messages sent by players on your friends list, like they've been marked with a highlighter pen.",
		position = 0
	)
	default boolean highlightMessages()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightColor",
		name = "Highlight color",
		description = "The color used to highlight a friend's public chat message. Defaults to a lighter shade of blue than the normal public chat text.",
		position = 1
	)
	default Color highlightColor()
	{
		return new Color(135, 206, 250);
	}

	@ConfigItem(
		keyName = "pinMessages",
		name = "Pin messages until acknowledged",
		description = "Keeps friends' public/global chat messages stacked at the top of the chatbox until you click the green tick to dismiss them.",
		position = 2
	)
	default boolean pinMessages()
	{
		return true;
	}

	@Range(min = 1, max = 50)
	@ConfigItem(
		keyName = "maxPinnedMessages",
		name = "Max pinned messages",
		description = "The maximum number of unacknowledged messages to keep stacked at once. The oldest unacknowledged message is dropped beyond this limit.",
		position = 3
	)
	default int maxPinnedMessages()
	{
		return 15;
	}
}
