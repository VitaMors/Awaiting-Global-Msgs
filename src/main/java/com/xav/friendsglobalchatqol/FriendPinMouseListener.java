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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import net.runelite.client.input.MouseAdapter;

/**
 * Listens for clicks on the tick buttons drawn by {@link FriendPinOverlay}.
 * Clicking any tick dismisses that message and every message pinned before
 * it (oldest first), so hovering down the stack and clicking marks however
 * many messages you hovered over as read in one go. Clicking the "+N more"
 * overflow row dismisses everything, including messages not currently drawn.
 */
class FriendPinMouseListener extends MouseAdapter
{
	private final FriendsGlobalChatQolPlugin plugin;

	FriendPinMouseListener(FriendsGlobalChatQolPlugin plugin)
	{
		this.plugin = plugin;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent mouseEvent)
	{
		Point point = mouseEvent.getPoint();

		Rectangle overflowBounds = plugin.getOverflowRowBounds();
		if (overflowBounds != null && overflowBounds.contains(point))
		{
			plugin.dismissAll();
			mouseEvent.consume();
			return mouseEvent;
		}

		for (PinnedMessage message : plugin.getPinnedMessages())
		{
			Rectangle tickBounds = message.getTickBounds();
			if (tickBounds != null && tickBounds.contains(point))
			{
				plugin.dismissUpToAndIncluding(message);
				mouseEvent.consume();
				break;
			}
		}

		return mouseEvent;
	}
}
