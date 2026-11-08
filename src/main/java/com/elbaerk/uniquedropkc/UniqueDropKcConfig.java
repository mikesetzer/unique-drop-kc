package com.elbaerk.uniquedropkc;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(UniqueDropKcConfig.GROUP)
public interface UniqueDropKcConfig extends Config
{
	String GROUP = "uniqueDropKc";

	/** Exact phrase required before debug mode can activate (case-sensitive). */
	String UNLOCK_PHRASE = "ENABLE DEBUG";

	String KEY_UNLOCK = "debugUnlockPhrase";
	String KEY_DEBUG_MODE = "debugMode";
	String KEY_DEBUG_INSTRUCTIONS = "debugInstructions";

	@ConfigSection(
		name = "Debug",
		description = "Developer testing tools. Requires an unlock phrase.",
		position = 100,
		closedByDefault = true
	)
	String debugSection = "debugSection";

	@ConfigItem(
		keyName = "showPanel",
		name = "Show side panel",
		description = "Show the Unique Drop KC side panel with hover tooltips for tracked uniques.",
		position = 0
	)
	default boolean showPanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = KEY_UNLOCK,
		name = "Unlock phrase",
		description = "Type the exact unlock phrase to allow enabling debug mode.",
		section = debugSection,
		position = 0,
		secret = true
	)
	default String debugUnlockPhrase()
	{
		return "";
	}

	@ConfigItem(
		keyName = KEY_DEBUG_MODE,
		name = "Debug mode",
		description = "Enable debug chat commands. Only works when the unlock phrase matches. Disabling wipes debug data only (real drops are kept).",
		section = debugSection,
		position = 1
	)
	default boolean debugMode()
	{
		return false;
	}

	@ConfigItem(
		keyName = KEY_DEBUG_INSTRUCTIONS,
		name = "Debug commands",
		description = "Filled in automatically while debug mode is active.",
		section = debugSection,
		position = 2
	)
	default String debugInstructions()
	{
		return "";
	}
}
