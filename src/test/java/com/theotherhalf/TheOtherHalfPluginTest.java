package com.theotherhalf;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TheOtherHalfPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TheOtherHalfPlugin.class);
		RuneLite.main(args);
	}
}
