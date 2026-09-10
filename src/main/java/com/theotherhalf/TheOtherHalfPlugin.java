package com.theotherhalf;

import com.google.inject.Provides;
import com.theotherhalf.model.LevelModel;
import com.theotherhalf.notify.LevelUpNotifier;
import com.theotherhalf.render.BarRenderer;
import com.theotherhalf.render.SkillTabRenderer;
import java.util.EnumMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.PostClientTick;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "The Other Half",
	description = "Resets the skills tab at 92 so you climb 1-92 twice",
	tags = {"level", "skill", "xp", "virtual", "progress", "prestige", "halfway", "bars"}
)
public class TheOtherHalfPlugin extends Plugin
{
	private static final String SKILL_TAB_TOTAL_LEVEL = "skillTabTotalLevel";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private TheOtherHalfConfig config;

	@Inject
	private SkillTabRenderer renderer;

	@Inject
	private BarRenderer bars;

	@Inject
	private LevelUpNotifier notifier;

	private LevelModel model;

	// last announced level per skill. absent means unseen this session, which is
	// how logging in stays silent.
	private final Map<Skill, Integer> lastRunLevel = new EnumMap<>(Skill.class);

	@Override
	protected void shutDown()
	{
		model = null;
		lastRunLevel.clear();
		clientThread.invoke(this::handBack);
	}

	// a redraw fixes the numbers but leaves the tint and bars behind
	private void handBack()
	{
		renderer.restore();
		bars.clear();
		redrawAllSkills();
	}

	// fires after the eventbus has us, startUp does not. first redraw belongs here
	// or the tab comes up stale.
	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		if (event.getPlugin() == this)
		{
			rebuildModel();
			clientThread.invoke(this::handBack);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!TheOtherHalfConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		rebuildModel();
		// a new threshold moves every level
		lastRunLevel.clear();
		// covers turning the tint off, or picking a new colour
		clientThread.invoke(this::handBack);
	}

	// tab widgets are destroyed on a logout or a hop, so drop what we cached
	// rather than writing into nothing
	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			renderer.reset();
			bars.reset();
			lastRunLevel.clear();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		LevelModel current = model;
		Skill skill = event.getSkill();
		if (current == null || skill == Skill.OVERALL
			|| !config.secondHalfLevels() || !renderer.isSkillEnabled(skill))
		{
			return;
		}

		int level = runLevel(current, skill, event.getXp());
		Integer previous = lastRunLevel.put(skill, level);

		// first sighting only primes the cache. logging in fires StatChanged for
		// all of them and must not spam 91 messages.
		if (previous != null && level > previous)
		{
			notifier.announce(skill, level, level == current.getThreshold());
		}
	}

	// -1 while a skill is still in its first half. at real 99 we hand back, but that
	// xp is also the run's last level, so announce it instead of swallowing it.
	private int runLevel(LevelModel current, Skill skill, int xp)
	{
		if (current.forSkill(skill, xp).isSecondHalf())
		{
			return current.forSkill(skill, xp).getLevel();
		}

		return current.secondHalfLevel(skill, xp) >= current.getThreshold()
			? current.getThreshold()
			: -1;
	}

	// the game rebuilds the tab whenever it likes, so reapply while it is open.
	// the renderer skips writes that change nothing, so this is nearly free.
	@Subscribe
	public void onPostClientTick(PostClientTick event)
	{
		LevelModel current = model;
		if (current == null || client.getGameState() != GameState.LOGGED_IN || !isSkillTabOpen())
		{
			return;
		}

		try
		{
			// work it out once, both renderers reveal the real level for it
			Skill hovered = renderer.hoveredSkill();

			renderer.render(current, hovered);
			renderer.renderTooltip(current);

			if (config.showProgressBars())
			{
				bars.render(current, hovered);
			}
		}
		catch (Exception e)
		{
			log.warn("failed drawing skill tab", e);
		}
	}

	// total level is a string on the object stack, not an int
	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		LevelModel current = model;
		if (current == null
			|| !config.matchTotalLevel()
			|| !SKILL_TAB_TOTAL_LEVEL.equals(event.getEventName()))
		{
			return;
		}

		try
		{
			Object[] stack = client.getObjectStack();
			int size = client.getObjectStackSize();
			if (size > 0)
			{
				stack[size - 1] = renderer.totalLevelText(current);
			}
		}
		catch (Exception e)
		{
			log.warn("failed rewriting total level", e);
		}
	}

	private void rebuildModel()
	{
		model = new LevelModel(config.secondHalfLevels(), config.threshold());
	}

	private boolean isSkillTabOpen()
	{
		Widget stats = client.getWidget(InterfaceID.Stats.UNIVERSE);
		return stats != null && !stats.isHidden();
	}

	private void redrawAllSkills()
	{
		// nothing to redraw into while logged out
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		for (Skill skill : Skill.values())
		{
			if (skill != Skill.OVERALL)
			{
				client.queueChangedSkill(skill);
			}
		}
	}

	@Provides
	TheOtherHalfConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TheOtherHalfConfig.class);
	}
}
