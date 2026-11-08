package com.elbaerk.uniquedropkc;

import com.google.gson.Gson;
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
}
