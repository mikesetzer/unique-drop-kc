package com.elbaerk.uniquedropkc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

class UniqueDropKcPanel extends PluginPanel
{
	private static final int ITEMS_PER_ROW = 5;

	private final ItemManager itemManager;
	private final JPanel content = new JPanel();

	@Inject
	UniqueDropKcPanel(ItemManager itemManager)
	{
		this.itemManager = itemManager;

		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());

		JLabel title = new JLabel("Unique Drop KC");
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setBorder(new EmptyBorder(0, 0, 10, 0));
		add(title, BorderLayout.NORTH);

		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(content, BorderLayout.CENTER);
	}

	void rebuild(List<UniqueDropRecord> records)
	{
		content.removeAll();

		JLabel hint = new JLabel("<html>Hover a unique for last received KCs.<br>Requires Loot Tracker + Chat Commands.</html>");
		hint.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		hint.setFont(FontManager.getRunescapeSmallFont());
		hint.setBorder(new EmptyBorder(0, 0, 8, 0));
		content.add(hint);

		if (records == null || records.isEmpty())
		{
			JLabel empty = new JLabel("No tracked unique drops yet.");
			empty.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			empty.setFont(FontManager.getRunescapeSmallFont());
			content.add(empty);
			content.revalidate();
			content.repaint();
			return;
		}

		Map<String, List<UniqueDropRecord>> bySource = new LinkedHashMap<>();
		for (UniqueDropRecord record : records)
		{
			bySource.computeIfAbsent(record.getSourceName(), k -> new ArrayList<>()).add(record);
		}

		for (Map.Entry<String, List<UniqueDropRecord>> entry : bySource.entrySet())
		{
			content.add(buildSourceSection(entry.getKey(), entry.getValue()));
		}

		content.revalidate();
		content.repaint();
	}

	private JPanel buildSourceSection(String sourceName, List<UniqueDropRecord> records)
	{
		JPanel section = new JPanel();
		section.setLayout(new BorderLayout());
		section.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		section.setBorder(new EmptyBorder(0, 0, 8, 0));

		JLabel header = new JLabel(sourceName);
		header.setForeground(Color.WHITE);
		header.setFont(FontManager.getRunescapeSmallFont());
		header.setBorder(new EmptyBorder(6, 6, 4, 6));
		section.add(header, BorderLayout.NORTH);

		int rows = (records.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW;
		JPanel grid = new JPanel(new GridLayout(rows, ITEMS_PER_ROW, 1, 1));
		grid.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		grid.setBorder(new EmptyBorder(0, 6, 6, 6));

		for (UniqueDropRecord record : records)
		{
			grid.add(buildItemSlot(record));
		}
		for (int i = records.size(); i < rows * ITEMS_PER_ROW; i++)
		{
			JPanel filler = new JPanel();
			filler.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			grid.add(filler);
		}

		section.add(grid, BorderLayout.CENTER);
		return section;
	}

	private static String escapeHtml(String value)
	{
		if (value == null || value.isEmpty())
		{
			return "";
		}
		return value
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;");
	}

	private JPanel buildItemSlot(UniqueDropRecord record)
	{
		JPanel slot = new JPanel(new BorderLayout());
		slot.setBackground(ColorScheme.DARK_GRAY_COLOR);
		slot.setPreferredSize(new Dimension(42, 42));

		JLabel icon = new JLabel();
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setVerticalAlignment(SwingConstants.CENTER);

		StringBuilder html = new StringBuilder("<html>");
		html.append(escapeHtml(record.getItemName()));
		String realTip = UniqueDropKcStore.formatTooltipLine(record.getReceivedAtKcs());
		if (realTip != null)
		{
			html.append("<br>").append(escapeHtml(realTip));
		}
		String debugTip = UniqueDropKcStore.formatTooltipLine(record.getDebugReceivedAtKcs());
		if (debugTip != null)
		{
			html.append("<br>").append(escapeHtml("[debug] " + debugTip));
		}
		html.append("</html>");
		icon.setToolTipText(html.toString());

		AsyncBufferedImage image = itemManager.getImage(record.getItemId(), 1, false);
		image.addTo(icon);

		slot.add(icon, BorderLayout.CENTER);
		return slot;
	}
}
