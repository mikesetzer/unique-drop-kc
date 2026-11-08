package com.elbaerk.uniquedropkc;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class UniqueDropKcStoreTest
{
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
}
