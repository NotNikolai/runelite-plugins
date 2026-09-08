package com.theotherhalf.render;

import com.theotherhalf.TheOtherHalfConfig;
import com.theotherhalf.model.LevelModel;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/**
 * Writes the level numbers on the skills tab. Nothing here decides what the level is,
 * that is all LevelModel.
 */
public class SkillTabRenderer
{
	// no gameval names these, but the widget inspector confirms them on 1.12.38.
	// 4 is the top left number, 5 is the bottom right one.
	private static final int CHILD_CURRENT_LEVEL = 4;
	private static final int CHILD_BASE_LEVEL = 5;

	private static final Map<Skill, Integer> SKILL_WIDGETS = buildSkillWidgets();

	@Inject
	private Client client;

	@Inject
	private TheOtherHalfConfig config;

	// queueChangedSkill rebuilds the level text but not its colour, so we have to
	// remember what each one looked like before we touched it
	private final Map<Skill, Integer> vanillaColour = new EnumMap<>(Skill.class);

	public void render(LevelModel model)
	{
		boolean tint = config.tintSecondHalfLevels();
		int colour = config.secondHalfColour().getRGB() & 0xFFFFFF;

		for (Map.Entry<Skill, Integer> entry : SKILL_WIDGETS.entrySet())
		{
			Skill skill = entry.getKey();
			if (!isEnabled(skill))
			{
				continue;
			}

			Widget widget = client.getWidget(entry.getValue());
			if (widget == null)
			{
				continue;
			}

			rememberColour(skill, widget, colour);

			LevelModel.Result result = model.forSkill(skill, client.getSkillExperience(skill));
			if (!result.isSecondHalf())
			{
				// first half or handed back, the game's own number is already right
				continue;
			}

			String text = String.valueOf(result.getLevel());
			write(widget.getChild(CHILD_BASE_LEVEL), text, tint, colour);

			// the top number for these two is live hp and prayer points, not a level.
			// rewriting it would hide something you need, so never do it.
			if (skill == Skill.HITPOINTS || skill == Skill.PRAYER)
			{
				continue;
			}

			// leave it alone while a boost is up so the boost stays visible
			if (client.getBoostedSkillLevel(skill) == client.getRealSkillLevel(skill))
			{
				write(widget.getChild(CHILD_CURRENT_LEVEL), text, tint, colour);
			}
		}
	}

	public String totalLevelText(LevelModel model)
	{
		int total = 0;
		for (Skill skill : SKILL_WIDGETS.keySet())
		{
			int xp = client.getSkillExperience(skill);
			total += isEnabled(skill)
				? model.forSkill(skill, xp).getLevel()
				: Experience.getLevelForXp(xp);
		}
		return "Total level: " + total;
	}

	/**
	 * Puts the vanilla colours back. Call this before handing the tab over on shutdown
	 * or when the tint gets switched off, because a skill redraw will not do it for you.
	 */
	public void restore()
	{
		for (Map.Entry<Skill, Integer> entry : vanillaColour.entrySet())
		{
			Integer widgetId = SKILL_WIDGETS.get(entry.getKey());
			Widget widget = widgetId == null ? null : client.getWidget(widgetId);
			if (widget == null)
			{
				continue;
			}

			recolour(widget.getChild(CHILD_CURRENT_LEVEL), entry.getValue());
			recolour(widget.getChild(CHILD_BASE_LEVEL), entry.getValue());
		}

		vanillaColour.clear();
	}

	private void rememberColour(Skill skill, Widget widget, int ourColour)
	{
		if (vanillaColour.containsKey(skill))
		{
			return;
		}

		Widget base = widget.getChild(CHILD_BASE_LEVEL);
		// if it already matches ours there is nothing original left to save
		if (base != null && base.getTextColor() != ourColour)
		{
			vanillaColour.put(skill, base.getTextColor());
		}
	}

	private static void recolour(Widget widget, int colour)
	{
		if (widget != null && widget.getTextColor() != colour)
		{
			widget.setTextColor(colour);
		}
	}

	private void write(Widget widget, String text, boolean tint, int colour)
	{
		if (widget == null)
		{
			return;
		}

		if (!text.equals(widget.getText()))
		{
			widget.setText(text);
		}

		if (tint && widget.getTextColor() != colour)
		{
			widget.setTextColor(colour);
		}
	}

	private boolean isEnabled(Skill skill)
	{
		switch (skill)
		{
			case ATTACK:
				return config.attack();
			case STRENGTH:
				return config.strength();
			case DEFENCE:
				return config.defence();
			case RANGED:
				return config.ranged();
			case MAGIC:
				return config.magic();
			case HITPOINTS:
				return config.hitpoints();
			case PRAYER:
				return config.prayer();
			case AGILITY:
				return config.agility();
			case CONSTRUCTION:
				return config.construction();
			case COOKING:
				return config.cooking();
			case CRAFTING:
				return config.crafting();
			case FARMING:
				return config.farming();
			case FIREMAKING:
				return config.firemaking();
			case FISHING:
				return config.fishing();
			case FLETCHING:
				return config.fletching();
			case HERBLORE:
				return config.herblore();
			case HUNTER:
				return config.hunter();
			case MINING:
				return config.mining();
			case RUNECRAFT:
				return config.runecraft();
			case SAILING:
				return config.sailing();
			case SLAYER:
				return config.slayer();
			case SMITHING:
				return config.smithing();
			case THIEVING:
				return config.thieving();
			case WOODCUTTING:
				return config.woodcutting();
			default:
				return false;
		}
	}

	private static Map<Skill, Integer> buildSkillWidgets()
	{
		Map<Skill, Integer> map = new EnumMap<>(Skill.class);
		map.put(Skill.ATTACK, InterfaceID.Stats.ATTACK);
		map.put(Skill.STRENGTH, InterfaceID.Stats.STRENGTH);
		map.put(Skill.DEFENCE, InterfaceID.Stats.DEFENCE);
		map.put(Skill.RANGED, InterfaceID.Stats.RANGED);
		map.put(Skill.PRAYER, InterfaceID.Stats.PRAYER);
		map.put(Skill.MAGIC, InterfaceID.Stats.MAGIC);
		map.put(Skill.HITPOINTS, InterfaceID.Stats.HITPOINTS);
		map.put(Skill.RUNECRAFT, InterfaceID.Stats.RUNECRAFT);
		map.put(Skill.CONSTRUCTION, InterfaceID.Stats.CONSTRUCTION);
		map.put(Skill.AGILITY, InterfaceID.Stats.AGILITY);
		map.put(Skill.HERBLORE, InterfaceID.Stats.HERBLORE);
		map.put(Skill.THIEVING, InterfaceID.Stats.THIEVING);
		map.put(Skill.CRAFTING, InterfaceID.Stats.CRAFTING);
		map.put(Skill.FLETCHING, InterfaceID.Stats.FLETCHING);
		map.put(Skill.SLAYER, InterfaceID.Stats.SLAYER);
		map.put(Skill.HUNTER, InterfaceID.Stats.HUNTER);
		map.put(Skill.MINING, InterfaceID.Stats.MINING);
		map.put(Skill.SMITHING, InterfaceID.Stats.SMITHING);
		map.put(Skill.FISHING, InterfaceID.Stats.FISHING);
		map.put(Skill.COOKING, InterfaceID.Stats.COOKING);
		map.put(Skill.FIREMAKING, InterfaceID.Stats.FIREMAKING);
		map.put(Skill.WOODCUTTING, InterfaceID.Stats.WOODCUTTING);
		map.put(Skill.FARMING, InterfaceID.Stats.FARMING);
		map.put(Skill.SAILING, InterfaceID.Stats.SAILING);
		return Collections.unmodifiableMap(map);
	}
}
