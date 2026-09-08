package com.theotherhalf.model;

import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LevelModelTest
{
	private static final int XP_92 = 6_517_253;
	private static final int XP_99 = 13_034_431;

	private static LevelModel model()
	{
		return new LevelModel(true, LevelModel.DEFAULT_THRESHOLD);
	}

	private static int level(LevelModel m, int xp)
	{
		return m.displayedLevel(Skill.WOODCUTTING, xp);
	}

	@Test
	public void theXpTableIsWhatWeThinkItIs()
	{
		assertEquals(XP_92, Experience.getXpForLevel(92));
		assertEquals(XP_99, Experience.getXpForLevel(99));

		// 92 is the halfway point, that is the whole reason for the plugin
		assertEquals(75, XP_92 - (XP_99 - XP_92));
	}

	@Test
	public void seedIs75AtTheDefaultThreshold()
	{
		assertEquals(75, model().getSeed());
	}

	@Test
	public void seedIsDerivedNotHardcoded()
	{
		// seed is 2*xpFor(t) - xp(99), so it only comes out tiny at 92
		assertEquals(75, new LevelModel(true, 92).getSeed());

		// below 92 the second run is the longer one, nothing to bank, so it clamps off
		assertEquals(0, new LevelModel(true, 80).getSeed());
		assertEquals(0, new LevelModel(true, 20).getSeed());

		// above 92 the run is short and needs a big head start to still land on 99
		assertEquals(2 * Experience.getXpForLevel(95) - XP_99, new LevelModel(true, 95).getSeed());
	}

	@Test
	public void aboveNinetyTwoStillLandsOnTheThreshold()
	{
		// the point of the seed: displayed threshold and real 99 are the same xp
		for (int t : new int[]{92, 93, 95, 98})
		{
			LevelModel m = new LevelModel(true, t);
			assertEquals("threshold " + t, t, m.secondHalfLevel(Skill.WOODCUTTING, XP_99));
		}
	}

	@Test
	public void theSeedTableFromTheSpec()
	{
		LevelModel m = model();

		assertEquals(1, level(m, XP_92));           // reset lands on 1, not 0
		assertEquals(2, level(m, XP_92 + 8));       // level 2 needs 83, seed banks 75
		assertEquals(81, level(m, 8_771_558));
		assertEquals(91, level(m, XP_99 - 1));      // one xp short of 99 is 91
	}

	@Test
	public void realAccountValues()
	{
		// checked against a live account in the dev client
		LevelModel m = model();

		assertEquals(70, level(m, 7_307_344));      // attack 93
		assertEquals(71, level(m, 7_351_117));      // crafting 93
		assertEquals(79, level(m, 8_341_084));      // firemaking 94
		assertEquals(83, level(m, 9_427_489));      // slayer 95
		assertEquals(88, level(m, 10_926_532));     // farming 97

		// defence 91, the closest one still under the threshold
		assertEquals(91, level(m, 6_175_030));
		assertFalse(m.forSkill(Skill.WOODCUTTING, 6_175_030).isSecondHalf());
	}

	@Test
	public void justBelowTheThresholdIsStillReal()
	{
		LevelModel m = model();
		assertEquals(91, level(m, XP_92 - 1));
		assertFalse(m.forSkill(Skill.WOODCUTTING, XP_92 - 1).isSecondHalf());
	}

	@Test
	public void exactlyTheThresholdFlipsToTheSecondHalf()
	{
		assertTrue(model().forSkill(Skill.WOODCUTTING, XP_92).isSecondHalf());
	}

	@Test
	public void realNinetyNineHandsBack()
	{
		LevelModel m = model();
		assertEquals(99, level(m, XP_99));
		assertFalse(m.forSkill(Skill.WOODCUTTING, XP_99).isSecondHalf());

		// and stays handed back above it
		assertFalse(m.forSkill(Skill.WOODCUTTING, 50_000_000).isSecondHalf());
	}

	@Test
	public void theFinalLevelAndRealNinetyNineAreTheSameEvent()
	{
		// handback shows 99, but the celebration needs to know it was really the last one
		assertEquals(92, model().secondHalfLevel(Skill.WOODCUTTING, XP_99));
		assertEquals(91, model().secondHalfLevel(Skill.WOODCUTTING, XP_99 - 1));
	}

	@Test
	public void hitpointsNeverDropsBelowTen()
	{
		LevelModel m = model();

		// a fresh account starts at 10, so the second run has to as well
		assertEquals(10, m.displayedLevel(Skill.HITPOINTS, XP_92));
		assertEquals(10, m.displayedLevel(Skill.HITPOINTS, XP_92 + 1_000));

		// and it still climbs normally once past it
		assertTrue(m.displayedLevel(Skill.HITPOINTS, 8_771_558) > 10);
	}

	@Test
	public void otherSkillsAreNotClamped()
	{
		assertEquals(1, model().displayedLevel(Skill.WOODCUTTING, XP_92));
	}

	@Test
	public void turnedOffMeansRealLevels()
	{
		LevelModel off = new LevelModel(false, 92);
		assertEquals(92, off.displayedLevel(Skill.WOODCUTTING, XP_92));
		assertEquals(95, off.displayedLevel(Skill.WOODCUTTING, 8_771_558));
		assertFalse(off.forSkill(Skill.WOODCUTTING, XP_92).isSecondHalf());
	}

	@Test
	public void notLoggedInIsSurvivable()
	{
		// the tab renders with zeroes before stats load, seen in the dev client
		LevelModel m = model();
		assertEquals(1, level(m, 0));
		assertEquals(1, level(m, -1));
	}

	@Test
	public void twoHundredMillionIsSurvivable()
	{
		LevelModel m = model();
		int atCap = level(m, LevelModel.MAX_XP);
		assertEquals(atCap, level(m, Integer.MAX_VALUE));
		assertTrue(atCap >= 1);
	}

	@Test
	public void aLowThresholdWorksForDevTesting()
	{
		// section 11.12, threshold 20 so you can test without grinding a 99
		LevelModel m = new LevelModel(true, 20);
		int xp20 = Experience.getXpForLevel(20);

		assertEquals(19, m.displayedLevel(Skill.WOODCUTTING, xp20 - 1));
		assertEquals(1, m.displayedLevel(Skill.WOODCUTTING, xp20));
		assertTrue(m.forSkill(Skill.WOODCUTTING, xp20).isSecondHalf());
	}

	@Test
	public void xpWindowBracketsTheCurrentXp()
	{
		LevelModel m = model();
		int xp = 8_771_558;
		LevelModel.Result r = m.forSkill(Skill.WOODCUTTING, xp);

		assertTrue(r.getXpAtLevel() <= xp);
		assertTrue(r.getXpAtNextLevel() > xp);
	}

	@Test
	public void progressStaysInRange()
	{
		LevelModel m = model();
		LevelModel.Result r = m.forSkill(Skill.WOODCUTTING, 8_771_558);

		assertTrue(r.progress(8_771_558) >= 0f);
		assertTrue(r.progress(8_771_558) <= 1f);
		assertEquals(0f, r.progress(0), 0.0001f);
		assertEquals(1f, r.progress(LevelModel.MAX_XP), 0.0001f);
	}

	@Test
	public void everyThresholdClimbsWithinItsRun()
	{
		// the reset itself drops you back to 1 on purpose, that is the whole feature.
		// what must never happen is going backwards once a run is under way.
		for (int t = 2; t <= 99; t++)
		{
			LevelModel m = new LevelModel(true, t);
			int thresholdXp = Experience.getXpForLevel(t);

			int last = 0;
			for (int xp = 0; xp < thresholdXp; xp += 997)
			{
				int shown = m.displayedLevel(Skill.WOODCUTTING, xp);
				assertTrue("threshold " + t + " went backwards in the first half", shown >= last);
				last = shown;
			}

			last = 0;
			for (int xp = thresholdXp; xp < XP_99; xp += 9_973)
			{
				int shown = m.displayedLevel(Skill.WOODCUTTING, xp);
				assertTrue("threshold " + t + " went backwards in the second run", shown >= last);
				last = shown;
			}
		}
	}

	@Test
	public void theResetIsTheOnlyDrop()
	{
		// one xp either side of the threshold is the single point where a level goes down
		for (int t : new int[]{28, 66, 80, 92})
		{
			LevelModel m = new LevelModel(true, t);
			int thresholdXp = Experience.getXpForLevel(t);

			assertEquals("threshold " + t, t - 1, m.displayedLevel(Skill.WOODCUTTING, thresholdXp - 1));
			assertEquals("threshold " + t, 1, m.displayedLevel(Skill.WOODCUTTING, thresholdXp));
		}
	}

	@Test
	public void everyThresholdStaysInSaneBounds()
	{
		for (int t = 2; t <= 99; t++)
		{
			LevelModel m = new LevelModel(true, t);

			for (int xp = 0; xp <= LevelModel.MAX_XP; xp += 1_000_003)
			{
				int shown = m.displayedLevel(Skill.WOODCUTTING, xp);
				assertTrue("threshold " + t + " gave " + shown + " at xp " + xp, shown >= 1 && shown <= 126);

				int hp = m.displayedLevel(Skill.HITPOINTS, xp);
				assertTrue("hp fell under 10 at threshold " + t, hp >= 10 || xp < Experience.getXpForLevel(t));
			}
		}
	}

	@Test
	public void belowNinetyTwoTheRunOvershoots()
	{
		// the second run is the longer one here, so it climbs past the reset level
		// before real 99 arrives. expected, and why 92 is the default.
		LevelModel m = new LevelModel(true, 66);
		assertEquals(0, m.getSeed());
		assertEquals(1, m.displayedLevel(Skill.WOODCUTTING, Experience.getXpForLevel(66)));
		assertTrue(m.displayedLevel(Skill.WOODCUTTING, XP_99 - 1) > 66);
	}

	@Test
	public void everyLevelInTheSecondRunIsReachable()
	{
		// 91 level ups is the entire product claim, so prove it
		LevelModel m = model();
		int seen = 0;
		int last = 0;

		for (int lvl = 1; lvl <= 91; lvl++)
		{
			// level 1's window opens 75 xp before the reset, so the run only joins it at xp(92)
			int xp = Math.max(XP_92, Experience.getXpForLevel(lvl) + XP_92 - 75);
			int shown = level(m, xp);
			if (shown != last)
			{
				seen++;
				last = shown;
			}
			assertEquals(lvl, shown);
		}

		assertEquals(91, seen);
	}
}
