package com.nhfc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

// the proof's panel: whether you're connected, and whether anything is being sent.
// the full side panel (rank, next fight, loadout) comes in stage 4
class NhfcPanel extends PluginPanel
{
	private static final Color GOLD = new Color(0xE8, 0xB1, 0x3A);

	private final JLabel status = new JLabel();
	private final JLabel sending = new JLabel();

	NhfcPanel()
	{
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		JPanel box = new JPanel(new GridLayout(0, 1, 0, 8));
		box.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		box.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JLabel title = new JLabel("NHFC");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(GOLD);
		box.add(title);
		box.add(status);
		box.add(sending);
		JLabel note = new JLabel("<html>Nothing about your gameplay is sent unless NHFC staff arm one of your fights, and only your own data ever is.</html>");
		note.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		box.add(note);
		add(box, BorderLayout.NORTH);
		setStatus("Starting", false);
	}

	void setStatus(String text, boolean armed)
	{
		status.setText("<html>" + escape(text) + "</html>");
		sending.setText(armed ? "Sending: your fight data" : "Sending: nothing");
		sending.setForeground(armed ? GOLD : ColorScheme.LIGHT_GRAY_COLOR);
	}

	private static String escape(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
