package com.elbaerk.uniquedropkc;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.loottracker.LootRecordType;

@PluginDescriptor(
	name = "Unique Drop KC",
	description = "Tracks the last official killcounts when collection log uniques drop from bosses and raids.",
	tags = {"loot", "unique", "killcount", "kc", "boss", "raid", "collection", "log"}
)
public class UniqueDropKcPlugin extends Plugin
{
	private static final Set<String> RAID_EVENTS = ImmutableSet.of(
		"Chambers of Xeric",
		"Theatre of Blood",
		"Tombs of Amascut",
		"Fractured Archive"
	);

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private UniqueDropKcConfig config;

	@Inject
	private UniqueDropKcStore store;

	@Inject
	private UniqueDropKcPanel panel;

	private NavigationButton navButton;

	@Provides
	UniqueDropKcConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(UniqueDropKcConfig.class);
	}

	@Override
	protected void startUp()
	{
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "panel_icon.png");

		navButton = NavigationButton.builder()
			.tooltip("Unique Drop KC")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();

		if (config.showPanel())
		{
			clientToolbar.addNavigation(navButton);
		}

		store.purgeLegacyItemNames();
		refreshPanel();
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
	}

	@Subscribe
	public void onLootReceived(LootReceived event)
	{
		if (handleLootReceived(event) > 0)
		{
			refreshPanel();
		}
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		store.clearCache();
		refreshPanel();
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		// Different OSRS accounts share a RuneLite profile but have separate RS-profile config.
		store.clearCache();
		refreshPanel();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!UniqueDropKcConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		if ("showPanel".equals(event.getKey()))
		{
			clientToolbar.removeNavigation(navButton);
			if (config.showPanel())
			{
				clientToolbar.addNavigation(navButton);
			}
		}
	}

	/**
	 * @return number of unique items that received a new KC stamp for this event
	 */
	int handleLootReceived(LootReceived event)
	{
		LootRecordType type = event.getType();
		String name = event.getName();
		if (!isTrackedSource(type, name))
		{
			return 0;
		}

		Integer kc = getOfficialKc(name);
		if (kc == null || kc <= 0)
		{
			return 0;
		}

		Collection<ItemStack> items = event.getItems();
		if (items == null || items.isEmpty())
		{
			return 0;
		}

		Set<Integer> seen = new HashSet<>();
		int stamped = 0;
		for (ItemStack stack : items)
		{
			int id = itemManager.canonicalize(stack.getId());
			if (!seen.add(id) || !CollectionLogItemIds.contains(id))
			{
				continue;
			}
			if (store.recordDrop(type, name, id, kc))
			{
				stamped++;
			}
		}
		return stamped;
	}

	static boolean isTrackedSource(LootRecordType type, String name)
	{
		if (type == LootRecordType.NPC)
		{
			return true;
		}
		return type == LootRecordType.EVENT && name != null && RAID_EVENTS.contains(name);
	}

	private Integer getOfficialKc(String sourceName)
	{
		if (sourceName == null)
		{
			return null;
		}
		return configManager.getRSProfileConfiguration("killcount", sourceName.toLowerCase(), int.class);
	}

	private void refreshPanel()
	{
		SwingUtilities.invokeLater(() -> panel.rebuild(store.listAll()));
	}
}
