package com.nhfc;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(NhfcConfig.GROUP)
public interface NhfcConfig extends Config
{
	String GROUP = "nhfc-league";
	String TOKEN_KEY = "token";

	@ConfigItem(
		keyName = "enabled",
		name = "Connect to NHFC",
		description = "Connects this client to the NHFC league site. Nothing about your gameplay is sent unless staff arm one of your NHFC fights.",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
		position = 0
	)
	default boolean enabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "connectCode",
		name = "Connect code",
		description = "The code from NHFC staff. It works once, then the plugin remembers you.",
		position = 1
	)
	default String connectCode()
	{
		return "";
	}

	@ConfigItem(
		keyName = "serverUrl",
		name = "Server",
		description = "The NHFC site. Leave this alone unless staff ask you to change it.",
		position = 2
	)
	default String serverUrl()
	{
		return "http://localhost:3000";
	}

	// kept by the plugin itself after the connect code is used
	@ConfigItem(
		keyName = TOKEN_KEY,
		name = "",
		description = "",
		hidden = true,
		secret = true
	)
	default String token()
	{
		return "";
	}
}
