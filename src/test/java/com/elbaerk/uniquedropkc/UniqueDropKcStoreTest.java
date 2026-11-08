package com.elbaerk.uniquedropkc;

import com.google.gson.Gson;
import java.util.List;
import net.runelite.api.ItemComposition;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.loottracker.LootRecordType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UniqueDropKcStoreTest
{
	@Mock
	private ConfigManager configManager;

	@Mock
	private ItemManager itemManager;

	@Mock
	private ItemComposition itemComposition;

	private UniqueDropKcStore store;

	@Before
	public void setUp()
	{
		store = new UniqueDropKcStore(configManager, itemManager, new Gson());
	}

	@Test
	public void prependCreatesSingleEntry()
	{
		assertArrayEquals(new int[]{412}, UniqueDropKcStore.prepend(null, 412));
	}

	@Test
	public void prependPutsNewestFirst()
	{
		int[] history = UniqueDropKcStore.prepend(new int[]{400, 224}, 412);
		assertArrayEquals(new int[]{412, 400, 224}, history);
	}

	@Test
	public void prependSkipsDuplicateConsecutiveKc()
	{
		int[] existing = new int[]{412, 400};
		assertSame(existing, UniqueDropKcStore.prepend(existing, 412));
	}

	@Test
	public void prependCapsAtFive()
	{
		int[] existing = new int[]{5, 4, 3, 2, 1};
		int[] next = UniqueDropKcStore.prepend(existing, 6);
		assertArrayEquals(new int[]{6, 5, 4, 3, 2}, next);
		assertEquals(UniqueDropKcStore.MAX_ENTRIES, next.length);
	}

	@Test
	public void formatTooltipLine()
	{
		assertNull(UniqueDropKcStore.formatTooltipLine(null));
		assertNull(UniqueDropKcStore.formatTooltipLine(new int[0]));
		assertEquals("Last received at 412 kc", UniqueDropKcStore.formatTooltipLine(new int[]{412}));
		assertEquals(
			"Last received at 412, 400, 224 kc",
			UniqueDropKcStore.formatTooltipLine(new int[]{412, 400, 224})
		);
	}

	@Test
	public void recordDropIgnoresCorruptHistoryJson()
	{
		String key = UniqueDropKcStore.historyKey(LootRecordType.NPC, "Zulrah");
		when(configManager.getRSProfileConfiguration(UniqueDropKcConfig.GROUP, key)).thenReturn("{not-json");
		when(configManager.getRSProfileConfiguration(UniqueDropKcConfig.GROUP, "index")).thenReturn("not-json-either");

		assertTrue(store.recordDrop(LootRecordType.NPC, "Zulrah", 12922, 100));
		verify(configManager).setRSProfileConfiguration(eq(UniqueDropKcConfig.GROUP), eq(key), anyString());
		verify(configManager).setRSProfileConfiguration(eq(UniqueDropKcConfig.GROUP), eq("index"), anyString());
	}

	@Test
	public void recordDropSkipsWriteWhenKcUnchanged()
	{
		String key = UniqueDropKcStore.historyKey(LootRecordType.NPC, "Zulrah");
		when(configManager.getRSProfileConfiguration(UniqueDropKcConfig.GROUP, key))
			.thenReturn("{\"12922\":[100]}");

		assertFalse(store.recordDrop(LootRecordType.NPC, "Zulrah", 12922, 100));
		verify(configManager, never()).setRSProfileConfiguration(eq(UniqueDropKcConfig.GROUP), eq(key), anyString());
	}

	@Test
	public void recordDebugDropWritesDebugKeysOnly()
	{
		String debugKey = UniqueDropKcStore.historyKey(LootRecordType.NPC, "Zulrah", true);
		when(configManager.getRSProfileConfiguration(UniqueDropKcConfig.GROUP, debugKey)).thenReturn(null);
		when(configManager.getRSProfileConfiguration(UniqueDropKcConfig.GROUP, "debug_index")).thenReturn(null);

		assertTrue(store.recordDebugDrop(LootRecordType.NPC, "Zulrah", 12922, 100));

		verify(configManager).setRSProfileConfiguration(eq(UniqueDropKcConfig.GROUP), eq(debugKey), anyString());
		verify(configManager).setRSProfileConfiguration(eq(UniqueDropKcConfig.GROUP), eq("debug_index"), anyString());
		verify(configManager, never()).setRSProfileConfiguration(eq(UniqueDropKcConfig.GROUP), eq("index"), anyString());
		verify(configManager, never()).setRSProfileConfiguration(
			eq(UniqueDropKcConfig.GROUP),
			eq(UniqueDropKcStore.historyKey(LootRecordType.NPC, "Zulrah", false)),
			anyString());
	}

	@Test
	public void clearDebugUnsetsDebugKeysOnly()
	{
		String debugKey = UniqueDropKcStore.historyKey(LootRecordType.NPC, "Zulrah", true);
		when(configManager.getRSProfileConfiguration(UniqueDropKcConfig.GROUP, "debug_index"))
			.thenReturn("[\"" + debugKey + "\"]");

		store.clearDebug();

		verify(configManager).unsetRSProfileConfiguration(UniqueDropKcConfig.GROUP, debugKey);
		verify(configManager).unsetRSProfileConfiguration(UniqueDropKcConfig.GROUP, "debug_index");
		verify(configManager, never()).unsetRSProfileConfiguration(eq(UniqueDropKcConfig.GROUP), eq("index"));
	}

	@Test
	public void listAllMergesRealAndDebugForSameItem()
	{
		String realKey = UniqueDropKcStore.historyKey(LootRecordType.NPC, "Zulrah", false);
		String debugKey = UniqueDropKcStore.historyKey(LootRecordType.NPC, "Zulrah", true);
		when(configManager.getRSProfileConfiguration(UniqueDropKcConfig.GROUP, "index"))
			.thenReturn("[\"" + realKey + "\"]");
		when(configManager.getRSProfileConfiguration(UniqueDropKcConfig.GROUP, "debug_index"))
			.thenReturn("[\"" + debugKey + "\"]");
		when(configManager.getRSProfileConfiguration(UniqueDropKcConfig.GROUP, realKey))
			.thenReturn("{\"12922\":[50]}");
		when(configManager.getRSProfileConfiguration(UniqueDropKcConfig.GROUP, debugKey))
			.thenReturn("{\"12922\":[100]}");
		when(itemManager.getItemComposition(12922)).thenReturn(itemComposition);
		when(itemComposition.getName()).thenReturn("Tanzanite fang");

		List<UniqueDropRecord> withoutDebug = store.listAll(false);
		assertEquals(1, withoutDebug.size());
		assertArrayEquals(new int[]{50}, withoutDebug.get(0).getReceivedAtKcs());
		assertEquals(0, withoutDebug.get(0).getDebugReceivedAtKcs().length);

		store.clearCache();
		List<UniqueDropRecord> withDebug = store.listAll(true);
		assertEquals(1, withDebug.size());
		assertArrayEquals(new int[]{50}, withDebug.get(0).getReceivedAtKcs());
		assertArrayEquals(new int[]{100}, withDebug.get(0).getDebugReceivedAtKcs());
	}
}
