package com.elbaerk.uniquedropkc;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;
import net.runelite.http.api.loottracker.LootRecordType;

@Singleton
class UniqueDropKcStore
{
	static final int MAX_ENTRIES = 5;

	private static final Type HISTORY_TYPE = new TypeToken<Map<Integer, int[]>>()
	{
	}.getType();

	private static final Type INDEX_TYPE = new TypeToken<List<String>>()
	{
	}.getType();

	private final ConfigManager configManager;
	private final Gson gson;

	@Inject
	UniqueDropKcStore(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	int[] recordDrop(LootRecordType type, String sourceName, int itemId, String itemName, int kc)
	{
		Map<Integer, int[]> map = loadSourceMap(type, sourceName);
		int[] updated = prepend(map.get(itemId), kc);
		map.put(itemId, updated);
		saveSourceMap(type, sourceName, map);
		if (itemName != null && !itemName.isEmpty())
		{
			configManager.setRSProfileConfiguration(UniqueDropKcConfig.GROUP, nameKey(itemId), itemName);
		}
		return updated;
	}

	List<UniqueDropRecord> listAll()
	{
		List<UniqueDropRecord> out = new ArrayList<>();
		String indexJson = configManager.getRSProfileConfiguration(UniqueDropKcConfig.GROUP, "index");
		if (indexJson == null || indexJson.isEmpty())
		{
			return out;
		}

		List<String> index = gson.fromJson(indexJson, INDEX_TYPE);
		if (index == null)
		{
			return out;
		}

		for (String sourceKey : index)
		{
			ParsedSource parsed = parseSourceKey(sourceKey);
			if (parsed == null)
			{
				continue;
			}
			Map<Integer, int[]> map = loadSourceMap(parsed.type, parsed.name);
			for (Map.Entry<Integer, int[]> e : map.entrySet())
			{
				String itemName = configManager.getRSProfileConfiguration(UniqueDropKcConfig.GROUP, nameKey(e.getKey()));
				if (itemName == null)
				{
					itemName = "Item " + e.getKey();
				}
				out.add(new UniqueDropRecord(parsed.type, parsed.name, e.getKey(), itemName, e.getValue()));
			}
		}

		out.sort((a, b) ->
		{
			int c = a.getSourceName().compareToIgnoreCase(b.getSourceName());
			if (c != 0)
			{
				return c;
			}
			return a.getItemName().compareToIgnoreCase(b.getItemName());
		});
		return out;
	}

	/** Prepends kc newest-first; skips when kc equals the current head. Caps at {@link #MAX_ENTRIES}. */
	static int[] prepend(int[] existing, int kc)
	{
		if (existing != null && existing.length > 0 && existing[0] == kc)
		{
			return existing;
		}

		int oldLen = existing == null ? 0 : existing.length;
		int newLen = Math.min(MAX_ENTRIES, oldLen + 1);
		int[] next = new int[newLen];
		next[0] = kc;
		if (oldLen > 0)
		{
			System.arraycopy(existing, 0, next, 1, newLen - 1);
		}
		return next;
	}

	static String formatTooltipLine(int[] kcs)
	{
		if (kcs == null || kcs.length == 0)
		{
			return null;
		}

		StringBuilder sb = new StringBuilder("Last received at ");
		for (int i = 0; i < kcs.length; i++)
		{
			if (i > 0)
			{
				sb.append(", ");
			}
			sb.append(kcs[i]);
		}
		sb.append(" kc");
		return sb.toString();
	}

	private Map<Integer, int[]> loadSourceMap(LootRecordType type, String sourceName)
	{
		String json = configManager.getRSProfileConfiguration(UniqueDropKcConfig.GROUP, historyKey(type, sourceName));
		if (json == null || json.isEmpty())
		{
			return new LinkedHashMap<>();
		}
		Map<Integer, int[]> map = gson.fromJson(json, HISTORY_TYPE);
		return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
	}

	private void saveSourceMap(LootRecordType type, String sourceName, Map<Integer, int[]> map)
	{
		String key = historyKey(type, sourceName);
		configManager.setRSProfileConfiguration(UniqueDropKcConfig.GROUP, key, gson.toJson(map));
		addToIndex(key);
	}

	private void addToIndex(String historyKey)
	{
		String indexJson = configManager.getRSProfileConfiguration(UniqueDropKcConfig.GROUP, "index");
		List<String> index;
		if (indexJson == null || indexJson.isEmpty())
		{
			index = new ArrayList<>();
		}
		else
		{
			index = gson.fromJson(indexJson, INDEX_TYPE);
			if (index == null)
			{
				index = new ArrayList<>();
			}
		}

		Set<String> seen = new HashSet<>(index);
		if (seen.add(historyKey))
		{
			index.add(historyKey);
			configManager.setRSProfileConfiguration(UniqueDropKcConfig.GROUP, "index", gson.toJson(index));
		}
	}

	static String historyKey(LootRecordType type, String sourceName)
	{
		return "hist_" + type.name() + "_" + sourceName;
	}

	private static String nameKey(int itemId)
	{
		return "name_" + itemId;
	}

	private static ParsedSource parseSourceKey(String historyKey)
	{
		if (historyKey == null || !historyKey.startsWith("hist_"))
		{
			return null;
		}
		String rest = historyKey.substring("hist_".length());
		int us = rest.indexOf('_');
		if (us <= 0)
		{
			return null;
		}
		String typeName = rest.substring(0, us);
		String sourceName = rest.substring(us + 1);
		try
		{
			return new ParsedSource(LootRecordType.valueOf(typeName), sourceName);
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
	}

	private static final class ParsedSource
	{
		final LootRecordType type;
		final String name;

		ParsedSource(LootRecordType type, String name)
		{
			this.type = type;
			this.name = name;
		}
	}
}
