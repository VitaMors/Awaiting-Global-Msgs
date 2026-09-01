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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws the stack of pinned friend messages over the top of the chatbox's
 * message area, each with a circular tick button on the right that
 * dismisses it.
 * <p>
 * Only the oldest pinned message's tick is bright green by default; every
 * other tick is greyed out. Hovering down over the stack "activates" (turns
 * green) every row from the oldest through whichever one the cursor is
 * over, previewing a bulk mark-as-read - clicking a tick then dismisses that
 * message and everything older than it, so how far down you hover controls
 * how many messages a single click clears.
 * <p>
 * The stack is deliberately capped to however many rows fit in the
 * chatbox's own message area, minus a couple of rows always left free so
 * live/normal chat stays visible underneath - if more messages are pinned
 * than that, the extra ones are summarized in a single "+N more" row
 * (clicking it dismisses everything, including the messages it's
 * summarizing) rather than growing the overlay past the chatbox and onto
 * the rest of the game screen.
 */
class FriendPinOverlay extends Overlay
{
	private static final int ROW_HEIGHT = 17;
	private static final int PADDING = 4;
	private static final int TICK_DIAMETER = 13;
	private static final int MIN_VISIBLE_NATIVE_ROWS = 2;
	private static final Color ROW_BACKGROUND = new Color(0, 0, 0, 195);
	private static final Color OVERFLOW_ROW_BACKGROUND_HOVER = new Color(35, 90, 55, 220);
	private static final Color TICK_COLOR = new Color(46, 204, 113);
	private static final Color TICK_HOVER_COLOR = new Color(76, 224, 143);
	private static final Color TICK_INACTIVE_COLOR = new Color(120, 120, 120);
	private static final Color OVERFLOW_TEXT_COLOR = new Color(200, 200, 200);

	private final Client client;
	private final FriendsGlobalChatQolPlugin plugin;
	private final FriendsGlobalChatQolConfig config;

	@Inject
	private FriendPinOverlay(Client client, FriendsGlobalChatQolPlugin plugin, FriendsGlobalChatQolConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.pinMessages())
		{
			return null;
		}

		List<PinnedMessage> messages = plugin.getPinnedMessages();
		int totalMessages = messages.size();
		if (totalMessages == 0)
		{
			return null;
		}

		Widget chatboxMessages = client.getWidget(InterfaceID.Chatbox.SCROLLAREA);
		if (chatboxMessages == null || chatboxMessages.isHidden())
		{
			return null;
		}

		Rectangle bounds = chatboxMessages.getBounds();
		if (bounds == null || bounds.isEmpty())
		{
			return null;
		}

		// Never let the pinned stack grow past the chatbox's own message
		// area - always leave at least a couple of rows free so live chat
		// stays visible underneath, and summarize anything that doesn't fit
		// instead of drawing off the bottom of the chatbox.
		int reservedForLiveChat = ROW_HEIGHT * MIN_VISIBLE_NATIVE_ROWS;
		int availableHeight = Math.max(ROW_HEIGHT, bounds.height - reservedForLiveChat);
		int maxRows = Math.max(1, availableHeight / ROW_HEIGHT);

		int rowsToDraw;
		boolean overflow;
		if (totalMessages <= maxRows)
		{
			rowsToDraw = totalMessages;
			overflow = false;
		}
		else
		{
			rowsToDraw = Math.max(1, maxRows - 1);
			overflow = true;
		}

		graphics.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics fontMetrics = graphics.getFontMetrics();

		Color highlight = config.highlightColor();
		// client.getMouseCanvasPosition() returns net.runelite.api.Point, not
		// java.awt.Point - convert it once here rather than importing both
		// same-named classes.
		net.runelite.api.Point rawMouse = client.getMouseCanvasPosition();
		java.awt.Point mouse = rawMouse == null ? null : new java.awt.Point(rawMouse.getX(), rawMouse.getY());

		int x = bounds.x;
		int startY = bounds.y;
		int y = startY;
		int width = bounds.width;

		// Which row (0 = oldest) the cursor is currently over, if any -
		// null means the cursor isn't over the stack at all this frame.
		Integer hoveredRow = null;
		if (mouse != null && mouse.x >= x && mouse.x < x + width && mouse.y >= startY)
		{
			hoveredRow = (mouse.y - startY) / ROW_HEIGHT;
		}

		// Only rows within the actually-drawn message rows count as
		// "activating" the bulk-select preview; hovering the overflow row
		// (handled separately below) or below everything doesn't.
		int hoveredIndex = (hoveredRow != null && hoveredRow < rowsToDraw) ? hoveredRow : -1;

		int rowsDrawn = 0;

		// CopyOnWriteArrayList's iterator is a fixed snapshot, so this stays
		// safe even if a tick button is clicked (removing a message from
		// the underlying list) on another thread while this is rendering.
		for (PinnedMessage message : messages)
		{
			if (rowsDrawn >= rowsToDraw)
			{
				break;
			}

			// The oldest message (row 0) is active by default so there's
			// always an obvious "next thing to read"; hovering down the
			// stack extends the active/green run to whatever's hovered.
			boolean active = hoveredIndex >= 0 ? rowsDrawn <= hoveredIndex : rowsDrawn == 0;
			boolean exactHover = rowsDrawn == hoveredIndex;

			drawMessageRow(graphics, fontMetrics, message, x, y, width, highlight, active, exactHover);

			y += ROW_HEIGHT;
			rowsDrawn++;
		}

		int totalRows = rowsDrawn;

		if (overflow)
		{
			int hiddenCount = totalMessages - rowsDrawn;
			boolean overflowHovered = hoveredRow != null && hoveredRow == rowsToDraw;
			Rectangle overflowBounds = drawOverflowRow(graphics, fontMetrics, hiddenCount, x, y, width, overflowHovered);
			plugin.setOverflowRowBounds(overflowBounds);
			y += ROW_HEIGHT;
			totalRows++;
		}
		else
		{
			plugin.setOverflowRowBounds(null);
		}

		return new Dimension(width, ROW_HEIGHT * totalRows);
	}

	private void drawMessageRow(Graphics2D graphics, FontMetrics fontMetrics, PinnedMessage message,
		int x, int y, int width, Color highlight, boolean active, boolean exactHover)
	{
		graphics.setColor(ROW_BACKGROUND);
		graphics.fillRect(x, y, width, ROW_HEIGHT);

		graphics.setColor(highlight);
		graphics.fillRect(x, y, 3, ROW_HEIGHT);

		int tickX = x + width - PADDING - TICK_DIAMETER;
		int tickY = y + (ROW_HEIGHT - TICK_DIAMETER) / 2;
		Rectangle tickBounds = new Rectangle(tickX, tickY, TICK_DIAMETER, TICK_DIAMETER);

		int textX = x + 8;
		int maxTextWidth = tickX - PADDING - textX;
		String full = message.getSender() + ": " + message.getText();
		String display = truncate(fontMetrics, full, maxTextWidth);

		graphics.setColor(highlight);
		graphics.drawString(display, textX, y + ROW_HEIGHT - 5);

		Color tickColor;
		if (!active)
		{
			tickColor = TICK_INACTIVE_COLOR;
		}
		else
		{
			tickColor = exactHover ? TICK_HOVER_COLOR : TICK_COLOR;
		}

		Ellipse2D circle = new Ellipse2D.Double(tickX, tickY, TICK_DIAMETER, TICK_DIAMETER);
		graphics.setColor(tickColor);
		graphics.fill(circle);

		graphics.setColor(Color.WHITE);
		graphics.setStroke(new BasicStroke(1.7f));
		graphics.drawLine(tickX + 3, tickY + 7, tickX + 5, tickY + 10);
		graphics.drawLine(tickX + 5, tickY + 10, tickX + 10, tickY + 3);

		// Published last so a click landing mid-render always sees a
		// fully-drawn button at this position, never a half-updated one.
		message.setTickBounds(tickBounds);
	}

	private Rectangle drawOverflowRow(Graphics2D graphics, FontMetrics fontMetrics, int hiddenCount,
		int x, int y, int width, boolean hovered)
	{
		graphics.setColor(hovered ? OVERFLOW_ROW_BACKGROUND_HOVER : ROW_BACKGROUND);
		graphics.fillRect(x, y, width, ROW_HEIGHT);

		String text = "+" + hiddenCount + " more - click to mark all read";
		String display = truncate(fontMetrics, text, width - 16);

		graphics.setColor(hovered ? Color.WHITE : OVERFLOW_TEXT_COLOR);
		graphics.drawString(display, x + 8, y + ROW_HEIGHT - 5);

		return new Rectangle(x, y, width, ROW_HEIGHT);
	}

	private static String truncate(FontMetrics fontMetrics, String text, int maxWidth)
	{
		if (maxWidth <= 0)
		{
			return "";
		}

		if (fontMetrics.stringWidth(text) <= maxWidth)
		{
			return text;
		}

		String ellipsis = "...";
		int ellipsisWidth = fontMetrics.stringWidth(ellipsis);
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < text.length(); i++)
		{
			char c = text.charAt(i);
			if (fontMetrics.stringWidth(builder.toString()) + fontMetrics.charWidth(c) + ellipsisWidth > maxWidth)
			{
				break;
			}
			builder.append(c);
		}

		return builder + ellipsis;
	}
}
