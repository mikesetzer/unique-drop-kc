package com.elbaerk.uniquedropkc;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class UniqueDropKcPluginLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(UniqueDropKcPlugin.class);
		RuneLite.main(args);
	}
}
