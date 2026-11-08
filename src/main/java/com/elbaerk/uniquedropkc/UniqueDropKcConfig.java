package com.elbaerk.uniquedropkc;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(UniqueDropKcConfig.GROUP)
public interface UniqueDropKcConfig extends Config
{
	String GROUP = "uniqueDropKc";

	@ConfigItem(
		keyName = "showPanel",
		name = "Show side panel",
		description = "Show the Unique Drop KC side panel with hover tooltips for tracked uniques."
	)
	default boolean showPanel()
	{
		return true;
	}
}
