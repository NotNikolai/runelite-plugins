package com.theotherhalf.notify;

import com.theotherhalf.TheOtherHalfConfig;
import java.util.Locale;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;

// the game won't announce these, the real level hasn't moved
public class LevelUpNotifier
{
	private static final String PREFIX = "The Other Half: ";
	private static final int MILESTONE_EVERY = 10;

	// nothing in the api names it, this is what Annoyance Mute silences
	private static final int LEVEL_UP_SOUND = 2396;

	// spotanim table key, 0 is the slot a level up uses
	private static final int SPOTANIM_SLOT = 0;

	// world units, a tile is 128. at 0 it goes off at your feet.
	private static final int SPOTANIM_HEIGHT = 128;

	@Inject
	private Client client;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private TheOtherHalfConfig config;

	// finalLevel lands on the same xp as a real 99, so it gets the loudest one
	public void announce(Skill skill, int level, boolean finalLevel)
	{
		if (config.levelUpMessage())
		{
			postChat(skill, level, finalLevel);
		}

		boolean milestone = finalLevel || level % MILESTONE_EVERY == 0;
		if (config.bigMilestonesOnly() && !milestone)
		{
			return;
		}

		celebrate(milestone ? SpotanimID.LEVELUP_99_ANIM : SpotanimID.LEVELUP_ANIM);
	}

	private void celebrate(int spotanim)
	{
		if (isSuppressed())
		{
			return;
		}

		if (config.levelUpSound())
		{
			client.playSoundEffect(LEVEL_UP_SOUND);
		}

		if (config.levelUpFireworks())
		{
			Player local = client.getLocalPlayer();
			if (local != null)
			{
				local.createSpotAnim(SPOTANIM_SLOT, spotanim, SPOTANIM_HEIGHT, 0);
			}
		}
	}

	private void postChat(Skill skill, int level, boolean finalLevel)
	{
		// no "your" here, Screenshot matches ".*Your (skill) (level is|are)? now (\d+)."
		String text = finalLevel
			? PREFIX + prettyName(skill) + " is now level " + level + ", and that is a real 99."
			: PREFIX + prettyName(skill) + " is now level " + level + ".";

		String formatted = new ChatMessageBuilder()
			.append(config.levelUpColour(), text)
			.build();

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(formatted)
			.build());
	}

	private boolean isSuppressed()
	{
		if (!config.suppressInCombat())
		{
			return false;
		}

		if (client.isInInstancedRegion())
		{
			return true;
		}

		// health bar only shows in real combat. going off getInteracting would kill
		// this for fishing and mining too.
		Player local = client.getLocalPlayer();
		return local != null && local.getHealthRatio() != -1;
	}

	private static String prettyName(Skill skill)
	{
		String name = skill.name();
		return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
	}
}
