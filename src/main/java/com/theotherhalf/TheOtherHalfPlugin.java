package com.theotherhalf;

import com.google.inject.Provides;
import com.theotherhalf.model.LevelModel;
import com.theotherhalf.render.SkillTabRenderer;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.PostClientTick;
import net.runelite.api.events.ScriptCallbackEvent;
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

	private LevelModel model;

	@Override
	protected void shutDown()
	{
		model = null;
		clientThread.invoke(this::handBack);
	}

	// a skill redraw fixes the numbers but leaves our tint behind, so undo that first
	private void handBack()
	{
		renderer.restore();
		redrawAllSkills();
	}

	// this fires after the eventbus has registered us, startUp does not, so the first
	// redraw has to happen here or the tab can come up stale
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
		if (TheOtherHalfConfig.GROUP.equals(event.getGroup()))
		{
			rebuildModel();
			// covers turning the tint off, or picking a new colour
			clientThread.invoke(this::handBack);
		}
	}

	// the game rebuilds the tab whenever it feels like it, so reapply while it is open.
	// the renderer skips writes that would not change anything, so this is nearly free.
	@Subscribe
	public void onPostClientTick(PostClientTick event)
	{
		LevelModel current = model;
		if (current == null || !isSkillTabOpen())
		{
			return;
		}

		try
		{
			renderer.render(current);
		}
		catch (Exception e)
		{
			log.warn("failed drawing skill tab", e);
		}
	}

	// total level is a string on the object stack, not an int, so it needs its own hook
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
