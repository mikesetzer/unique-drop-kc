package com.elbaerk.uniquedropkc;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.loottracker.LootRecordType;

@Slf4j
@Singleton
class UniqueDropKcStore
{
	static final int MAX_ENTRIES = 5;

	private static final String INDEX_KEY = "index";
	private static final String DEBUG_INDEX_KEY = "debug_index";
	private static final String HIST_PREFIX = "hist_";
	private static final String DEBUG_HIST_PREFIX = "debug_hist_";

	private static final Type HISTORY_TYPE = new TypeToken<Map<Integer, int[]>>()
	{
	}.getType();

	private static final Type INDEX_TYPE = new TypeToken<List<String>>()
	{
	}.getType();

	private final ConfigManager configManager;
	private final ItemManager itemManager;
	private final Gson gson;

	private final Object lock = new Object();
	private List<String> indexCache;
	private List<String> debugIndexCache;
	private final Map<String, Map<Integer, int[]>> historyCache = new HashMap<>();

	@Inject
	UniqueDropKcStore(ConfigManager configManager, ItemManager itemManager, Gson gson)
	{
		this.configManager = configManager;
		this.itemManager = itemManager;
		this.gson = gson;
	}

	/**
	 * Clears in-memory caches (e.g. after profile changes).
	 */
	void clearCache()
	{
		synchronized (lock)
		{
			indexCache = null;
			debugIndexCache = null;
			historyCache.clear();
		}
	}

	/**
	 * Removes legacy per-item name keys previously persisted for display.
	 * Names are resolved from {@link ItemManager} at UI time instead.
	 */
	void purgeLegacyItemNames()
	{
		synchronized (lock)
		{
			for (String sourceKey : loadIndex(false))
			{
				ParsedSource parsed = parseSourceKey(sourceKey);
				if (parsed == null)
				{
					continue;
				}
				for (Integer itemId : loadSourceMap(parsed.type, parsed.name, false).keySet())
				{
					configManager.unsetRSProfileConfiguration(UniqueDropKcConfig.GROUP, nameKey(itemId));
				}
			}
		}
	}

	/**
	 * @return true if a new KC stamp was recorded (false when unchanged / duplicate consecutive KC)
	 */
	boolean recordDrop(LootRecordType type, String sourceName, int itemId, int kc)
	{
		return record(type, sourceName, itemId, kc, false);
	}

	/**
	 * Records into the debug store only. Never writes real hist/index keys.
	 *
	 * @return true if a new KC stamp was recorded
	 */
	boolean recordDebugDrop(LootRecordType type, String sourceName, int itemId, int kc)
	{
		return record(type, sourceName, itemId, kc, true);
	}

	/**
	 * Deletes all debug_* RS-profile keys for the current account. Real data is untouched.
	 */
	void clearDebug()
	{
		synchronized (lock)
		{
			List<String> index = new ArrayList<>(loadIndex(true));
			for (String key : index)
			{
				configManager.unsetRSProfileConfiguration(UniqueDropKcConfig.GROUP, key);
				historyCache.remove(key);
			}
			configManager.unsetRSProfileConfiguration(UniqueDropKcConfig.GROUP, DEBUG_INDEX_KEY);
			debugIndexCache = null;
		}
	}

	List<UniqueDropRecord> listAll(boolean includeDebug)
	{
		synchronized (lock)
		{
			Map<String, UniqueDropRecord> merged = new LinkedHashMap<>();

			collectInto(merged, false);
			if (includeDebug)
			{
				collectInto(merged, true);
			}

			List<UniqueDropRecord> out = new ArrayList<>(merged.values());
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

	private boolean record(LootRecordType type, String sourceName, int itemId, int kc, boolean debug)
	{
		synchronized (lock)
		{
			Map<Integer, int[]> map = loadSourceMap(type, sourceName, debug);
			int[] existing = map.get(itemId);
			int[] updated = prepend(existing, kc);
			if (updated == existing)
			{
				return false;
			}

			map.put(itemId, updated);
			saveSourceMap(type, sourceName, map, debug);
			return true;
		}
	}

	private void collectInto(Map<String, UniqueDropRecord> merged, boolean debug)
	{
		for (String sourceKey : loadIndex(debug))
		{
			ParsedSource parsed = parseSourceKey(sourceKey);
			if (parsed == null)
			{
				continue;
			}
			Map<Integer, int[]> map = loadSourceMap(parsed.type, parsed.name, debug);
			for (Map.Entry<Integer, int[]> e : map.entrySet())
			{
				int itemId = e.getKey();
				String mergeKey = parsed.name.toLowerCase() + '\0' + itemId;
				UniqueDropRecord existing = merged.get(mergeKey);
				if (existing == null)
				{
					int[] real = debug ? new int[0] : e.getValue();
					int[] dbg = debug ? e.getValue() : new int[0];
					merged.put(mergeKey, new UniqueDropRecord(
						parsed.type, parsed.name, itemId, resolveItemName(itemId), real, dbg));
				}
				else if (debug)
				{
					merged.put(mergeKey, new UniqueDropRecord(
						existing.getType(),
						existing.getSourceName(),
						existing.getItemId(),
						existing.getItemName(),
						existing.getReceivedAtKcs(),
						e.getValue()));
				}
				else
				{
					merged.put(mergeKey, new UniqueDropRecord(
						existing.getType(),
						existing.getSourceName(),
						existing.getItemId(),
						existing.getItemName(),
						e.getValue(),
						existing.getDebugReceivedAtKcs()));
				}
			}
		}
	}

	private String resolveItemName(int itemId)
	{
		try
		{
			String name = itemManager.getItemComposition(itemId).getName();
			if (name != null && !name.isEmpty())
			{
				return name;
			}
		}
		catch (RuntimeException ex)
		{
			log.debug("Unable to resolve item name for id {}", itemId, ex);
		}
		return "Item " + itemId;
	}

	private Map<Integer, int[]> loadSourceMap(LootRecordType type, String sourceName, boolean debug)
	{
		String key = historyKey(type, sourceName, debug);
		Map<Integer, int[]> cached = historyCache.get(key);
		if (cached != null)
		{
			return cached;
		}

		String json = configManager.getRSProfileConfiguration(UniqueDropKcConfig.GROUP, key);
		Map<Integer, int[]> map = parseHistory(json);
		historyCache.put(key, map);
		return map;
	}

	private void saveSourceMap(LootRecordType type, String sourceName, Map<Integer, int[]> map, boolean debug)
	{
		String key = historyKey(type, sourceName, debug);
		historyCache.put(key, map);
		configManager.setRSProfileConfiguration(UniqueDropKcConfig.GROUP, key, gson.toJson(map));
		addToIndex(key, debug);
	}

	private List<String> loadIndex(boolean debug)
	{
		if (debug)
		{
			if (debugIndexCache != null)
			{
				return debugIndexCache;
			}
			String indexJson = configManager.getRSProfileConfiguration(UniqueDropKcConfig.GROUP, DEBUG_INDEX_KEY);
			debugIndexCache = parseIndex(indexJson);
			return debugIndexCache;
		}

		if (indexCache != null)
		{
			return indexCache;
		}
		String indexJson = configManager.getRSProfileConfiguration(UniqueDropKcConfig.GROUP, INDEX_KEY);
		indexCache = parseIndex(indexJson);
		return indexCache;
	}

	private void addToIndex(String historyKey, boolean debug)
	{
		List<String> index = loadIndex(debug);
		Set<String> seen = new HashSet<>(index);
		if (seen.add(historyKey))
		{
			index.add(historyKey);
			if (debug)
			{
				debugIndexCache = index;
				configManager.setRSProfileConfiguration(UniqueDropKcConfig.GROUP, DEBUG_INDEX_KEY, gson.toJson(index));
			}
			else
			{
				indexCache = index;
				configManager.setRSProfileConfiguration(UniqueDropKcConfig.GROUP, INDEX_KEY, gson.toJson(index));
			}
		}
	}

	private Map<Integer, int[]> parseHistory(String json)
	{
		if (json == null || json.isEmpty())
		{
			return new LinkedHashMap<>();
		}
		try
		{
			Map<Integer, int[]> map = gson.fromJson(json, HISTORY_TYPE);
			return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
		}
		catch (JsonParseException | NumberFormatException | ClassCastException ex)
		{
			log.warn("Corrupt Unique Drop KC history JSON; ignoring", ex);
			return new LinkedHashMap<>();
		}
	}

	private List<String> parseIndex(String json)
	{
		if (json == null || json.isEmpty())
		{
			return new ArrayList<>();
		}
		try
		{
			List<String> index = gson.fromJson(json, INDEX_TYPE);
			return index == null ? new ArrayList<>() : new ArrayList<>(index);
		}
		catch (JsonParseException | ClassCastException ex)
		{
			log.warn("Corrupt Unique Drop KC index JSON; ignoring", ex);
			return new ArrayList<>();
		}
	}

	static String historyKey(LootRecordType type, String sourceName)
	{
		return historyKey(type, sourceName, false);
	}

	static String historyKey(LootRecordType type, String sourceName, boolean debug)
	{
		return (debug ? DEBUG_HIST_PREFIX : HIST_PREFIX) + type.name() + "_" + sourceName;
	}

	static String nameKey(int itemId)
	{
		return "name_" + itemId;
	}

	private static ParsedSource parseSourceKey(String historyKey)
	{
		if (historyKey == null)
		{
			return null;
		}

		String rest;
		if (historyKey.startsWith(DEBUG_HIST_PREFIX))
		{
			rest = historyKey.substring(DEBUG_HIST_PREFIX.length());
		}
		else if (historyKey.startsWith(HIST_PREFIX))
		{
			rest = historyKey.substring(HIST_PREFIX.length());
		}
		else
		{
			return null;
		}

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
