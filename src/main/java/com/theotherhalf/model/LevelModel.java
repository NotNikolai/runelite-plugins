package com.theotherhalf.model;

import net.runelite.api.Experience;
import net.runelite.api.Skill;

// real xp in, displayed level out. no client, no widgets, so it unit tests.
public final class LevelModel
{
	public static final int MAX_XP = 200_000_000;
	public static final int DEFAULT_THRESHOLD = 92;

	private static final int MIN_THRESHOLD = 2;
	private static final int MAX_THRESHOLD = 99;
	private static final int XP_99 = Experience.getXpForLevel(99);

	// hp has no level 1
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

		// run two is shorter, so start it part filled and every level lands on the
		// xp it did first time. 75 at level 92. negative below 92, so it overshoots.
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

		// first half or turned off, the game's number is already right
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

	// what the tooltip shows when it counts the run instead of real xp
	public int xpIntoRun(int totalXp)
	{
		int xp = Math.max(0, Math.min(MAX_XP, totalXp));
		return xp < thresholdXp ? xp : xp - thresholdXp + seed;
	}

	// ignores the handback so the celebration knows the last level was the last one.
	// at exactly xp(99) this is the threshold.
	public int secondHalfLevel(Skill skill, int totalXp)
	{
		int xp = Math.max(0, Math.min(MAX_XP, totalXp));
		if (xp < thresholdXp)
		{
			return levelFor(skill, xp);
		}
		return levelFor(skill, xp - thresholdXp + seed);
	}

	// real level and its xp window, untouched by the reset
	public Result realFor(int totalXp)
	{
		return real(Math.max(0, Math.min(MAX_XP, totalXp)));
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

		// the only case where we write to the tab at all
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
