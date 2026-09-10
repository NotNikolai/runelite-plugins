package com.theotherhalf.render;

import com.theotherhalf.BarGoal;
import com.theotherhalf.TheOtherHalfConfig;
import com.theotherhalf.model.LevelModel;
import java.util.EnumMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

// our own widgets on each tile, never edits to the game's
@Slf4j
public class BarRenderer
{
	private static final int INDENT = 3;
	private static final int OPAQUE = 0;

	// same child SkillTabRenderer writes to
	private static final int CHILD_BASE_LEVEL = 5;

	@Inject
	private Client client;

	@Inject
	private TheOtherHalfConfig config;

	// [background, fill] per skill
	private final Map<Skill, Widget[]> bars = new EnumMap<>(Skill.class);

	public void render(LevelModel model, Skill hovered)
	{
		int height = config.barHeight();
		int indent = config.indentToMatchCorners() ? INDENT : 0;
		int fillColour = config.barColour().getRGB() & 0xFFFFFF;
		int backColour = config.barBackgroundColour().getRGB() & 0xFFFFFF;
		boolean realGoal = config.barGoal() == BarGoal.REAL;
		boolean revealOnHover = config.showRealLevel();

		for (Map.Entry<Skill, Integer> entry : SkillTabRenderer.skillWidgets().entrySet())
		{
			Skill skill = entry.getKey();
			Widget parent = client.getWidget(entry.getValue());
			if (parent == null || parent.getWidth() <= 0)
			{
				continue;
			}

			Widget[] pair = attached(parent, bars.get(skill));
			if (pair == null)
			{
				pair = create(parent);
				bars.put(skill, pair);
			}

			int xp = client.getSkillExperience(skill);

			// hovering shows the real goal even when the bar tracks the shown one
			boolean useReal = realGoal || (revealOnHover && skill == hovered);
			LevelModel.Result result = useReal ? model.realFor(xp) : model.forSkill(skill, xp);
			float progress = result.progress(xp);

			int full = Math.max(0, parent.getWidth() - indent * 2);
			int y = barTop(parent, height);

			place(pair[0], indent, y, full, height, backColour);
			place(pair[1], indent, y, Math.round(full * progress), height, fillColour);
		}
	}

	// no way to delete one child, and deleteAllChildren takes the game's too
	public void clear()
	{
		for (Map.Entry<Skill, Widget[]> entry : bars.entrySet())
		{
			// don't let one bad skill strand the rest
			try
			{
				for (Widget widget : entry.getValue())
				{
					if (widget == null)
					{
						continue;
					}

					// hiding alone doesn't repaint, and a leftover size still draws
					widget.setHidden(true);
					widget.setOriginalWidth(0);
					widget.setOriginalHeight(0);
					widget.revalidate();
				}

				// tile owns the layout
				Integer widgetId = SkillTabRenderer.skillWidgets().get(entry.getKey());
				Widget parent = widgetId == null ? null : client.getWidget(widgetId);
				if (parent != null)
				{
					parent.revalidate();
				}

			}
			catch (Exception e)
			{
				log.debug("could not clear the bar on {}", entry.getKey(), e);
			}
		}

		bars.clear();
	}

	// widgets already gone, just drop the refs
	public void reset()
	{
		bars.clear();
	}

	// bottom row is 2px taller and those 2 get clipped, so sit on the base level
	// number rather than the tile height
	private static int barTop(Widget parent, int height)
	{
		int bottom = parent.getHeight();

		Widget baseLevel = parent.getChild(CHILD_BASE_LEVEL);
		if (baseLevel != null && baseLevel.getOriginalHeight() > 0)
		{
			bottom = baseLevel.getOriginalY() + baseLevel.getOriginalHeight();
		}

		return Math.max(0, Math.min(bottom, parent.getHeight() - height));
	}

	// the game appends its own children after ours so position is not fixed.
	// assumed they stayed last once and leaked a pair per rebuild.
	private static Widget[] attached(Widget parent, Widget[] pair)
	{
		if (pair == null || pair[0] == null || pair[1] == null)
		{
			return null;
		}

		Widget[] children = parent.getDynamicChildren();
		if (children == null)
		{
			return null;
		}

		boolean first = false;
		boolean second = false;
		for (Widget child : children)
		{
			if (child == pair[0])
			{
				first = true;
			}
			else if (child == pair[1])
			{
				second = true;
			}
		}

		return first && second ? pair : null;
	}

	private static Widget[] create(Widget parent)
	{
		return new Widget[]{
			parent.createChild(WidgetType.RECTANGLE),
			parent.createChild(WidgetType.RECTANGLE)
		};
	}

	private static void place(Widget widget, int x, int y, int width, int height, int colour)
	{
		if (widget == null)
		{
			return;
		}

		widget.setHidden(width <= 0);
		widget.setOriginalX(x);
		widget.setOriginalY(y);
		widget.setOriginalWidth(width);
		widget.setOriginalHeight(height);
		widget.setTextColor(colour);
		widget.setFilled(true);
		widget.setOpacity(OPAQUE);
		widget.revalidate();
	}
}
