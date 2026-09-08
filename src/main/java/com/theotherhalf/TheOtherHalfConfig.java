package com.theotherhalf;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(TheOtherHalfConfig.GROUP)
public interface TheOtherHalfConfig extends Config
{
	// never change this, it prefixes every saved setting
	String GROUP = "the-other-half";


	@ConfigSection(
		name = "Progress bars",
		description = "Bars along the bottom of every skill tile",
		position = 18
	)
	String BARS = "bars";

	@ConfigSection(
		name = "Celebration",
		description = "What happens when a second-half level lands",
		position = 19
	)
	String CELEBRATION = "celebration";

	@ConfigSection(
		name = "Combat",
		description = "Which combat skills get second-half levels",
		position = 20
	)
	String COMBAT = "combat";

	@ConfigSection(
		name = "Skilling",
		description = "Which non combat skills get second-half levels",
		position = 21
	)
	String SKILLING = "skilling";

	@ConfigItem(
		keyName = "secondHalfLevels",
		name = "Second-half levels",
		description = "Restart the skills tab at level 1 once a skill passes the halfway point to 99, so the back half of the grind gives you 91 level ups instead of 7.",
		position = 0
	)
	default boolean secondHalfLevels()
	{
		return true;
	}

	@ConfigItem(
		keyName = "threshold",
		name = "Reset at level",
		description = "The level the tab resets at. 92 is the xp midpoint of 99 and the only value that splits the grind evenly. Drop it low to test without grinding a 99.",
		position = 1
	)
	@Range(min = 2, max = 99)
	default int threshold()
	{
		return 92;
	}

	@ConfigItem(
		keyName = "tintSecondHalfLevels",
		name = "Tint second-half levels",
		description = "Shades levels that are a relabel of real xp, so you can tell a second-half 41 from a real one.",
		position = 3
	)
	default boolean tintSecondHalfLevels()
	{
		return true;
	}

	@ConfigItem(
		keyName = "secondHalfColour",
		name = "Second-half colour",
		description = "Colour for tinted levels. Vanilla is bright yellow, so a different hue reads better than a different brightness.",
		position = 4
	)
	default Color secondHalfColour()
	{
		// vanilla is ffff00, close enough to look at home but still tell apart
		return new Color(0xFFD54A);
	}

	@ConfigItem(
		keyName = "showRealLevel",
		name = "Show real level on hover",
		description = "Puts your real level in the skill's hover tooltip, so you can always check what a relabelled level really is.",
		position = 6
	)
	default boolean showRealLevel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tooltipSecondHalfXp",
		name = "Second-half xp on hover",
		description = "Makes the hover tooltip count xp through the second run instead of your real total, so it lines up with the level on the tab. Off means the tooltip keeps telling you the truth.",
		position = 7
	)
	default boolean tooltipSecondHalfXp()
	{
		return false;
	}

	@ConfigItem(
		keyName = "levelUpMessage",
		name = "Level up message",
		description = "Posts a chat message when a second-half level lands. The game will not do it for you, your real level has not moved.",
		position = 8
	)
	default boolean levelUpMessage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "levelUpColour",
		name = "Level up message colour",
		position = 9,
		description = "Colour of the level up chat message."
	)
	default Color levelUpColour()
	{
		return new Color(0x00A000);
	}

	@ConfigItem(
		keyName = "showProgressBars",
		name = "Progress bars",
		description = "Adds progress bars to every skill on the tab. Turn off the Skills Tab Progress Bars plugin if you use it, or you will get two sets.",
		section = BARS,
		position = 1
	)
	default boolean showProgressBars()
	{
		return false;
	}

	@ConfigItem(
		keyName = "barHeight",
		name = "Bar height",
		description = "How tall the bars are, in pixels.",
		section = BARS,
		position = 2
	)
	@Range(min = 1, max = 6)
	default int barHeight()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "barColour",
		name = "Bar colour",
		description = "Colour of the filled part.",
		section = BARS,
		position = 3
	)
	default Color barColour()
	{
		return new Color(0x33CC33);
	}

	@ConfigItem(
		keyName = "barBackgroundColour",
		name = "Bar background",
		description = "Colour of the unfilled part.",
		section = BARS,
		position = 4
	)
	default Color barBackgroundColour()
	{
		return new Color(0x1E1E1E);
	}

	@ConfigItem(
		keyName = "indentToMatchCorners",
		name = "Indent to match corners",
		description = "Pulls the bars in a few pixels so they sit inside the rounded corners of the tile.",
		section = BARS,
		position = 5
	)
	default boolean indentToMatchCorners()
	{
		return true;
	}

	@ConfigItem(
		keyName = "levelUpFireworks",
		name = "Fireworks",
		description = "Sets off the game's own level up fireworks over your character. Every tenth level, and the last one of the run, get the 99 ones instead.",
		section = CELEBRATION,
		position = 1
	)
	default boolean levelUpFireworks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "levelUpSound",
		name = "Sound",
		description = "Plays a sound when a second-half level lands.",
		section = CELEBRATION,
		position = 2
	)
	default boolean levelUpSound()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bigMilestonesOnly",
		name = "Only on milestones",
		description = "Limits the sound and fireworks to every tenth level plus the final one. The chat message still fires every level.",
		section = CELEBRATION,
		position = 4
	)
	default boolean bigMilestonesOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "suppressInCombat",
		name = "Not in combat",
		description = "Skips the sound and fireworks while you are fighting or inside an instance.",
		section = CELEBRATION,
		position = 5
	)
	default boolean suppressInCombat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "matchTotalLevel",
		name = "Match total level",
		description = "Sum the displayed levels instead of the real ones. Off by default, because your total is the number other people ask you for.",
		position = 5
	)
	default boolean matchTotalLevel()
	{
		return false;
	}

	@ConfigItem(keyName = "skillAttack", name = "Attack", description = "", section = COMBAT, position = 1)
	default boolean attack()
	{
		return true;
	}

	@ConfigItem(keyName = "skillStrength", name = "Strength", description = "", section = COMBAT, position = 2)
	default boolean strength()
	{
		return true;
	}

	@ConfigItem(keyName = "skillDefence", name = "Defence", description = "", section = COMBAT, position = 3)
	default boolean defence()
	{
		return true;
	}

	@ConfigItem(keyName = "skillRanged", name = "Ranged", description = "", section = COMBAT, position = 4)
	default boolean ranged()
	{
		return true;
	}

	@ConfigItem(keyName = "skillMagic", name = "Magic", description = "", section = COMBAT, position = 5)
	default boolean magic()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillHitpoints",
		name = "Hitpoints",
		description = "Off by default. The top number is your live hp, not a level, so it will not match.",
		section = COMBAT,
		position = 6
	)
	default boolean hitpoints()
	{
		return false;
	}

	@ConfigItem(
		keyName = "skillPrayer",
		name = "Prayer",
		description = "Off by default. The top number is your live prayer points, not a level, so it will not match.",
		section = COMBAT,
		position = 7
	)
	default boolean prayer()
	{
		return false;
	}

	@ConfigItem(keyName = "skillAgility", name = "Agility", description = "", section = SKILLING, position = 1)
	default boolean agility()
	{
		return true;
	}

	@ConfigItem(keyName = "skillConstruction", name = "Construction", description = "", section = SKILLING, position = 2)
	default boolean construction()
	{
		return true;
	}

	@ConfigItem(keyName = "skillCooking", name = "Cooking", description = "", section = SKILLING, position = 3)
	default boolean cooking()
	{
		return true;
	}

	@ConfigItem(keyName = "skillCrafting", name = "Crafting", description = "", section = SKILLING, position = 4)
	default boolean crafting()
	{
		return true;
	}

	@ConfigItem(keyName = "skillFarming", name = "Farming", description = "", section = SKILLING, position = 5)
	default boolean farming()
	{
		return true;
	}

	@ConfigItem(keyName = "skillFiremaking", name = "Firemaking", description = "", section = SKILLING, position = 6)
	default boolean firemaking()
	{
		return true;
	}

	@ConfigItem(keyName = "skillFishing", name = "Fishing", description = "", section = SKILLING, position = 7)
	default boolean fishing()
	{
		return true;
	}

	@ConfigItem(keyName = "skillFletching", name = "Fletching", description = "", section = SKILLING, position = 8)
	default boolean fletching()
	{
		return true;
	}

	@ConfigItem(keyName = "skillHerblore", name = "Herblore", description = "", section = SKILLING, position = 9)
	default boolean herblore()
	{
		return true;
	}

	@ConfigItem(keyName = "skillHunter", name = "Hunter", description = "", section = SKILLING, position = 10)
	default boolean hunter()
	{
		return true;
	}

	@ConfigItem(keyName = "skillMining", name = "Mining", description = "", section = SKILLING, position = 11)
	default boolean mining()
	{
		return true;
	}

	@ConfigItem(keyName = "skillRunecraft", name = "Runecraft", description = "", section = SKILLING, position = 12)
	default boolean runecraft()
	{
		return true;
	}

	@ConfigItem(keyName = "skillSailing", name = "Sailing", description = "", section = SKILLING, position = 13)
	default boolean sailing()
	{
		return true;
	}

	@ConfigItem(keyName = "skillSlayer", name = "Slayer", description = "", section = SKILLING, position = 14)
	default boolean slayer()
	{
		return true;
	}

	@ConfigItem(keyName = "skillSmithing", name = "Smithing", description = "", section = SKILLING, position = 15)
	default boolean smithing()
	{
		return true;
	}

	@ConfigItem(keyName = "skillThieving", name = "Thieving", description = "", section = SKILLING, position = 16)
	default boolean thieving()
	{
		return true;
	}

	@ConfigItem(keyName = "skillWoodcutting", name = "Woodcutting", description = "", section = SKILLING, position = 17)
	default boolean woodcutting()
	{
		return true;
	}
}
