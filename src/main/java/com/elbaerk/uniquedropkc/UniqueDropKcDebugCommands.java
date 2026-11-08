package com.elbaerk.uniquedropkc;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.http.api.loottracker.LootRecordType;

@Singleton
class UniqueDropKcDebugCommands
{
	static final String USAGE = "Usage: ::udkc help | add <source> <itemId> <kc> | clear";
	static final String HELP = USAGE + " — example: ::udkc add Zulrah 12922 100";

	private final Client client;
	private final UniqueDropKcStore store;

	@Inject
	UniqueDropKcDebugCommands(Client client, UniqueDropKcStore store)
	{
		this.client = client;
		this.store = store;
	}

	/**
	 * @param args command arguments after {@code udkc} (may be null/empty)
	 * @return true if the panel should refresh
	 */
	boolean handle(String[] args)
	{
		if (args == null || args.length == 0)
		{
			chat(USAGE);
			return false;
		}

		String sub = args[0].toLowerCase();
		switch (sub)
		{
			case "help":
				chat(HELP);
				return false;
			case "clear":
				store.clearDebug();
				chat("Unique Drop KC: cleared debug data.");
				return true;
			case "add":
				return handleAdd(args);
			default:
				chat(USAGE);
				return false;
		}
	}

	private boolean handleAdd(String[] args)
	{
		// add <source words...> <itemId> <kc>  → at least 4 tokens total including "add"
		if (args.length < 4)
		{
			chat(USAGE);
			return false;
		}

		String itemIdToken = args[args.length - 2];
		String kcToken = args[args.length - 1];
		int itemId;
		int kc;
		try
		{
			itemId = Integer.parseInt(itemIdToken);
			kc = Integer.parseInt(kcToken);
		}
		catch (NumberFormatException ex)
		{
			chat(USAGE);
			return false;
		}

		if (kc <= 0)
		{
			chat("Unique Drop KC: kc must be a positive integer.");
			return false;
		}

		if (!CollectionLogItemIds.contains(itemId))
		{
			chat("Unique Drop KC: item id " + itemId + " is not a tracked collection-log unique.");
			return false;
		}

		StringBuilder source = new StringBuilder(args[1]);
		for (int i = 2; i < args.length - 2; i++)
		{
			source.append(' ').append(args[i]);
		}
		String sourceName = source.toString();

		boolean stamped = store.recordDebugDrop(LootRecordType.NPC, sourceName, itemId, kc);
		if (stamped)
		{
			chat("Unique Drop KC debug: " + sourceName + " item " + itemId + " @ " + kc + " kc");
		}
		else
		{
			chat("Unique Drop KC debug: unchanged (duplicate consecutive kc).");
		}
		return stamped;
	}

	private void chat(String message)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
	}
}
