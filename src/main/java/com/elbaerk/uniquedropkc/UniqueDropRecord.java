package com.elbaerk.uniquedropkc;

import lombok.Value;
import net.runelite.http.api.loottracker.LootRecordType;

@Value
class UniqueDropRecord
{
	LootRecordType type;
	String sourceName;
	int itemId;
	String itemName;
	int[] receivedAtKcs;
}
