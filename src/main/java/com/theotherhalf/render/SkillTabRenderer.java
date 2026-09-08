package com.theotherhalf.render;

import com.theotherhalf.TheOtherHalfConfig;
import com.theotherhalf.model.LevelModel;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

// writes the numbers. LevelModel decides what they are.
public class SkillTabRenderer
{
	// no gameval for these. checked on 1.12.38: 4 is top left, 5 is bottom right.
	private static final int CHILD_CURRENT_LEVEL = 4;
	private static final int CHILD_BASE_LEVEL = 5;

	// tooltip splits labels and numbers into two text children
	private static final int TOOLTIP_LABELS = 2;
	private static final int TOOLTIP_VALUES = 3;
	private static final String LINE_BREAK = "<br>";
	private static final String XP_LABEL = " XP:";

	private static final Map<Skill, Integer> SKILL_WIDGETS = buildSkillWidgets();

	@Inject
	private Client client;

	@Inject
	private TheOtherHalfConfig config;

	// queueChangedSkill rebuilds the text but not the colour, so remember it ourselves
	private final Map<Skill, Integer> vanillaColour = new EnumMap<>(Skill.class);

	public void render(LevelModel model)
	{
		boolean tint = config.tintSecondHalfLevels();
		int colour = config.secondHalfColour().getRGB() & 0xFFFFFF;

		for (Map.Entry<Skill, Integer> entry : SKILL_WIDGETS.entrySet())
		{
			Skill skill = entry.getKey();
			if (!isSkillEnabled(skill))
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
				// game's number is already right
				continue;
			}

			String text = String.valueOf(result.getLevel());
			write(widget.getChild(CHILD_BASE_LEVEL), text, tint, colour);

			// top number here is live hp and prayer points, not a level. never touch it.
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
			total += isSkillEnabled(skill)
				? model.forSkill(skill, xp).getLevel()
				: Experience.getLevelForXp(xp);
		}
		return "Total level: " + total;
	}

	// labels in one child, values in another, newline separated.
	// a maxed skill only has one line, so never assume three.
	public void renderTooltip(LevelModel model)
	{
		boolean showReal = config.showRealLevel();
		boolean runXp = config.tooltipSecondHalfXp();
		if (!showReal && !runXp)
		{
			return;
		}

		Widget tip = client.getWidget(InterfaceID.Stats.TOOLTIP);
		if (tip == null || tip.isHidden())
		{
			return;
		}

		Widget[] children = tip.getDynamicChildren();
		if (children == null || children.length <= TOOLTIP_VALUES)
		{
			return;
		}

		Widget labels = children[TOOLTIP_LABELS];
		Widget values = children[TOOLTIP_VALUES];
		if (labels == null || values == null || labels.getText() == null || values.getText() == null)
		{
			return;
		}

		Skill skill = skillFromLabel(labels.getText());
		if (skill == null || !isSkillEnabled(skill))
		{
			return;
		}

		int xp = client.getSkillExperience(skill);
		LevelModel.Result result = model.forSkill(skill, xp);
		if (!result.isSecondHalf())
		{
			// game's tooltip is already honest
			return;
		}

		if (runXp)
		{
			writeRunXp(values, model, result, xp);
		}

		if (showReal)
		{
			writeRealLevel(labels, Experience.getLevelForXp(xp));
		}
	}

	private void writeRunXp(Widget values, LevelModel model, LevelModel.Result result, int xp)
	{
		String[] lines = values.getText().split(LINE_BREAK);
		if (lines.length < 3)
		{
			return;
		}

		// gap to the next level is the same either way, only the total shifts
		int into = model.xpIntoRun(xp);
		int remaining = result.getXpAtNextLevel() - xp;

		String text = format(into) + LINE_BREAK + format(into + remaining) + LINE_BREAK + format(remaining);
		if (!text.equals(values.getText()))
		{
			values.setText(text);
		}
	}

	private void writeRealLevel(Widget labels, int realLevel)
	{
		String text = labels.getText();
		int cut = text.indexOf(XP_LABEL);
		if (cut <= 0)
		{
			return;
		}

		// runs every tick while the tooltip is up, don't staple it on twice
		String name = text.substring(0, cut);
		if (name.endsWith(")"))
		{
			return;
		}

		labels.setText(name + " (" + realLevel + ")" + text.substring(cut));
	}

	private static Skill skillFromLabel(String text)
	{
		int cut = text.indexOf(XP_LABEL);
		if (cut <= 0)
		{
			return null;
		}

		String name = text.substring(0, cut);
		// drop our own suffix if it is already there
		int bracket = name.indexOf(" (");
		if (bracket > 0)
		{
			name = name.substring(0, bracket);
		}

		try
		{
			return Skill.valueOf(name.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	private static String format(int value)
	{
		return String.format("%,d", value);
	}

	// a skill redraw won't put the colour back, so do it here
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

	// widgets gone under us, drop the refs without touching them
	public void reset()
	{
		vanillaColour.clear();
	}

	private void rememberColour(Skill skill, Widget widget, int ourColour)
	{
		if (vanillaColour.containsKey(skill))
		{
			return;
		}

		Widget base = widget.getChild(CHILD_BASE_LEVEL);
		// already ours, nothing original left to save
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

	public boolean isSkillEnabled(Skill skill)
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

	// BarRenderer draws into the same tiles
	static Map<Skill, Integer> skillWidgets()
	{
		return SKILL_WIDGETS;
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
