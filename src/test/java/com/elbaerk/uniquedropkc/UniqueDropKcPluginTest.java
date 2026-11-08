package com.elbaerk.uniquedropkc;

import net.runelite.http.api.loottracker.LootRecordType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UniqueDropKcPluginTest
{
	@Test
	public void tracksNpcSources()
	{
		assertTrue(UniqueDropKcPlugin.isTrackedSource(LootRecordType.NPC, "General Graardor"));
	}

	@Test
	public void tracksRaidEvents()
	{
		assertTrue(UniqueDropKcPlugin.isTrackedSource(LootRecordType.EVENT, "Chambers of Xeric"));
		assertTrue(UniqueDropKcPlugin.isTrackedSource(LootRecordType.EVENT, "Theatre of Blood"));
		assertTrue(UniqueDropKcPlugin.isTrackedSource(LootRecordType.EVENT, "Tombs of Amascut"));
		assertTrue(UniqueDropKcPlugin.isTrackedSource(LootRecordType.EVENT, "Fractured Archive"));
	}

	@Test
	public void ignoresNonRaidEvents()
	{
		assertFalse(UniqueDropKcPlugin.isTrackedSource(LootRecordType.EVENT, "Herbiboar"));
		assertFalse(UniqueDropKcPlugin.isTrackedSource(LootRecordType.PICKPOCKET, "Man"));
		assertFalse(UniqueDropKcPlugin.isTrackedSource(LootRecordType.PLAYER, "Player"));
	}

	@Test
	public void recognizesKnownCollectionLogItem()
	{
		assertTrue(CollectionLogItemIds.contains(20997));
	}

	@Test
	public void rejectsUnknownItem()
	{
		assertFalse(CollectionLogItemIds.contains(1));
	}
}
