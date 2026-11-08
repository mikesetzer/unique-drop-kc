package com.elbaerk.uniquedropkc;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.http.api.loottracker.LootRecordType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UniqueDropKcDebugCommandsTest
{
	@Mock
	private Client client;

	@Mock
	private UniqueDropKcStore store;

	private UniqueDropKcDebugCommands commands;

	@Before
	public void setUp()
	{
		commands = new UniqueDropKcDebugCommands(client, store);
	}

	@Test
	public void helpDoesNotRefresh()
	{
		assertFalse(commands.handle(new String[]{"help"}));
		verify(store, never()).recordDebugDrop(eq(LootRecordType.NPC), anyString(), anyInt(), anyInt());
		verify(client).addChatMessage(eq(ChatMessageType.GAMEMESSAGE), eq(""), anyString(), isNull());
	}

	@Test
	public void addMultiWordSource()
	{
		when(store.recordDebugDrop(LootRecordType.NPC, "General Graardor", 11818, 50)).thenReturn(true);

		assertTrue(commands.handle(new String[]{"add", "General", "Graardor", "11818", "50"}));
		verify(store).recordDebugDrop(LootRecordType.NPC, "General Graardor", 11818, 50);
	}

	@Test
	public void addRejectsUnknownItem()
	{
		assertFalse(commands.handle(new String[]{"add", "Zulrah", "1", "100"}));
		verify(store, never()).recordDebugDrop(eq(LootRecordType.NPC), anyString(), anyInt(), anyInt());
	}

	@Test
	public void addRejectsBadInts()
	{
		assertFalse(commands.handle(new String[]{"add", "Zulrah", "fang", "100"}));
		verify(store, never()).recordDebugDrop(eq(LootRecordType.NPC), anyString(), anyInt(), anyInt());
	}

	@Test
	public void clearWipesDebug()
	{
		assertTrue(commands.handle(new String[]{"clear"}));
		verify(store).clearDebug();
	}
}
