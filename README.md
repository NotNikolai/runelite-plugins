# The Other Half

Level 92 is almost exactly halfway to 99 in experience. The first half of the grind gives you 91 level ups. The second half gives you 7.

The Other Half resets the skills tab at 92 so you climb 1 to 92 again on the way to 99.

| | Experience | Level ups |
|---|---|---|
| 1 to 92 | 6,517,253 | **91** |
| 92 to 99 | 6,517,178 | **7** |

Visual only. Nothing is sent to the server, and no real level, experience or combat level is changed.

## What it does

- **Second-half levels.** Once a skill passes 92, the tab starts again from 1. At a real 99 it hands back and shows 99 from then on.
- **Level ups that land.** The game will not announce these, because your real level has not moved, so the plugin does: a chat message, the game's own level up sound, and its own fireworks. Every tenth level and the last one of the run get the 99 fireworks.
- **Progress bars** on every skill, first half included, so it works as a plain bars plugin with the reset switched off.
- **Honest tooltips.** Hovering a skill shows your real level, so you can always check what a relabelled number really is. Optionally the tooltip can count experience through the second run instead.

The reset level is configurable, every skill can be turned off individually, and second-half levels are tinted so you can tell them from real ones.

## The 75 experience seed

The second half is 75 experience shorter than the first, so a plain reset would top out at 91 and jump straight to 99, losing the level the plugin exists to deliver. The run starts with 75 experience already banked instead, which lines everything up exactly: at a real 99 the displayed level is exactly 92.

## Known limitations

- Experience globes, the experience tracker, experience drops and the hiscores all show real levels
- Combat level is never touched
- Total level stays real unless you turn on the match toggle
- Above a real 99 the plugin stops interfering, so vanilla or Virtual Levels takes over
- Turn off the Skills Tab Progress Bars plugin if you use it, or you will get two sets of bars
- Hitpoints and Prayer are off by default, because their top number is a live resource rather than a level
- Level up plugins like C Engineer fire on real level ups, not these

## Licence

BSD-2-Clause.
