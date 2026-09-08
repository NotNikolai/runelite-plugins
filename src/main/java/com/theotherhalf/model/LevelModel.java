package com.theotherhalf.model;

import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * Turns real xp into the level we want to show. Pure maths, no client, no widgets.
 */
public final class LevelModel
{
	public static final int MAX_XP = 200_000_000;
	public static final int DEFAULT_THRESHOLD = 92;

	private static final int MIN_THRESHOLD = 2;
	private static final int MAX_THRESHOLD = 99;
	private static final int XP_99 = Experience.getXpForLevel(99);

	// hp has no level 1, a fresh account starts here
	private static final int HITPOINTS_START = 10;

	private final boolean enabled;
	private final int threshold;
	private final int thresholdXp;
	private final int seed;

	public LevelModel(boolean enabled, int threshold)
	{
		this.enabled = enabled;
		this.threshold = Math.max(MIN_THRESHOLD, Math.min(MAX_THRESHOLD, threshold));
		this.thresholdXp = Experience.getXpForLevel(this.threshold);

		// the run back to 99 is shorter than the run up to the threshold, so start it
		// part filled and every level lands on the same xp it did the first time.
		// 75 at level 92. below 92 it comes out negative, so the run just overshoots instead.
		this.seed = Math.max(0, this.thresholdXp - (XP_99 - this.thresholdXp));
	}

	public int getThreshold()
	{
		return threshold;
	}

	public int getSeed()
	{
		return seed;
	}

	public Result forSkill(Skill skill, int totalXp)
	{
		int xp = Math.max(0, Math.min(MAX_XP, totalXp));

		// first half, or turned off, the game's own number is already right
		if (!enabled || xp < thresholdXp)
		{
			return real(xp);
		}

		// at real 99 we hand back and stop touching anything
		if (xp >= XP_99)
		{
			return real(xp);
		}

		return run(skill, xp, thresholdXp - seed);
	}

	public int displayedLevel(Skill skill, int totalXp)
	{
		return forSkill(skill, totalXp).getLevel();
	}

	/**
	 * The level the second run has reached ignoring the handback, so the celebration at
	 * real 99 knows it is really the final level. At exactly xp(99) this is the threshold.
	 */
	public int secondHalfLevel(Skill skill, int totalXp)
	{
		int xp = Math.max(0, Math.min(MAX_XP, totalXp));
		if (xp < thresholdXp)
		{
			return levelFor(skill, xp);
		}
		return levelFor(skill, xp - thresholdXp + seed);
	}

	private Result real(int xp)
	{
		int level = Experience.getLevelForXp(xp);
		return new Result(level, xpAtLevel(level), xpAtLevel(level + 1), false);
	}

	private Result run(Skill skill, int xp, int offset)
	{
		int level = levelFor(skill, xp - offset);
		return new Result(level, xpAtLevel(level) + offset, xpAtLevel(level + 1) + offset, true);
	}

	private int levelFor(Skill skill, int xpIntoRun)
	{
		int level = Experience.getLevelForXp(Math.max(0, xpIntoRun));
		return skill == Skill.HITPOINTS ? Math.max(HITPOINTS_START, level) : level;
	}

	private static int xpAtLevel(int level)
	{
		if (level >= Experience.MAX_VIRT_LEVEL)
		{
			return MAX_XP;
		}
		return Experience.getXpForLevel(Math.max(1, level));
	}

	public static final class Result
	{
		private final int level;
		private final int xpAtLevel;
		private final int xpAtNextLevel;
		private final boolean secondHalf;

		Result(int level, int xpAtLevel, int xpAtNextLevel, boolean secondHalf)
		{
			this.level = level;
			this.xpAtLevel = xpAtLevel;
			this.xpAtNextLevel = xpAtNextLevel;
			this.secondHalf = secondHalf;
		}

		public int getLevel()
		{
			return level;
		}

		public int getXpAtLevel()
		{
			return xpAtLevel;
		}

		public int getXpAtNextLevel()
		{
			return xpAtNextLevel;
		}

		/**
		 * True when this level is a relabel of real xp, ie the only case where we
		 * should be writing anything to the skills tab.
		 */
		public boolean isSecondHalf()
		{
			return secondHalf;
		}

		public float progress(int totalXp)
		{
			int span = xpAtNextLevel - xpAtLevel;
			if (span <= 0)
			{
				return 1f;
			}
			float p = (float) (totalXp - xpAtLevel) / span;
			return Math.max(0f, Math.min(1f, p));
		}
	}
}
