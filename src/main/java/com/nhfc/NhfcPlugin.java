package com.nhfc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import okhttp3.OkHttpClient;

@Slf4j
@PluginDescriptor(
	name = "NHFC",
	description = "For NHFC fighters: sends your own fight data to the NHFC league during NHFC fights",
	tags = {"nhfc", "pvp", "league", "fight"}
)
public class NhfcPlugin extends Plugin
{
	static final String VERSION = "0.0.2";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private NhfcConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private Gson gson;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private ClientToolbar clientToolbar;

	private NhfcConnection connection;
	private NhfcPanel panel;
	private NavigationButton navButton;
	private ScheduledFuture<?> pinger;

	// what the server has told us. only an armed plugin sends gameplay data
	private volatile boolean armed;

	// this tick's own events, sent together on the next game tick
	private final List<JsonObject> attacks = new ArrayList<>();
	private final List<JsonObject> hits = new ArrayList<>();
	private int lastHp = -1;
	private int lastPrayer = -1;
	private String lastOverhead = "";
	private int lastSpec = -1;

	@Provides
	NhfcConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NhfcConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new NhfcPanel();
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder().tooltip("NHFC").icon(icon).priority(10).panel(panel).build();
		clientToolbar.addNavigation(navButton);
		connection = new NhfcConnection(okHttpClient, gson, executor, this::onServerMessage, this::onClosed);
		pinger = executor.scheduleAtFixedRate(this::ping, 30, 30, TimeUnit.SECONDS);
		clientThread.invoke(this::reconnect);
	}

	@Override
	protected void shutDown()
	{
		pinger.cancel(false);
		connection.close();
		clientToolbar.removeNavigation(navButton);
		armed = false;
		clearTick();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!NhfcConfig.GROUP.equals(event.getGroup()) || NhfcConfig.TOKEN_KEY.equals(event.getKey()))
		{
			return;
		}
		clientThread.invoke(this::reconnect);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			// the name and account aren't ready the moment you log in, so wait a tick
			clientThread.invokeLater(this::reconnect);
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			connection.close();
			armed = false;
			status("Logged out");
		}
	}

	// connects once the fighter has opted in, has a code or token, and is logged in
	private void reconnect()
	{
		connection.close();
		armed = false;
		if (!config.enabled())
		{
			status("Off. Turn on Connect to NHFC in the settings.");
			return;
		}
		String code = config.connectCode().trim();
		String token = config.token();
		if (code.isEmpty() && token.isEmpty())
		{
			status("Paste the connect code from NHFC staff in the settings.");
			return;
		}
		Player me = client.getLocalPlayer();
		if (client.getGameState() != GameState.LOGGED_IN || me == null || me.getName() == null)
		{
			status("Waiting for you to log in");
			return;
		}
		JsonObject hello = new JsonObject();
		hello.addProperty("t", "hello");
		hello.addProperty("v", NhfcConnection.PROTOCOL);
		hello.addProperty("plugin", VERSION);
		if (!code.isEmpty())
		{
			hello.addProperty("code", code);
		}
		else
		{
			hello.addProperty("token", token);
		}
		hello.addProperty("accountHash", Long.toString(client.getAccountHash()));
		hello.addProperty("rsn", me.getName());
		hello.addProperty("world", client.getWorld());
		status("Connecting");
		connection.open(config.serverUrl(), hello);
	}

	// runs on okhttp's thread
	private void onServerMessage(JsonObject msg)
	{
		String t = msg.has("t") ? msg.get("t").getAsString() : "";
		switch (t)
		{
			case "welcome":
				if (msg.has("token"))
				{
					configManager.setConfiguration(NhfcConfig.GROUP, NhfcConfig.TOKEN_KEY, msg.get("token").getAsString());
					configManager.setConfiguration(NhfcConfig.GROUP, "connectCode", "");
				}
				armed = msg.has("armed");
				status("Connected as " + msg.get("fighter").getAsString());
				break;
			case "arm":
				armed = true;
				status("Armed for an NHFC fight. Sending your own fight data.");
				break;
			case "disarm":
				armed = false;
				clientThread.invoke(this::clearTick);
				status("Connected. Not sending anything.");
				break;
			case "error":
				String code = msg.has("code") ? msg.get("code").getAsString() : "";
				if ("bad_token".equals(code))
				{
					configManager.setConfiguration(NhfcConfig.GROUP, NhfcConfig.TOKEN_KEY, "");
				}
				connection.close();
				status(msg.has("message") ? msg.get("message").getAsString() : "NHFC refused the connection");
				break;
			case "update":
				connection.close();
				status(msg.get("message").getAsString());
				break;
			default:
				break;
		}
	}

	private void onClosed(String reason)
	{
		armed = false;
		status("Not connected: " + reason);
	}

	private void ping()
	{
		if (connection.isOpen())
		{
			JsonObject p = new JsonObject();
			p.addProperty("t", "ping");
			connection.send(p);
		}
	}

	// --- the fighter's own data, only while armed ---

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		// hits on this player only. never anyone else's
		if (!armed || event.getActor() != client.getLocalPlayer())
		{
			return;
		}
		Hitsplat h = event.getHitsplat();
		JsonObject hit = new JsonObject();
		hit.addProperty("amount", h.getAmount());
		// the raw hitsplat type for now. which types count gets settled from in-game testing
		hit.addProperty("type", String.valueOf(h.getHitsplatType()));
		hits.add(hit);
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		Player me = client.getLocalPlayer();
		if (!armed || event.getActor() != me || me == null)
		{
			return;
		}
		// an animation while interacting with a player counts as an attack.
		// the proof uses this simple rule; the real attack animation list comes in stage 4
		Actor target = me.getInteracting();
		int anim = me.getAnimation();
		if (anim == -1 || !(target instanceof Player))
		{
			return;
		}
		JsonObject attack = new JsonObject();
		attack.addProperty("anim", anim);
		attack.addProperty("weapon", weaponId());
		attack.addProperty("style", "unknown");
		attack.addProperty("spec", false);
		attacks.add(attack);
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (!armed)
		{
			return;
		}
		JsonObject msg = new JsonObject();
		msg.addProperty("t", "tick");
		msg.addProperty("tick", client.getTickCount());
		boolean changed = false;

		int hp = client.getBoostedSkillLevel(Skill.HITPOINTS);
		if (hp != lastHp)
		{
			JsonObject o = new JsonObject();
			o.addProperty("cur", hp);
			o.addProperty("max", client.getRealSkillLevel(Skill.HITPOINTS));
			msg.add("hp", o);
			lastHp = hp;
			changed = true;
		}

		int prayer = client.getBoostedSkillLevel(Skill.PRAYER);
		String overhead = overhead();
		if (prayer != lastPrayer || !overhead.equals(lastOverhead))
		{
			JsonObject o = new JsonObject();
			o.addProperty("points", prayer);
			if (overhead.isEmpty())
			{
				o.add("overhead", null);
			}
			else
			{
				o.addProperty("overhead", overhead);
			}
			msg.add("prayer", o);
			lastPrayer = prayer;
			lastOverhead = overhead;
			changed = true;
		}

		// the varp holds energy times ten
		int spec = client.getVarpValue(VarPlayerID.SA_ENERGY) / 10;
		if (spec != lastSpec)
		{
			// energy dropping on the tick of an attack means that attack was a special
			if (lastSpec != -1 && spec < lastSpec && !attacks.isEmpty())
			{
				attacks.get(attacks.size() - 1).addProperty("spec", true);
			}
			msg.addProperty("spec", spec);
			lastSpec = spec;
			changed = true;
		}

		if (!attacks.isEmpty())
		{
			JsonArray a = new JsonArray();
			attacks.forEach(a::add);
			msg.add("attacks", a);
			changed = true;
		}
		if (!hits.isEmpty())
		{
			JsonArray h = new JsonArray();
			hits.forEach(h::add);
			msg.add("hits", h);
			changed = true;
		}
		clearTick();
		if (changed)
		{
			connection.send(msg);
		}
	}

	private void clearTick()
	{
		attacks.clear();
		hits.clear();
	}

	private int weaponId()
	{
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn == null)
		{
			return -1;
		}
		Item weapon = worn.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		return weapon == null ? -1 : weapon.getId();
	}

	// the active overhead, from the prayer varbits (1 while the prayer is on)
	private String overhead()
	{
		if (client.getVarbitValue(VarbitID.PRAYER_PROTECTFROMMELEE) == 1)
		{
			return "melee";
		}
		if (client.getVarbitValue(VarbitID.PRAYER_PROTECTFROMMISSILES) == 1)
		{
			return "ranged";
		}
		if (client.getVarbitValue(VarbitID.PRAYER_PROTECTFROMMAGIC) == 1)
		{
			return "magic";
		}
		if (client.getVarbitValue(VarbitID.PRAYER_SMITE) == 1)
		{
			return "smite";
		}
		if (client.getVarbitValue(VarbitID.PRAYER_REDEMPTION) == 1)
		{
			return "redemption";
		}
		if (client.getVarbitValue(VarbitID.PRAYER_RETRIBUTION) == 1)
		{
			return "retribution";
		}
		return "";
	}

	private void status(String text)
	{
		log.debug("nhfc: {}", text);
		SwingUtilities.invokeLater(() -> panel.setStatus(text, armed));
	}
}
